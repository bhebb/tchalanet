import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/request_id_interceptor.dart';
import 'package:tchalanet_mobile/core/observability/diagnostic_repository.dart';

/// Calls interceptor.onError and swallows the async error from
/// ErrorInterceptorHandler.next() (which completes an internal Future with an
/// error when called outside a real Dio context). The side effect we care
/// about — recording to DiagnosticRepository — happens before next() is called.
void _fireError(RequestIdInterceptor interceptor, DioException err) {
  runZonedGuarded(
    () => interceptor.onError(err, ErrorInterceptorHandler()),
    (_, __) {}, // swallow unhandled Future error from handler.next()
  );
}

void main() {
  late DiagnosticRepository diagnostics;
  late RequestIdInterceptor interceptor;

  setUp(() {
    diagnostics = DiagnosticRepository();
    interceptor = RequestIdInterceptor(diagnostics);
  });

  group('onRequest — header injection', () {
    test('adds X-Request-Id with tch_req_ prefix when absent', () {
      final options = RequestOptions(path: '/tenant/sell');
      interceptor.onRequest(options, RequestInterceptorHandler());

      final header = options.headers['X-Request-Id'] as String?;
      expect(header, isNotNull);
      expect(header, startsWith('tch_req_'));
    });

    test('does not overwrite existing X-Request-Id', () {
      const existing = 'tch_req_already-set';
      final options = RequestOptions(
        path: '/tenant/sell',
        headers: {'X-Request-Id': existing},
      );

      interceptor.onRequest(options, RequestInterceptorHandler());

      expect(options.headers['X-Request-Id'], existing);
    });

    test('generates unique IDs for each request', () {
      final ids = <String>{};
      for (var i = 0; i < 20; i++) {
        final options = RequestOptions(path: '/tenant/sell');
        interceptor.onRequest(options, RequestInterceptorHandler());
        ids.add(options.headers['X-Request-Id'] as String);
      }
      expect(ids.length, 20);
    });
  });

  group('onError — diagnostic recording', () {
    test('records DiagnosticInfo when response has X-Request-Id', () {
      final reqOptions = RequestOptions(
        path: '/tenant/sell',
        method: 'POST',
        headers: {'X-Request-Id': 'tch_req_abc123'},
      );
      final response = Response<Map<String, dynamic>>(
        requestOptions: reqOptions,
        statusCode: 400,
        data: {'detail': 'bad request'},
        headers: Headers.fromMap({'X-Trace-Id': ['trace-xyz']}),
      );

      _fireError(interceptor,
          DioException.badResponse(
            statusCode: 400,
            requestOptions: reqOptions,
            response: response,
          ));

      final info = diagnostics.last;
      expect(info, isNotNull);
      expect(info!.requestId, 'tch_req_abc123');
      expect(info.traceId, 'trace-xyz');
      expect(info.route, '/tenant/sell');
      expect(info.operation, 'POST');
    });

    test('records traceId from body when X-Trace-Id header absent', () {
      final reqOptions = RequestOptions(
        path: '/tenant/tickets',
        method: 'POST',
        headers: {'X-Request-Id': 'tch_req_xyz'},
      );
      final response = Response<Map<String, dynamic>>(
        requestOptions: reqOptions,
        statusCode: 503,
        data: {'detail': 'unavailable', 'traceId': 'body-trace'},
      );

      _fireError(interceptor,
          DioException.badResponse(
            statusCode: 503,
            requestOptions: reqOptions,
            response: response,
          ));

      expect(diagnostics.last?.traceId, 'body-trace');
    });

    test('does not record when neither requestId nor traceId available', () {
      final reqOptions = RequestOptions(path: '/public/catalog');
      final response = Response<dynamic>(
        requestOptions: reqOptions,
        statusCode: 500,
        data: null,
      );

      _fireError(interceptor,
          DioException.badResponse(
            statusCode: 500,
            requestOptions: reqOptions,
            response: response,
          ));

      expect(diagnostics.last, isNull);
    });

    test('retains last 5 entries and evicts oldest', () {
      for (var i = 0; i < 6; i++) {
        final reqOptions = RequestOptions(
          path: '/tenant/sell',
          headers: {'X-Request-Id': 'tch_req_$i'},
        );
        _fireError(interceptor,
            DioException.badResponse(
              statusCode: 400,
              requestOptions: reqOptions,
              response: Response<dynamic>(
                  requestOptions: reqOptions, statusCode: 400, data: null),
            ));
      }

      expect(diagnostics.all.length, 5);
      expect(diagnostics.last?.requestId, 'tch_req_5');
    });
  });

  group('DiagnosticInfo.toCopyText', () {
    test('formats copy text with all trace fields, no PII', () {
      final reqOptions = RequestOptions(
        path: '/tenant/sell',
        method: 'POST',
        headers: {'X-Request-Id': 'tch_req_copytest'},
      );
      final response = Response<dynamic>(
        requestOptions: reqOptions,
        statusCode: 422,
        data: null,
        headers: Headers.fromMap({
          'X-Trace-Id': ['t-abc'],
          'X-Span-Id': ['s-def'],
        }),
      );

      _fireError(interceptor,
          DioException.badResponse(
            statusCode: 422,
            requestOptions: reqOptions,
            response: response,
          ));

      final text = diagnostics.last!.toCopyText();
      expect(text, contains('requestId=tch_req_copytest'));
      expect(text, contains('traceId=t-abc'));
      expect(text, contains('spanId=s-def'));
      expect(text, contains('operation=POST'));
      expect(text, contains('time='));
      expect(text, isNot(contains('Authorization')));
      expect(text, isNot(contains('Bearer')));
      expect(text, isNot(contains('@')));
    });
  });
}
