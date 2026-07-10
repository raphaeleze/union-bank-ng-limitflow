import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../../core/widgets/loading_overlay.dart';
import '../../../core/widgets/otp_input.dart';
import '../../../core/widgets/primary_button.dart';
import '../../authentication/application/auth_repository.dart';
import '../application/limit_request_repository.dart';
import '../domain/limit_request_model.dart';

enum _WizardStep { chooseLimit, reason, review, otp, biometric, success }

class IncreaseLimitWizardScreen extends ConsumerStatefulWidget {
  const IncreaseLimitWizardScreen({super.key});

  @override
  ConsumerState<IncreaseLimitWizardScreen> createState() => _IncreaseLimitWizardScreenState();
}

class _IncreaseLimitWizardScreenState extends ConsumerState<IncreaseLimitWizardScreen> {
  _WizardStep _step = _WizardStep.chooseLimit;

  bool _isLoadingAccount = true;
  String? _accountId;
  double _currentLimit = 0;

  double _requestedLimit = 0;
  final _reasonController = TextEditingController();
  bool _knownDevice = true;

  bool _isSubmitting = false;
  String? _errorMessage;
  LimitRequestModel? _request;
  String _otpCode = '';

  @override
  void initState() {
    super.initState();
    _loadAccount();
  }

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _loadAccount() async {
    try {
      final summary = await ref.read(limitRequestRepositoryProvider).fetchCurrentLimit();
      setState(() {
        _accountId = summary.accountId;
        _currentLimit = summary.dailyLimit;
        _requestedLimit = summary.dailyLimit * 1.5;
        _isLoadingAccount = false;
      });
    } on ApiException catch (error) {
      setState(() {
        _errorMessage = error.message;
        _isLoadingAccount = false;
      });
    }
  }

  int get _stepIndex => _WizardStep.values.indexOf(_step);

  void _goTo(_WizardStep step) => setState(() {
        _errorMessage = null;
        _step = step;
      });

  Future<void> _submitRequest() async {
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      final request = await ref.read(limitRequestRepositoryProvider).submitRequest(
            accountId: _accountId!,
            requestedLimit: _requestedLimit,
            reason: _reasonController.text.trim(),
            knownDevice: _knownDevice,
          );
      setState(() {
        _request = request;
        _step = _WizardStep.otp;
      });
    } on ApiException catch (error) {
      setState(() => _errorMessage = error.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _verifyOtp() async {
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      final request = await ref.read(limitRequestRepositoryProvider).verifyOtp(
            requestId: _request!.id,
            code: _otpCode,
          );
      setState(() {
        _request = request;
        _step = _WizardStep.biometric;
      });
    } on ApiException catch (error) {
      setState(() => _errorMessage = error.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _verifyBiometric() async {
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    final biometrics = ref.read(biometricServiceProvider);
    final available = await biometrics.isAvailable();
    final success = available
        ? await biometrics.authenticate('Confirm your transfer limit increase')
        : true; // no biometric hardware/enrollment on this device — demo proceeds anyway

    if (!success) {
      setState(() {
        _isSubmitting = false;
        _errorMessage = 'Biometric confirmation was cancelled. Please try again.';
      });
      return;
    }

    try {
      final request = await ref.read(limitRequestRepositoryProvider).verifyBiometric(
            requestId: _request!.id,
            success: true,
          );
      setState(() {
        _request = request;
        _step = _WizardStep.success;
      });
    } on ApiException catch (error) {
      setState(() => _errorMessage = error.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final canGoBack = _stepIndex > 0 &&
        _step != _WizardStep.otp &&
        _step != _WizardStep.biometric &&
        _step != _WizardStep.success;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Increase transfer limit'),
        automaticallyImplyLeading: canGoBack,
      ),
      body: SafeArea(
        child: _isLoadingAccount
            ? const LoadingOverlay()
            : Column(
                children: [
                  if (_step != _WizardStep.success) _StepProgress(index: _stepIndex, total: 5),
                  Expanded(child: _buildStep(context)),
                ],
              ),
      ),
    );
  }

  Widget _buildStep(BuildContext context) {
    switch (_step) {
      case _WizardStep.chooseLimit:
        return _ChooseLimitStep(
          currentLimit: _currentLimit,
          requestedLimit: _requestedLimit,
          onChanged: (value) => setState(() => _requestedLimit = value),
          onNext: () => _goTo(_WizardStep.reason),
        );
      case _WizardStep.reason:
        return _ReasonStep(
          controller: _reasonController,
          onNext: () => _goTo(_WizardStep.review),
        );
      case _WizardStep.review:
        return _ReviewStep(
          currentLimit: _currentLimit,
          requestedLimit: _requestedLimit,
          reason: _reasonController.text.trim(),
          knownDevice: _knownDevice,
          onKnownDeviceChanged: (value) => setState(() => _knownDevice = value),
          errorMessage: _errorMessage,
          isSubmitting: _isSubmitting,
          onSubmit: _submitRequest,
        );
      case _WizardStep.otp:
        return _OtpStep(
          errorMessage: _errorMessage,
          isSubmitting: _isSubmitting,
          onCodeChanged: (value) => _otpCode = value,
          onVerify: _verifyOtp,
        );
      case _WizardStep.biometric:
        return _BiometricStep(
          errorMessage: _errorMessage,
          isSubmitting: _isSubmitting,
          onConfirm: _verifyBiometric,
        );
      case _WizardStep.success:
        return _SuccessStep(request: _request!);
    }
  }
}

class _StepProgress extends StatelessWidget {
  const _StepProgress({required this.index, required this.total});

  final int index;
  final int total;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Step ${index + 1} of $total', style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(value: (index + 1) / total, minHeight: 6),
          ),
        ],
      ),
    );
  }
}

class _ChooseLimitStep extends StatelessWidget {
  const _ChooseLimitStep({
    required this.currentLimit,
    required this.requestedLimit,
    required this.onChanged,
    required this.onNext,
  });

  final double currentLimit;
  final double requestedLimit;
  final ValueChanged<double> onChanged;
  final VoidCallback onNext;

  @override
  Widget build(BuildContext context) {
    final quickOptions = [currentLimit * 1.5, currentLimit * 2, currentLimit * 5];

    return _WizardBody(
      title: 'How much do you need?',
      subtitle: 'Your current daily limit is ${CurrencyFormatter.format(currentLimit)}.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            CurrencyFormatter.format(requestedLimit),
            style: Theme.of(context).textTheme.headlineLarge,
          ),
          const SizedBox(height: 8),
          Slider(
            value: requestedLimit.clamp(currentLimit + 1, currentLimit * 10),
            min: currentLimit + 1,
            max: currentLimit * 10,
            onChanged: onChanged,
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            children: quickOptions
                .map((amount) => ChoiceChip(
                      label: Text(CurrencyFormatter.format(amount)),
                      selected: requestedLimit == amount,
                      onSelected: (_) => onChanged(amount),
                    ))
                .toList(),
          ),
        ],
      ),
      onNext: onNext,
    );
  }
}

class _ReasonStep extends StatefulWidget {
  const _ReasonStep({required this.controller, required this.onNext});

  final TextEditingController controller;
  final VoidCallback onNext;

  @override
  State<_ReasonStep> createState() => _ReasonStepState();
}

class _ReasonStepState extends State<_ReasonStep> {
  static const _suggestions = [
    'Family emergency',
    'Business payment',
    'Rent or tuition',
    'Large purchase',
  ];

  @override
  Widget build(BuildContext context) {
    return _WizardBody(
      title: "What's this for?",
      subtitle: 'A short reason helps us review your request faster.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _suggestions
                .map((s) => ActionChip(label: Text(s), onPressed: () {
                      widget.controller.text = s;
                      setState(() {});
                    }))
                .toList(),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: widget.controller,
            maxLines: 3,
            decoration: const InputDecoration(hintText: 'Tell us why you need a higher limit'),
            onChanged: (_) => setState(() {}),
          ),
        ],
      ),
      onNext: widget.controller.text.trim().isEmpty ? null : widget.onNext,
    );
  }
}

class _ReviewStep extends StatelessWidget {
  const _ReviewStep({
    required this.currentLimit,
    required this.requestedLimit,
    required this.reason,
    required this.knownDevice,
    required this.onKnownDeviceChanged,
    required this.errorMessage,
    required this.isSubmitting,
    required this.onSubmit,
  });

  final double currentLimit;
  final double requestedLimit;
  final String reason;
  final bool knownDevice;
  final ValueChanged<bool> onKnownDeviceChanged;
  final String? errorMessage;
  final bool isSubmitting;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return _WizardBody(
      title: 'Review your request',
      subtitle: "We'll send a one-time code to verify it's really you.",
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _ReviewRow(label: 'Current limit', value: CurrencyFormatter.format(currentLimit)),
          _ReviewRow(label: 'Requested limit', value: CurrencyFormatter.format(requestedLimit)),
          _ReviewRow(label: 'Reason', value: reason),
          const Divider(height: 32),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('This is a trusted device'),
            subtitle: const Text('Turn off to see how an unrecognized device changes the review.'),
            value: knownDevice,
            onChanged: onKnownDeviceChanged,
          ),
          if (errorMessage != null) ...[
            const SizedBox(height: 8),
            Text(errorMessage!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ],
      ),
      onNext: null,
      customAction: PrimaryButton(label: 'Send verification code', onPressed: onSubmit, isLoading: isSubmitting),
    );
  }
}

class _ReviewRow extends StatelessWidget {
  const _ReviewRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodyMedium),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
        ],
      ),
    );
  }
}

class _OtpStep extends StatelessWidget {
  const _OtpStep({
    required this.errorMessage,
    required this.isSubmitting,
    required this.onCodeChanged,
    required this.onVerify,
  });

  final String? errorMessage;
  final bool isSubmitting;
  final ValueChanged<String> onCodeChanged;
  final VoidCallback onVerify;

  @override
  Widget build(BuildContext context) {
    return _WizardBody(
      title: 'Enter your verification code',
      subtitle: 'For this demo there\'s no real SMS gateway — open the Notifications '
          'tab to find the 6-digit code we just sent.',
      child: OtpInput(length: 6, onChanged: onCodeChanged),
      onNext: null,
      customAction: Column(
        children: [
          if (errorMessage != null) ...[
            Text(errorMessage!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            const SizedBox(height: 12),
          ],
          PrimaryButton(label: 'Verify code', onPressed: onVerify, isLoading: isSubmitting),
        ],
      ),
    );
  }
}

class _BiometricStep extends StatelessWidget {
  const _BiometricStep({
    required this.errorMessage,
    required this.isSubmitting,
    required this.onConfirm,
  });

  final String? errorMessage;
  final bool isSubmitting;
  final VoidCallback onConfirm;

  @override
  Widget build(BuildContext context) {
    return _WizardBody(
      title: 'Confirm with biometrics',
      subtitle: 'One last check to make sure this is really you.',
      child: Center(
        child: Icon(Icons.fingerprint, size: 96, color: Theme.of(context).colorScheme.primary),
      ),
      onNext: null,
      customAction: Column(
        children: [
          if (errorMessage != null) ...[
            Text(errorMessage!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            const SizedBox(height: 12),
          ],
          PrimaryButton(label: 'Confirm identity', onPressed: onConfirm, isLoading: isSubmitting),
        ],
      ),
    );
  }
}

class _SuccessStep extends StatelessWidget {
  const _SuccessStep({required this.request});

  final LimitRequestModel request;

  @override
  Widget build(BuildContext context) {
    final approved = request.status == 'APPROVED';

    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            approved ? Icons.check_circle : Icons.hourglass_top_rounded,
            size: 88,
            color: approved ? Theme.of(context).colorScheme.primary : Colors.orange,
          ),
          const SizedBox(height: 24),
          Text(
            approved ? 'Limit increased!' : "You're all set — almost there",
            style: Theme.of(context).textTheme.headlineMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 12),
          Text(
            approved
                ? 'Your daily transfer limit is now ${CurrencyFormatter.format(request.requestedLimit)}.'
                : 'Your request needs a quick manual review. Estimated response time: within 24 hours.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 32),
          PrimaryButton(
            label: approved ? 'Done' : 'View request status',
            onPressed: () {
              if (approved) {
                context.go(AppRoutes.home);
              } else {
                context.pushReplacement(AppRoutes.requestStatusPath(request.id));
              }
            },
          ),
        ],
      ),
    );
  }
}

/// Shared scaffolding for the title/subtitle/content/next-button layout used
/// by every step except the terminal success screen.
class _WizardBody extends StatelessWidget {
  const _WizardBody({
    required this.title,
    required this.subtitle,
    required this.child,
    required this.onNext,
    this.customAction,
  });

  final String title;
  final String subtitle;
  final Widget child;
  final VoidCallback? onNext;
  final Widget? customAction;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 8),
          Text(subtitle, style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 24),
          child,
          const SizedBox(height: 32),
          if (customAction != null)
            customAction!
          else
            PrimaryButton(label: 'Continue', onPressed: onNext),
        ],
      ),
    );
  }
}
