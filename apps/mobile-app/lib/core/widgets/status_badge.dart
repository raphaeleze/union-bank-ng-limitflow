import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

/// Renders a `RequestStatus` value (e.g. `UNDER_REVIEW`) as a friendly pill.
class StatusBadge extends StatelessWidget {
  const StatusBadge({super.key, required this.status});

  final String status;

  static const _labels = {
    'PENDING': 'Pending',
    'OTP_PENDING': 'Verifying code',
    'BIOMETRIC_PENDING': 'Verifying identity',
    'UNDER_REVIEW': 'Under review',
    'APPROVED': 'Approved',
    'REJECTED': 'Declined',
  };

  Color _color() {
    switch (status) {
      case 'APPROVED':
        return AppColors.accent;
      case 'REJECTED':
        return AppColors.error;
      case 'UNDER_REVIEW':
        return AppColors.warning;
      default:
        return AppColors.primary;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _color();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        _labels[status] ?? status,
        style: TextStyle(color: color, fontWeight: FontWeight.w600, fontSize: 12),
      ),
    );
  }
}
