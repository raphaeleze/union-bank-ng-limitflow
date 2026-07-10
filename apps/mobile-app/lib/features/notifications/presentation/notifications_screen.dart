import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/widgets/error_state.dart';
import '../../../core/widgets/info_card.dart';
import '../../../core/widgets/loading_overlay.dart';
import '../application/notifications_repository.dart';
import '../domain/notification_model.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationsAsync = ref.watch(notificationsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(notificationsProvider.future),
          child: notificationsAsync.when(
            loading: () => const LoadingOverlay(),
            error: (error, _) => ErrorState(
              message: "We couldn't load your notifications.",
              onRetry: () => ref.invalidate(notificationsProvider),
            ),
            data: (notifications) {
              if (notifications.isEmpty) {
                return ListView(
                  children: const [
                    SizedBox(height: 120),
                    Center(child: Text('No notifications yet.')),
                  ],
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.all(20),
                itemCount: notifications.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (context, index) => _NotificationTile(notification: notifications[index]),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.notification});

  final NotificationModel notification;

  IconData get _icon {
    switch (notification.type) {
      case 'OTP_SENT':
        return Icons.sms_outlined;
      case 'VERIFICATION_COMPLETED':
      case 'VERIFICATION_REQUESTED':
        return Icons.verified_user_outlined;
      case 'LIMIT_APPROVED':
        return Icons.check_circle_outline;
      case 'LIMIT_REJECTED':
        return Icons.cancel_outlined;
      case 'SUPPORT_COMMENT':
        return Icons.chat_bubble_outline;
      default:
        return Icons.notifications_outlined;
    }
  }

  @override
  Widget build(BuildContext context) {
    return InfoCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: AppColors.primary.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(_icon, color: AppColors.primary, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(notification.title, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(notification.message, style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
          if (!notification.read)
            Container(
              width: 8,
              height: 8,
              margin: const EdgeInsets.only(top: 4),
              decoration: const BoxDecoration(color: AppColors.primary, shape: BoxShape.circle),
            ),
        ],
      ),
    );
  }
}
