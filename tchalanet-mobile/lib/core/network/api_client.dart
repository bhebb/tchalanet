import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/cashier/operationalcontext/data/interceptor/op_context_interceptor.dart';
import '../../features/cashier/operationalcontext/data/storage/op_context_storage.dart';
import '../config/app_config.dart';
import '../notifications/app_notification_controller.dart';
import '../storage/secure_token_storage.dart';
import 'api_exception.dart';
import 'api_notice_interceptor.dart';
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
  dio.interceptors.add(
    ApiNoticeInterceptor(
      ref.read(appNotificationProvider.notifier).showApiNotice,
    ),
  );

  // Dev-only: trust the local mkcert CA on *.localtest.me (no-op in release/web).
  applyDevCertOverride(dio);

  return dio;
});

ApiException mapDioException(DioException e) {
  return switch (e.type) {
    DioExceptionType.connectionTimeout ||
    DioExceptionType.sendTimeout ||
    DioExceptionType.receiveTimeout => ApiException(
      message: 'Délai de connexion dépassé',
    ),
    DioExceptionType.badResponse => ApiException(
      message: _extractErrorMessage(e.response?.data),
      statusCode: e.response?.statusCode,
      traceId: _extractTraceId(e.response),
      errorId: _extractString(e.response?.data, 'errorId'),
      code: _extractString(e.response?.data, 'code'),
    ),
    DioExceptionType.connectionError => ApiException(
      message: 'Impossible de se connecter au serveur',
    ),
    DioExceptionType.cancel => ApiException(message: 'Requête annulée'),
    _ => ApiException(message: 'Erreur réseau inattendue'),
  };
}

String? _extractTraceId(Response<dynamic>? response) {
  final bodyTraceId = _extractString(response?.data, 'traceId');
  if (bodyTraceId != null) return bodyTraceId;
  final headerTraceId = response?.headers.value('X-Request-Id');
  return headerTraceId == null || headerTraceId.isEmpty ? null : headerTraceId;
}

String? _extractString(dynamic data, String key) {
  if (data is! Map) return null;
  final value = data[key]?.toString();
  return value == null || value.isEmpty ? null : value;
}

String _extractErrorMessage(dynamic data) {
  if (data is Map<String, dynamic>) {
    return (data['detail'] ?? data['message'] ?? data['title'])?.toString() ??
        'Erreur serveur';
  }
  return 'Erreur serveur';
}
