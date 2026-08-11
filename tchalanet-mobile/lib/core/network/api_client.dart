import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/firebase_auth_token_client.dart';
import '../config/app_config.dart';
import '../i18n/i18n_repository.dart';
import '../notifications/app_notification_controller.dart';
import '../observability/diagnostic_repository.dart';
import '../storage/secure_token_storage.dart';
import 'api_exception.dart';
import 'api_notice_interceptor.dart';
import 'auth_interceptor.dart';
import 'dev_cert_override.dart';
import 'op_context_interceptor.dart';
import 'request_id_interceptor.dart';
import 'session_invalidation_controller.dart';

final apiClientProvider = Provider<Dio>((ref) {
  final tokenStorage = ref.read(tokenStorageProvider);
  final diagnostics = ref.read(diagnosticRepositoryProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      contentType: Headers.jsonContentType,
      headers: {
        // The backend identity bootstrap resolves this client as a
        // SellerTerminal (instead of an AppUser) only when this hint is set.
        'X-Tch-Client-Type': 'POS',
      },
    ),
  );

  if (kDebugMode) {
    // Dev-only wire log: one line per request outcome, greppable in logcat.
    dio.interceptors.add(
      InterceptorsWrapper(
        onResponse: (response, handler) {
          // ignore: avoid_print
          print(
            'TCH-NET ${response.requestOptions.method} '
            '${response.requestOptions.path} -> ${response.statusCode}',
          );
          handler.next(response);
        },
        onError: (error, handler) {
          // ignore: avoid_print
          print(
            'TCH-NET ${error.requestOptions.method} '
            '${error.requestOptions.path} -> ERROR ${error.type} '
            '${error.response?.statusCode ?? ''} ${error.message ?? ''}',
          );
          handler.next(error);
        },
      ),
    );
  }

  // RequestIdInterceptor must run first so the request ID is set before auth.
  dio.interceptors.add(RequestIdInterceptor(diagnostics));
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        options.headers['Accept-Language'] = ref.read(localeProvider);
        handler.next(options);
      },
    ),
  );
  dio.interceptors.add(
    AuthInterceptor(
      dio,
      tokenStorage,
      FirebaseAuthTokenClient(),
      ref.read(sessionInvalidationProvider.notifier).invalidate,
    ),
  );
  dio.interceptors.add(const OpContextInterceptor());
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
      code: 'client.network.timeout',
      category: 'network',
      retryable: true,
    ),
    DioExceptionType.badResponse => _apiExceptionFromBadResponse(e),
    DioExceptionType.connectionError => ApiException(
      message: 'Impossible de se connecter au serveur',
      code: 'client.network.unavailable',
      category: 'network',
      retryable: true,
    ),
    DioExceptionType.cancel => ApiException(
      message: 'Requête annulée',
      code: 'client.request.cancelled',
    ),
    _ => ApiException(
      message: 'Erreur réseau inattendue',
      code: 'client.unexpected',
    ),
  };
}

ApiException _apiExceptionFromBadResponse(DioException e) {
  final data = _normalizeProblemData(e.response?.data);
  return ApiException(
    message: _extractErrorMessage(data),
    statusCode: e.response?.statusCode,
    requestId: _extractRequestId(e, data),
    traceId: _extractTraceId(e.response, data),
    spanId: _extractSpanId(e.response),
    errorId: _extractString(data, 'errorId'),
    code: _extractString(data, 'code'),
    category: _extractString(data, 'category'),
    retryPolicy: _extractString(data, 'retryPolicy'),
    retryable: _extractBool(data, 'retryable'),
    params: _extractParams(data),
  );
}

String? _extractRequestId(DioException e, dynamic data) {
  // Prefer the outgoing request header (set by RequestIdInterceptor) over the
  // response echo, then the ProblemDetail body. Some proxy/security failures
  // may not echo headers but still carry requestId in the JSON contract.
  final sent = e.requestOptions.headers['X-Request-Id']?.toString();
  if (sent != null && sent.isNotEmpty) return sent;
  final echo = e.response?.headers.value('X-Request-Id');
  if (echo != null && echo.isNotEmpty) return echo;
  return _extractString(data, 'requestId');
}

String? _extractTraceId(Response<dynamic>? response, dynamic data) {
  final bodyTraceId = _extractString(data, 'traceId');
  if (bodyTraceId != null) return bodyTraceId;
  final headerTraceId = response?.headers.value('X-Trace-Id');
  return headerTraceId == null || headerTraceId.isEmpty ? null : headerTraceId;
}

String? _extractSpanId(Response<dynamic>? response) {
  final headerSpanId = response?.headers.value('X-Span-Id');
  return headerSpanId == null || headerSpanId.isEmpty ? null : headerSpanId;
}

String? _extractString(dynamic data, String key) {
  if (data is! Map) return null;
  final value = data[key]?.toString();
  return value == null || value.isEmpty ? null : value;
}

bool _extractBool(dynamic data, String key) {
  if (data is! Map) return false;
  return data[key] is bool && data[key] as bool;
}

Map<String, Object?> _extractParams(dynamic data) {
  if (data is! Map || data['params'] is! Map) return const {};
  return Map<String, Object?>.from(data['params'] as Map);
}

dynamic _normalizeProblemData(dynamic data) {
  if (data is Uint8List) {
    return _decodeJsonBytes(data);
  }
  if (data is List<int>) {
    return _decodeJsonBytes(data);
  }
  return data;
}

dynamic _decodeJsonBytes(List<int> data) {
  if (data.isEmpty) return data;
  try {
    final decoded = jsonDecode(utf8.decode(data));
    return decoded is Map<String, dynamic> ? decoded : data;
  } catch (_) {
    return data;
  }
}

String _extractErrorMessage(dynamic data) {
  // Detail, title and message are diagnostics, never a mobile display contract.
  // UI migration will resolve the stable code through the active locale bundle.
  return 'Erreur serveur';
}
