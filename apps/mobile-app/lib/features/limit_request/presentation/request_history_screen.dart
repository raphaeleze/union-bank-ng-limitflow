import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/router/app_router.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../../core/widgets/error_state.dart';
import '../../../core/widgets/info_card.dart';
import '../../../core/widgets/skeleton_list_tile.dart';
import '../../../core/widgets/status_badge.dart';
import '../application/limit_request_repository.dart';
import '../domain/limit_request_model.dart';

final requestHistoryProvider = FutureProvider.autoDispose<List<LimitRequestModel>>((ref) {
  return ref.watch(limitRequestRepositoryProvider).fetchHistory();
});

class RequestHistoryScreen extends ConsumerWidget {
  const RequestHistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final historyAsync = ref.watch(requestHistoryProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Requests')),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(requestHistoryProvider.future),
          child: historyAsync.when(
            loading: () => const SkeletonList(),
            error: (error, _) => ErrorState(
              message: "We couldn't load your requests.",
              onRetry: () => ref.invalidate(requestHistoryProvider),
            ),
            data: (requests) {
              if (requests.isEmpty) {
                return ListView(
                  children: const [
                    SizedBox(height: 120),
                    Center(child: Text('No transfer limit requests yet.')),
                  ],
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.all(20),
                itemCount: requests.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  final request = requests[index];
                  return InkWell(
                    borderRadius: BorderRadius.circular(18),
                    onTap: () => context.push(AppRoutes.requestStatusPath(request.id)),
                    child: InfoCard(
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  CurrencyFormatter.format(request.requestedLimit),
                                  style: Theme.of(context).textTheme.titleMedium,
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  DateFormat.yMMMd().add_jm().format(request.createdAt),
                                  style: Theme.of(context).textTheme.bodyMedium,
                                ),
                              ],
                            ),
                          ),
                          StatusBadge(status: request.status),
                        ],
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }
}
