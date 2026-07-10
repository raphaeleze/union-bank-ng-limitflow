import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/providers.dart';
import '../../../core/session/app_user.dart';
import '../../../core/widgets/primary_button.dart';
import '../application/auth_repository.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  static const _cachedUserKey = 'cached_user';

  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController(text: 'customer@limitflow.demo');
  final _passwordController = TextEditingController();

  bool _rememberMe = true;
  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final result = await ref.read(authRepositoryProvider).login(
            email: _emailController.text.trim(),
            password: _passwordController.text,
          );
      await _completeLogin(result.token, result.user);
    } on ApiException catch (error) {
      setState(() => _errorMessage = error.message);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _completeLogin(String token, AppUser user) async {
    if (_rememberMe) {
      await ref.read(localCacheServiceProvider).writeJson(_cachedUserKey, {
        'id': user.id,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'email': user.email,
        'role': user.role,
      });
    }
    await ref.read(sessionControllerProvider.notifier).login(token: token, user: user);
  }

  Future<void> _biometricLogin() async {
    final cache = ref.read(localCacheServiceProvider);
    final cachedJson = cache.readJson(_cachedUserKey);
    if (cachedJson == null) {
      setState(() => _errorMessage = 'Log in with your password first to enable biometric login.');
      return;
    }

    final biometrics = ref.read(biometricServiceProvider);
    final ok = await biometrics.authenticate('Log in to LimitFlow');
    if (!ok) return;

    try {
      final freshUser = await ref.read(authRepositoryProvider).fetchCurrentUser();
      final token = await ref.read(secureStorageServiceProvider).readToken();
      if (token == null) {
        setState(() => _errorMessage = 'Your session expired. Please log in with your password.');
        return;
      }
      await _completeLogin(token, freshUser);
    } on ApiException {
      setState(() => _errorMessage = 'Your session expired. Please log in with your password.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Welcome back', style: Theme.of(context).textTheme.headlineMedium),
                const SizedBox(height: 8),
                Text(
                  'Log in to manage your transfer limit.',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: 32),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(labelText: 'Email'),
                  validator: (value) =>
                      (value == null || !value.contains('@')) ? 'Enter a valid email' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _passwordController,
                  obscureText: _obscurePassword,
                  decoration: InputDecoration(
                    labelText: 'Password',
                    suffixIcon: IconButton(
                      icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility),
                      onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                    ),
                  ),
                  validator: (value) =>
                      (value == null || value.isEmpty) ? 'Enter your password' : null,
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Checkbox(
                          value: _rememberMe,
                          onChanged: (value) => setState(() => _rememberMe = value ?? true),
                        ),
                        const Text('Remember me'),
                      ],
                    ),
                    TextButton(
                      onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Contact support to reset your password.')),
                      ),
                      child: const Text('Forgot password?'),
                    ),
                  ],
                ),
                if (_errorMessage != null) ...[
                  const SizedBox(height: 8),
                  Text(_errorMessage!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                ],
                const SizedBox(height: 16),
                PrimaryButton(label: 'Log in', onPressed: _submit, isLoading: _isLoading),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: _isLoading ? null : _biometricLogin,
                  icon: const Icon(Icons.fingerprint),
                  label: const Text('Log in with biometrics'),
                  style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
