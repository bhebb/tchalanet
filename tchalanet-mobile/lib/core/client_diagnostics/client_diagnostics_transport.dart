import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/firebase_auth_token_client.dart';
import '../config/app_config.dart';
import '../network/auth_interceptor.dart';
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
    dio.interceptors.add(
      AuthInterceptor(
        dio,
        ref.read(tokenStorageProvider),
        FirebaseAuthTokenClient(),
        () {},
      ),
    );
    return ClientDiagnosticsTransport(dio);
  },
);

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
