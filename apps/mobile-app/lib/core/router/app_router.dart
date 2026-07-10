import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/authentication/presentation/login_screen.dart';
import '../../features/authentication/presentation/splash_screen.dart';
import '../../features/limit_request/presentation/increase_limit_wizard_screen.dart';
import '../../features/limit_request/presentation/request_status_screen.dart';
import '../../features/shell/presentation/app_shell_screen.dart';
import '../../features/support/presentation/support_chat_screen.dart';
import '../providers.dart';
import '../session/session_controller.dart';

abstract class AppRoutes {
  static const splash = '/splash';
  static const login = '/login';
  static const home = '/home';
  static const increaseLimit = '/increase-limit';
  static const requestStatus = '/requests/:id';
  static const support = '/support';

  static String requestStatusPath(String id) => '/requests/$id';
}

final routerProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = _RouterRefreshNotifier(ref);
  ref.onDispose(refreshNotifier.dispose);

  return GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final session = ref.read(sessionControllerProvider);
      final atSplash = state.matchedLocation == AppRoutes.splash;
      final atLogin = state.matchedLocation == AppRoutes.login;

      switch (session.status) {
        case SessionStatus.unknown:
          return atSplash ? null : AppRoutes.splash;
        case SessionStatus.unauthenticated:
          return atLogin ? null : AppRoutes.login;
        case SessionStatus.authenticated:
          return (atSplash || atLogin) ? AppRoutes.home : null;
      }
    },
    routes: [
      GoRoute(path: AppRoutes.splash, builder: (context, state) => const SplashScreen()),
      GoRoute(path: AppRoutes.login, builder: (context, state) => const LoginScreen()),
      GoRoute(path: AppRoutes.home, builder: (context, state) => const AppShellScreen()),
      GoRoute(
        path: AppRoutes.increaseLimit,
        builder: (context, state) => const IncreaseLimitWizardScreen(),
      ),
      GoRoute(
        path: AppRoutes.requestStatus,
        builder: (context, state) =>
            RequestStatusScreen(requestId: state.pathParameters['id']!),
      ),
      GoRoute(path: AppRoutes.support, builder: (context, state) => const SupportChatScreen()),
    ],
  );
});

/// Bridges Riverpod state changes into something [GoRouter]'s
/// `refreshListenable` (a plain [Listenable]) can react to, so a login/logout
/// immediately re-runs the redirect logic above.
class _RouterRefreshNotifier extends ChangeNotifier {
  _RouterRefreshNotifier(Ref ref) {
    _subscription = ref.listen<SessionState>(
      sessionControllerProvider,
      (_, __) => notifyListeners(),
    );
  }

  late final ProviderSubscription<SessionState> _subscription;

  @override
  void dispose() {
    _subscription.close();
    super.dispose();
  }
}
