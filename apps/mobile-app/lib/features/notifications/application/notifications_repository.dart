import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/providers.dart';
import '../domain/notification_model.dart';

class NotificationsRepository {
  NotificationsRepository(this._client);

  final ApiClient _client;

  Future<List<NotificationModel>> fetchAll() async {
    final response = await _client.getList('/notifications');
    return response.map((e) => NotificationModel.fromJson(e as Map<String, dynamic>)).toList();
  }
}

final notificationsRepositoryProvider = Provider<NotificationsRepository>((ref) {
  return NotificationsRepository(ref.watch(apiClientProvider));
});

final notificationsProvider = FutureProvider.autoDispose<List<NotificationModel>>((ref) {
  return ref.watch(notificationsRepositoryProvider).fetchAll();
});
