/// A friendly, user-facing error. Screens should never surface raw
/// Dio/HTTP/stack-trace text — everything gets translated into one of these
/// before it reaches the UI.
class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}
