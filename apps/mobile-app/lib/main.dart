import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/providers.dart';
import 'core/router/app_router.dart';
import 'core/storage/local_cache_service.dart';
import 'core/theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final localCache = LocalCacheService();
  await localCache.init();

  runApp(
    ProviderScope(
      overrides: [
        localCacheServiceProvider.overrideWithValue(localCache),
      ],
      child: const LimitFlowApp(),
    ),
  );
}

class LimitFlowApp extends ConsumerWidget {
  const LimitFlowApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);

    return MaterialApp.router(
      title: 'LimitFlow',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: router,
    );
  }
}
