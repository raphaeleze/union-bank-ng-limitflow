import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../../limit_request/application/limit_request_repository.dart';
import '../../limit_request/domain/limit_request_model.dart';

const _cacheKey = 'dashboard_current_limit';

/// Fetches the dashboard summary, falling back to the last cached copy if the
/// network call fails — the dashboard should still show *something* useful
/// when offline rather than a blank error screen.
final currentLimitProvider = FutureProvider.autoDispose<CurrentLimitModel>((ref) async {
  final repository = ref.watch(limitRequestRepositoryProvider);
  final cache = ref.watch(localCacheServiceProvider);

  try {
    final result = await repository.fetchCurrentLimit();
    await cache.writeJson(_cacheKey, {
      'accountId': result.accountId,
      'dailyLimit': result.dailyLimit,
      'usedToday': result.usedToday,
      'remaining': result.remaining,
      'activeRequest': null, // active request is always re-fetched fresh, not cached
    });
    return result;
  } catch (error) {
    final cached = cache.readJson(_cacheKey);
    if (cached != null) {
      return CurrentLimitModel.fromJson(cached);
    }
    rethrow;
  }
});
