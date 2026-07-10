import 'package:flutter/material.dart';

import 'primary_button.dart';

/// Friendly, plain-language error state with a retry action — screens should
/// never show a raw exception message or stack trace.
class ErrorState extends StatelessWidget {
  const ErrorState({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.wifi_off_rounded, size: 40, color: Colors.grey),
            const SizedBox(height: 16),
            Text(message, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              SizedBox(width: 160, child: PrimaryButton(label: 'Try again', onPressed: onRetry)),
            ],
          ],
        ),
      ),
    );
  }
}
