import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../../../core/session/app_user.dart';
import '../../../core/theme/app_colors.dart';

class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen> {
  static const _cachedUserKey = 'cached_user';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _restoreSession());
  }

  Future<void> _restoreSession() async {
    final cache = ref.read(localCacheServiceProvider);
    final cachedJson = cache.readJson(_cachedUserKey);
    final cachedUser = cachedJson != null ? AppUser.fromJson(cachedJson) : null;

    // A short, deliberate pause so the splash doesn't just flash by — this is
    // a demo app, first impressions matter more than shaving milliseconds.
    await Future.delayed(const Duration(milliseconds: 700));

    await ref.read(sessionControllerProvider.notifier).restore(cachedUser);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.primary,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 84,
              height: 84,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
              ),
              child: const Icon(Icons.bolt_rounded, color: AppColors.primary, size: 44),
            ),
            const SizedBox(height: 20),
            const Text(
              'LimitFlow',
              style: TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 32),
            const SizedBox(
              width: 28,
              height: 28,
              child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white),
            ),
          ],
        ),
      ),
    );
  }
}
