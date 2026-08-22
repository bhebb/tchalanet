import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/firebase_auth_token_client.dart';
import '../config/app_config.dart';
import '../network/auth_interceptor.dart';
import '../network/dev_cert_override.dart';
import '../network/request_id_interceptor.dart';
import '../observability/diagnostic_repository.dart';
import '../storage/secure_token_storage.dart';
import 'client_diagnostics_models.dart';

final clientDiagnosticsTransportProvider = Provider<ClientDiagnosticsTransport>(
  (ref) {
    final dio = Dio(
      BaseOptions(
        baseUrl: apiBaseUrl,
        connectTimeout: const Duration(seconds: 5),
        receiveTimeout: const Duration(seconds: 8),
        contentType: Headers.jsonContentType,
        headers: {'X-Tch-Client-Type': 'POS'},
      ),
    );
    if (kDebugMode) {
      dio.interceptors.add(
        InterceptorsWrapper(
          onResponse: (response, handler) {
            // ignore: avoid_print
            print(
              'TCH-DIAG ${response.requestOptions.method} '
              '${response.requestOptions.path} -> ${response.statusCode}',
            );
            handler.next(response);
          },
          onError: (error, handler) {
            final responseData = error.response?.data;
            // ignore: avoid_print
            print(
              'TCH-DIAG ${error.requestOptions.method} '
              '${error.requestOptions.path} -> ERROR ${error.type} '
              '${error.response?.statusCode ?? ''} ${error.message ?? ''} '
              '${responseData == null ? '' : responseData.toString()}',
            );
            handler.next(error);
          },
        ),
      );
    }
    dio.interceptors.add(
      RequestIdInterceptor(ref.read(diagnosticRepositoryProvider)),
    );
    dio.interceptors.add(
      AuthInterceptor(
        dio,
        ref.read(tokenStorageProvider),
        FirebaseAuthTokenClient(),
        () {},
      ),
    );
    applyDevCertOverride(dio);
    return ClientDiagnosticsTransport(dio);
  },
);

final clientDiagnosticsPublicTransportProvider =
    Provider<ClientDiagnosticsPublicTransport>((ref) {
      final dio = Dio(
        BaseOptions(
          baseUrl: apiBaseUrl,
          connectTimeout: const Duration(seconds: 5),
          receiveTimeout: const Duration(seconds: 8),
          contentType: Headers.jsonContentType,
          headers: {'X-Tch-Client-Type': 'POS'},
        ),
      );
      if (kDebugMode) {
        dio.interceptors.add(
          InterceptorsWrapper(
            onResponse: (response, handler) {
              // ignore: avoid_print
              print(
                'TCH-DIAG-PUBLIC ${response.requestOptions.method} '
                '${response.requestOptions.path} -> ${response.statusCode}',
              );
              handler.next(response);
            },
            onError: (error, handler) {
              // ignore: avoid_print
              print(
                'TCH-DIAG-PUBLIC ${error.requestOptions.method} '
                '${error.requestOptions.path} -> ERROR ${error.type} '
                '${error.response?.statusCode ?? ''} ${error.message ?? ''}',
              );
              handler.next(error);
            },
          ),
        );
      }
      dio.interceptors.add(
        RequestIdInterceptor(ref.read(diagnosticRepositoryProvider)),
      );
      applyDevCertOverride(dio);
      return ClientDiagnosticsPublicTransport(dio);
    });

class ClientDiagnosticsTransport {
  ClientDiagnosticsTransport(this._dio);

  final Dio _dio;

  Future<void> submit(List<ClientDiagnosticEvent> events) async {
    await _dio.post<Map<String, dynamic>>(
      '/tenant/client-diagnostics/events',
      data: {'events': events.map((event) => event.toJson()).toList()},
    );
  }
}

class ClientDiagnosticsPublicTransport {
  ClientDiagnosticsPublicTransport(this._dio);

  final Dio _dio;

  Future<ClientDiagnosticsPolicy> loadPolicy({
    required String terminalCode,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/public/client-diagnostics/policy',
      data: {'terminalCode': terminalCode},
    );
    return ClientDiagnosticsPolicy.fromJson(_payload(response.data));
  }

  Future<void> submit({
    required String terminalCode,
    required List<ClientDiagnosticEvent> events,
  }) async {
    await _dio.post<Map<String, dynamic>>(
      '/public/client-diagnostics/events',
      data: {
        'terminalCode': terminalCode,
        'events': events.map((event) => event.toJson()).toList(),
      },
    );
  }

  Map<dynamic, dynamic> _payload(Map<String, dynamic>? data) {
    final responseData = data ?? const <String, dynamic>{};
    final payload = responseData['data'];
    return payload is Map ? payload : responseData;
  }
}
