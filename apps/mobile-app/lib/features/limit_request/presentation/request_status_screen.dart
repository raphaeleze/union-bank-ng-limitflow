import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/utils/currency_formatter.dart';
import '../../../core/widgets/error_state.dart';
import '../../../core/widgets/info_card.dart';
import '../../../core/widgets/risk_badge.dart';
import '../../../core/widgets/skeleton_box.dart';
import '../../../core/widgets/status_badge.dart';
import '../../../core/widgets/timeline_tile.dart';
import '../application/limit_request_repository.dart';
import '../domain/limit_request_model.dart';

final _requestByIdProvider =
    FutureProvider.autoDispose.family<LimitRequestModel, String>((ref, id) {
  return ref.watch(limitRequestRepositoryProvider).fetchById(id);
});

class RequestStatusScreen extends ConsumerWidget {
  const RequestStatusScreen({super.key, required this.requestId});

  final String requestId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final requestAsync = ref.watch(_requestByIdProvider(requestId));

    return Scaffold(
      appBar: AppBar(title: const Text('Request status')),
      body: SafeArea(
        child: requestAsync.when(
          loading: () => const _RequestStatusSkeleton(),
          error: (error, _) => ErrorState(
            message: "We couldn't load this request.",
            onRetry: () => ref.invalidate(_requestByIdProvider(requestId)),
          ),
          data: (request) => RefreshIndicator(
            onRefresh: () async => ref.invalidate(_requestByIdProvider(requestId)),
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                InfoCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            CurrencyFormatter.format(request.requestedLimit),
                            style: Theme.of(context).textTheme.headlineMedium,
                          ),
                          StatusBadge(status: request.status),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(request.reason, style: Theme.of(context).textTheme.bodyMedium),
                      if (request.riskLevel != null) ...[
                        const SizedBox(height: 10),
                        RiskBadge(riskLevel: request.riskLevel),
                      ],
                      if (request.isUnderManualReview) ...[
                        const SizedBox(height: 12),
                        Text(
                          "This one needs a quick human look. We'll notify you as soon as it's decided — usually within 24 hours.",
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                InfoCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Timeline', style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 16),
                      for (var i = 0; i < request.timeline.length; i++)
                        TimelineTile(
                          step: TimelineStepData(
                            label: request.timeline[i].label,
                            status: request.timeline[i].status,
                          ),
                          isLast: i == request.timeline.length - 1,
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _RequestStatusSkeleton extends StatelessWidget {
  const _RequestStatusSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const InfoCard(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              SkeletonBox(width: 140, height: 28),
              SkeletonBox(width: 90, height: 24, borderRadius: 999),
            ],
          ),
        ),
        const SizedBox(height: 20),
        InfoCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List.generate(4, (index) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 24),
                child: Row(
                  children: [
                    const SkeletonBox(width: 26, height: 26, borderRadius: 13),
                    const SizedBox(width: 14),
                    SkeletonBox(width: 120 + (index.isEven ? 20 : 0), height: 14),
                  ],
                ),
              );
            }),
          ),
        ),
      ],
    );
  }
}
