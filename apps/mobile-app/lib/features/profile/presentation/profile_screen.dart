import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/providers.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/widgets/confirm_dialog.dart';
import '../../../core/widgets/info_card.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(sessionControllerProvider).user;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            InfoCard(
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: AppColors.primary,
                    child: Text(
                      (user?.firstName.isNotEmpty == true ? user!.firstName[0] : '?').toUpperCase(),
                      style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w700),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(user?.fullName ?? '', style: Theme.of(context).textTheme.titleLarge),
                        const SizedBox(height: 4),
                        Text(user?.email ?? '', style: Theme.of(context).textTheme.bodyMedium),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            _MenuTile(
              icon: Icons.badge_outlined,
              label: 'Personal information',
              onTap: () => _showComingSoon(context),
            ),
            _MenuTile(
              icon: Icons.security_outlined,
              label: 'Security',
              onTap: () => _showComingSoon(context),
            ),
            _MenuTile(
              icon: Icons.phone_iphone_outlined,
              label: 'Devices',
              onTap: () => _showComingSoon(context),
            ),
            _MenuTile(
              icon: Icons.speed_outlined,
              label: 'Transfer limits',
              onTap: () => context.push(AppRoutes.increaseLimit),
            ),
            _MenuTile(
              icon: Icons.support_agent_outlined,
              label: 'Support',
              onTap: () => context.push(AppRoutes.support),
            ),
            _MenuTile(
              icon: Icons.settings_outlined,
              label: 'Settings',
              onTap: () => _showComingSoon(context),
            ),
            const SizedBox(height: 12),
            _MenuTile(
              icon: Icons.logout,
              label: 'Log out',
              isDestructive: true,
              onTap: () async {
                final confirmed = await showConfirmDialog(
                  context,
                  title: 'Log out',
                  message: 'Are you sure you want to log out of LimitFlow?',
                  confirmLabel: 'Log out',
                  isDestructive: true,
                );
                if (confirmed) {
                  await ref.read(sessionControllerProvider.notifier).logout();
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showComingSoon(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('This is a demo — this section is not wired up yet.')),
    );
  }
}

class _MenuTile extends StatelessWidget {
  const _MenuTile({
    required this.icon,
    required this.label,
    required this.onTap,
    this.isDestructive = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool isDestructive;

  @override
  Widget build(BuildContext context) {
    final color = isDestructive ? AppColors.error : null;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 4),
          child: Row(
            children: [
              Icon(icon, color: color),
              const SizedBox(width: 16),
              Expanded(child: Text(label, style: TextStyle(color: color, fontSize: 16))),
              if (!isDestructive) const Icon(Icons.chevron_right, color: Colors.grey),
            ],
          ),
        ),
      ),
    );
  }
}
