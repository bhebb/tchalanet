import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/cashier/operationalcontext/data/interceptor/op_context_interceptor.dart';
import '../../features/cashier/operationalcontext/data/storage/op_context_storage.dart';
import '../config/app_config.dart';
import '../storage/secure_token_storage.dart';
import 'api_exception.dart';
import 'auth_interceptor.dart';

final apiClientProvider = Provider<Dio>((ref) {
  final tokenStorage = ref.watch(tokenStorageProvider);
  final opCtxStorage = ref.watch(opContextStorageProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
    ),
  );

  dio.interceptors.add(AuthInterceptor(tokenStorage));
  dio.interceptors.add(OpContextInterceptor(opCtxStorage));

  return dio;
});

ApiException mapDioException(DioException e) {
  return switch (e.type) {
    DioExceptionType.connectionTimeout ||
    DioExceptionType.sendTimeout ||
    DioExceptionType.receiveTimeout =>
      ApiException(message: 'Délai de connexion dépassé'),
    DioExceptionType.badResponse => ApiException(
        message: e.response?.data?['message']?.toString() ?? 'Erreur serveur',
        statusCode: e.response?.statusCode,
      ),
    DioExceptionType.connectionError =>
      ApiException(message: 'Impossible de se connecter au serveur'),
    _ => ApiException(message: 'Erreur réseau inattendue'),
  };
}
