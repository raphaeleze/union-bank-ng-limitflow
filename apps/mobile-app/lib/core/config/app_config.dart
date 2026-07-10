import 'dart:io' show Platform;

/// Central place for environment-dependent configuration.
///
/// Override at build/run time with:
///   flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080/api
class AppConfig {
  AppConfig._();

  static const String _override = String.fromEnvironment('API_BASE_URL');

  /// The Android emulator can't reach the host machine via `localhost`; it
  /// needs the special `10.0.2.2` alias instead. iOS simulators and physical
  /// devices connected over the same network don't have that restriction.
  static String get apiBaseUrl {
    if (_override.isNotEmpty) return _override;
    final host = Platform.isAndroid ? '10.0.2.2' : 'localhost';
    return 'http://$host:8080/api';
  }

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 10);
}
