import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/providers.dart';
import '../domain/limit_request_model.dart';

class LimitRequestRepository {
  LimitRequestRepository(this._client);

  final ApiClient _client;

  Future<CurrentLimitModel> fetchCurrentLimit() async {
    final response = await _client.get('/limits/current');
    return CurrentLimitModel.fromJson(response);
  }

  Future<List<LimitRequestModel>> fetchHistory() async {
    final response = await _client.getList('/limits/history');
    return response.map((e) => LimitRequestModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<LimitRequestModel> fetchById(String id) async {
    final response = await _client.get('/limits/$id');
    return LimitRequestModel.fromJson(response);
  }

  Future<LimitRequestModel> submitRequest({
    required String accountId,
    required double requestedLimit,
    required String reason,
    required bool knownDevice,
  }) async {
    final response = await _client.post('/limits/request', body: {
      'accountId': accountId,
      'requestedLimit': requestedLimit,
      'reason': reason,
      'knownDevice': knownDevice,
    });
    return LimitRequestModel.fromJson(response);
  }

  Future<LimitRequestModel> verifyOtp({required String requestId, required String code}) async {
    final response = await _client.post('/limits/$requestId/otp/verify', body: {'code': code});
    return LimitRequestModel.fromJson(response);
  }

  Future<LimitRequestModel> verifyBiometric({
    required String requestId,
    required bool success,
  }) async {
    final response =
        await _client.post('/limits/$requestId/biometric/verify', body: {'success': success});
    return LimitRequestModel.fromJson(response);
  }
}

final limitRequestRepositoryProvider = Provider<LimitRequestRepository>((ref) {
  return LimitRequestRepository(ref.watch(apiClientProvider));
});
