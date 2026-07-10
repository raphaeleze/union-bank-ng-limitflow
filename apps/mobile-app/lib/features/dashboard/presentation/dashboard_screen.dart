import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/providers.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../../core/widgets/error_state.dart';
import '../../../core/widgets/info_card.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../core/widgets/secondary_button.dart';
import '../../../core/widgets/skeleton_box.dart';
import '../../../core/widgets/status_badge.dart';
import '../../limit_request/domain/limit_request_model.dart';
import '../application/dashboard_providers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  static const _activeStatuses = {
    'PENDING',
    'OTP_PENDING',
    'BIOMETRIC_PENDING',
    'UNDER_REVIEW',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(sessionControllerProvider);
    final currentLimitAsync = ref.watch(currentLimitProvider);

    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(currentLimitProvider.future),
          child: currentLimitAsync.when(
            loading: () => const _DashboardSkeleton(),
            error: (error, _) => ErrorState(
              message: "We couldn't load your dashboard. Pull down to try again.",
              onRetry: () => ref.invalidate(currentLimitProvider),
            ),
            data: (summary) => ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Text(
                  'Hi ${session.user?.firstName ?? 'there'} 👋',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 4),
                Text('Here\'s your account today.', style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: 24),
                _LimitSummaryCard(summary: summary),
                const SizedBox(height: 20),
                if (summary.activeRequest != null &&
                    _activeStatuses.contains(summary.activeRequest!.status))
                  _ActiveRequestCard(request: summary.activeRequest!)
                else
                  PrimaryButton(
                    label: 'Increase transfer limit',
                    onPressed: () => context.push(AppRoutes.increaseLimit),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LimitSummaryCard extends StatelessWidget {
  const _LimitSummaryCard({required this.summary});

  final CurrentLimitModel summary;

  @override
  Widget build(BuildContext context) {
    return InfoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Daily transfer limit', style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 6),
          Text(
            CurrencyFormatter.format(summary.dailyLimit),
            style: Theme.of(context).textTheme.headlineLarge,
          ),
          const SizedBox(height: 20),
          _ProgressBar(used: summary.usedToday, total: summary.dailyLimit),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _StatColumn(
                  label: 'Used today',
                  value: CurrencyFormatter.format(summary.usedToday),
                ),
              ),
              Expanded(
                child: _StatColumn(
                  label: 'Remaining',
                  value: CurrencyFormatter.format(summary.remaining),
                  valueColor: AppColors.accent,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ProgressBar extends StatelessWidget {
  const _ProgressBar({required this.used, required this.total});

  final double used;
  final double total;

  @override
  Widget build(BuildContext context) {
    final ratio = total <= 0 ? 0.0 : (used / total).clamp(0, 1).toDouble();
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: LinearProgressIndicator(
        value: ratio,
        minHeight: 8,
        backgroundColor: Theme.of(context).dividerColor,
        valueColor: AlwaysStoppedAnimation(ratio > 0.85 ? AppColors.warning : AppColors.primary),
      ),
    );
  }
}

class _StatColumn extends StatelessWidget {
  const _StatColumn({required this.label, required this.value, this.valueColor});

  final String label;
  final String value;
  final Color? valueColor;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 4),
        Text(
          value,
          style: Theme.of(context)
              .textTheme
              .titleMedium
              ?.copyWith(color: valueColor),
        ),
      ],
    );
  }
}

class _ActiveRequestCard extends StatelessWidget {
  const _ActiveRequestCard({required this.request});

  final LimitRequestModel request;

  @override
  Widget build(BuildContext context) {
    return InfoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Active request', style: Theme.of(context).textTheme.titleMedium),
              StatusBadge(status: request.status),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Requesting ${CurrencyFormatter.format(request.requestedLimit)}',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 4),
          Text(
            'We\'ll notify you the moment there\'s an update.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          SecondaryButton(
            label: 'View status',
            onPressed: () => context.push(AppRoutes.requestStatusPath(request.id)),
          ),
        ],
      ),
    );
  }
}

class _DashboardSkeleton extends StatelessWidget {
  const _DashboardSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: const [
        SkeletonBox(width: 160, height: 28),
        SizedBox(height: 8),
        SkeletonBox(width: 200, height: 14),
        SizedBox(height: 24),
        InfoCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SkeletonBox(width: 140, height: 14),
              SizedBox(height: 10),
              SkeletonBox(width: 180, height: 32),
              SizedBox(height: 20),
              SkeletonBox(height: 8, borderRadius: 4),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SkeletonBox(width: 80, height: 12),
                        SizedBox(height: 6),
                        SkeletonBox(width: 100, height: 18),
                      ],
                    ),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SkeletonBox(width: 80, height: 12),
                        SizedBox(height: 6),
                        SkeletonBox(width: 100, height: 18),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        SizedBox(height: 20),
        SkeletonBox(height: 52, borderRadius: 14),
      ],
    );
  }
}
