import 'package:local_auth/local_auth.dart';

/// Wraps `local_auth` for the demo's "mock biometric" steps (login shortcut
/// and the biometric-confirmation step of the increase-limit wizard).
///
/// This is a real call to the platform's biometric prompt — there's no
/// banking-grade liveness/anti-spoof check behind it, which is exactly what
/// "mock" means here: it demonstrates the UX, not a production auth system.
class BiometricService {
  final _localAuth = LocalAuthentication();

  Future<bool> isAvailable() async {
    try {
      final canCheck = await _localAuth.canCheckBiometrics;
      final isSupported = await _localAuth.isDeviceSupported();
      return canCheck && isSupported;
    } catch (_) {
      return false;
    }
  }

  Future<bool> authenticate(String reason) async {
    try {
      return await _localAuth.authenticate(
        localizedReason: reason,
        biometricOnly: false,
        persistAcrossBackgrounding: true,
      );
    } catch (_) {
      return false;
    }
  }
}
