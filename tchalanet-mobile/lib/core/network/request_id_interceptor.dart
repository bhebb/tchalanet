import 'dart:math';

import 'package:dio/dio.dart';

import '../observability/diagnostic_info.dart';
import '../observability/diagnostic_repository.dart';

class RequestIdInterceptor extends Interceptor {
  RequestIdInterceptor(this._diagnostics);

  static const _xRequestId = 'X-Request-Id';
  static const _xTraceId = 'X-Trace-Id';
  static const _xSpanId = 'X-Span-Id';

  final DiagnosticRepository _diagnostics;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (!options.headers.containsKey(_xRequestId)) {
      options.headers[_xRequestId] = _generateRequestId();
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final res = err.response;
    final requestId = err.requestOptions.headers[_xRequestId]?.toString() ??
        res?.headers.value(_xRequestId);
    final traceId = res?.headers.value(_xTraceId) ??
        (res?.data is Map ? res?.data['traceId']?.toString() : null);
    final spanId = res?.headers.value(_xSpanId);

    if (requestId != null || traceId != null) {
      _diagnostics.record(DiagnosticInfo(
        requestId: requestId,
        traceId: traceId,
        spanId: spanId,
        route: err.requestOptions.path,
        operation: err.requestOptions.method,
        occurredAt: DateTime.now(),
      ));
    }

    handler.next(err);
  }

  static String _generateRequestId() {
    final rand = Random.secure();
    final bytes = List.generate(16, (_) => rand.nextInt(256));
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return 'tch_req_$hex';
  }
}
