import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/providers.dart';
import '../../../core/session/app_user.dart';
import 'biometric_service.dart';

class AuthRepository {
  AuthRepository(this._client);

  final ApiClient _client;

  Future<({String token, AppUser user})> login({
    required String email,
    required String password,
  }) async {
    final response = await _client.post('/auth/login', body: {
      'email': email,
      'password': password,
    });

    final user = AppUser.fromJson(response['user'] as Map<String, dynamic>);
    return (token: response['token'] as String, user: user);
  }

  Future<AppUser> fetchCurrentUser() async {
    final response = await _client.get('/customer/me');
    return AppUser.fromJson(response);
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(apiClientProvider));
});

final biometricServiceProvider = Provider<BiometricService>((ref) {
  return BiometricService();
});
