import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'network/api_client.dart';
import 'session/session_controller.dart';
import 'storage/local_cache_service.dart';
import 'storage/secure_storage_service.dart';

final secureStorageServiceProvider = Provider<SecureStorageService>((ref) {
  return SecureStorageService(const FlutterSecureStorage());
});

/// Overridden in `main.dart` with an already-initialized instance, since Hive
/// setup is asynchronous and must happen before the widget tree is built.
final localCacheServiceProvider = Provider<LocalCacheService>((ref) {
  throw UnimplementedError('localCacheServiceProvider must be overridden in main()');
});

final sessionControllerProvider = StateNotifierProvider<SessionController, SessionState>((ref) {
  return SessionController(ref.watch(secureStorageServiceProvider));
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    ref.watch(secureStorageServiceProvider),
    onUnauthorized: () => ref.read(sessionControllerProvider.notifier).logout(),
  );
});
