import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/cashier/operationalcontext/data/interceptor/op_context_interceptor.dart';
import '../../features/cashier/operationalcontext/data/storage/op_context_storage.dart';
import '../config/app_config.dart';
import '../storage/secure_token_storage.dart';
import 'api_exception.dart';
import 'auth_interceptor.dart';
import 'dev_cert_override.dart';

final apiClientProvider = Provider<Dio>((ref) {
  final tokenStorage = ref.read(tokenStorageProvider);
  final opCtxStorage = ref.read(opContextStorageProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      contentType: Headers.jsonContentType,
    ),
  );

  dio.interceptors.add(AuthInterceptor(tokenStorage));
  dio.interceptors.add(OpContextInterceptor(opCtxStorage));

  // Dev-only: trust the local mkcert CA on *.localtest.me (no-op in release/web).
  applyDevCertOverride(dio);

  return dio;
});

ApiException mapDioException(DioException e) {
  return switch (e.type) {
    DioExceptionType.connectionTimeout ||
    DioExceptionType.sendTimeout ||
    DioExceptionType.receiveTimeout =>
      ApiException(message: 'Délai de connexion dépassé'),
    DioExceptionType.badResponse => ApiException(
        message: _extractErrorMessage(e.response?.data),
        statusCode: e.response?.statusCode,
      ),
    DioExceptionType.connectionError =>
      ApiException(message: 'Impossible de se connecter au serveur'),
    DioExceptionType.cancel =>
      ApiException(message: 'Requête annulée'),
    _ => ApiException(message: 'Erreur réseau inattendue'),
  };
}

String _extractErrorMessage(dynamic data) {
  if (data is Map<String, dynamic>) {
    return (data['detail'] ?? data['message'] ?? data['title'])?.toString() ??
        'Erreur serveur';
  }
  return 'Erreur serveur';
}
