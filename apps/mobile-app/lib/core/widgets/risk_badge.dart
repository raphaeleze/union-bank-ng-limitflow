import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

/// Renders a `RiskLevel` value (`LOW` / `MEDIUM` / `HIGH`) as a small pill.
/// Shown to support/manager staff context, not customers.
class RiskBadge extends StatelessWidget {
  const RiskBadge({super.key, required this.riskLevel});

  final String? riskLevel;

  Color _color(BuildContext context) {
    switch (riskLevel) {
      case 'HIGH':
        return AppColors.error;
      case 'MEDIUM':
        return AppColors.warning;
      case 'LOW':
        return AppColors.accent;
      default:
        return Theme.of(context).textTheme.bodyMedium?.color ?? AppColors.lightTextSecondary;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (riskLevel == null) return const SizedBox.shrink();
    final color = _color(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        '$riskLevel RISK',
        style: TextStyle(color: color, fontWeight: FontWeight.w700, fontSize: 10),
      ),
    );
  }
}
