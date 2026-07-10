import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

/// One step in a vertical progress timeline (e.g. Submitted → OTP Verified →
/// Biometric Verified → Risk Assessment → Approved).
class TimelineStepData {
  const TimelineStepData({required this.label, required this.status});

  final String label;

  /// One of `COMPLETE`, `CURRENT`, `PENDING`.
  final String status;
}

class TimelineTile extends StatelessWidget {
  const TimelineTile({
    super.key,
    required this.step,
    required this.isLast,
  });

  final TimelineStepData step;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    final isComplete = step.status == 'COMPLETE';
    final isCurrent = step.status == 'CURRENT';
    final color = isComplete
        ? AppColors.accent
        : isCurrent
            ? AppColors.primary
            : AppColors.lightTextSecondary.withValues(alpha: 0.4);

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 26,
                height: 26,
                decoration: BoxDecoration(
                  color: isComplete || isCurrent ? color : Colors.transparent,
                  shape: BoxShape.circle,
                  border: Border.all(color: color, width: 2),
                ),
                child: isComplete
                    ? const Icon(Icons.check, size: 16, color: Colors.white)
                    : isCurrent
                        ? const Padding(
                            padding: EdgeInsets.all(6),
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : null,
              ),
              if (!isLast)
                Expanded(
                  child: Container(
                    width: 2,
                    margin: const EdgeInsets.symmetric(vertical: 2),
                    color: color.withValues(alpha: isComplete ? 1 : 0.3),
                  ),
                ),
            ],
          ),
          const SizedBox(width: 14),
          Padding(
            padding: const EdgeInsets.only(bottom: 24, top: 2),
            child: Text(
              step.label,
              style: TextStyle(
                fontWeight: isCurrent ? FontWeight.w700 : FontWeight.w500,
                color: isCurrent || isComplete ? null : AppColors.lightTextSecondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
