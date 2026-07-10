import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/secure_storage_service.dart';
import 'api_exception.dart';

/// Thin wrapper around [Dio] that attaches the JWT to every request and
/// converts transport-level failures into friendly [ApiException]s.
class ApiClient {
  ApiClient(this._secureStorage, {this.onUnauthorized}) {
    _dio = Dio(
      BaseOptions(
        baseUrl: AppConfig.apiBaseUrl,
        connectTimeout: AppConfig.connectTimeout,
        receiveTimeout: AppConfig.receiveTimeout,
        contentType: 'application/json',
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _secureStorage.readToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) {
          if (error.response?.statusCode == 401) {
            onUnauthorized?.call();
          }
          handler.next(error);
        },
      ),
    );
  }

  final SecureStorageService _secureStorage;
  final void Function()? onUnauthorized;
  late final Dio _dio;

  Future<Map<String, dynamic>> get(String path, {Map<String, dynamic>? query}) async {
    final response = await _run(() => _dio.get(path, queryParameters: query));
    return response.data as Map<String, dynamic>;
  }

  Future<List<dynamic>> getList(String path, {Map<String, dynamic>? query}) async {
    final response = await _run(() => _dio.get(path, queryParameters: query));
    return response.data as List<dynamic>;
  }

  Future<Map<String, dynamic>> post(String path, {Object? body}) async {
    final response = await _run(() => _dio.post(path, data: body));
    return response.data as Map<String, dynamic>;
  }

  Future<Response> _run(Future<Response> Function() request) async {
    try {
      return await request();
    } on DioException catch (error) {
      throw _translate(error);
    }
  }

  ApiException _translate(DioException error) {
    final data = error.response?.data;
    final statusCode = error.response?.statusCode;

    if (data is Map && data['message'] is String) {
      return ApiException(data['message'] as String, statusCode: statusCode);
    }

    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return const ApiException('This is taking longer than expected. Please try again.');
      case DioExceptionType.connectionError:
        return const ApiException("We couldn't reach LimitFlow. Check your connection and try again.");
      default:
        return const ApiException('Something went wrong. Please try again.');
    }
  }
}
