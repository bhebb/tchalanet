import 'dart:async';

import 'package:dio/dio.dart';

import 'client_diagnostics_models.dart';
import 'client_diagnostics_reporter.dart';

class ApiClientDiagnosticsInterceptor extends Interceptor {
  ApiClientDiagnosticsInterceptor(this._reporter);

  final ClientDiagnosticsReporter _reporter;

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final path = response.requestOptions.path;
    if (!path.contains('/client-diagnostics/')) {
      unawaited(_reporter.flush());
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final path = err.requestOptions.path;
    if (!path.contains('/client-diagnostics/')) {
      final problem = err.response?.data;
      final problemMap = problem is Map ? problem : const {};
      final requestId =
          err.requestOptions.headers['X-Request-Id']?.toString() ??
          err.response?.headers.value('X-Request-Id') ??
          problemMap['requestId']?.toString();
      _reporter.record(
        ClientDiagnosticEvent(
          category: _categoryFor(err),
          occurredAtClient: DateTime.now().toUtc(),
          severity: ClientDiagnosticSeverity.error,
          operation: '${err.requestOptions.method} $path',
          errorCode: problemMap['code']?.toString() ?? err.type.name,
          message: err.message,
          exceptionType: err.runtimeType.toString(),
          requestId: requestId,
          correlationId: problemMap['traceId']?.toString(),
          httpStatus: err.response?.statusCode,
          endpointKey: path,
          platform: 'android',
        ),
      );
    }
    handler.next(err);
  }
}

ClientDiagnosticCategory _categoryFor(DioException err) {
  return switch (err.type) {
    DioExceptionType.connectionTimeout ||
    DioExceptionType.sendTimeout ||
    DioExceptionType.receiveTimeout ||
    DioExceptionType.connectionError => ClientDiagnosticCategory.connectivity,
    _ =>
      _isSalePath(err.requestOptions.path)
          ? ClientDiagnosticCategory.sale
          : ClientDiagnosticCategory.api,
  };
}

bool _isSalePath(String path) =>
    path.contains('/cashier/tickets') ||
    path.contains('/cashier/sales') ||
    path.contains('/tenant/pos/sale');
