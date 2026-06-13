import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_client.dart';

void main() {
  test('ProblemDetail traceId is retained in mapped API exception', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/tickets'),
      statusCode: 503,
      data: {
        'detail': 'Service unavailable',
        'traceId': 'body-trace',
        'errorId': 'error-id',
        'code': 'SERVICE_UNAVAILABLE',
      },
      headers: Headers.fromMap({
        'X-Request-Id': ['header-trace'],
      }),
    );

    final exception = mapDioException(
      DioException.badResponse(
        statusCode: 503,
        requestOptions: response.requestOptions,
        response: response,
      ),
    );

    expect(exception.traceId, 'body-trace');
    expect(exception.errorId, 'error-id');
    expect(exception.code, 'SERVICE_UNAVAILABLE');
    expect(exception.statusCode, 503);
  });

  test('X-Request-Id header is mapped to requestId (not traceId)', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(
        path: '/tickets',
        headers: {'X-Request-Id': 'tch_req_sent'},
      ),
      statusCode: 500,
      data: {'detail': 'Server error'},
      headers: Headers.fromMap({
        'X-Request-Id': ['tch_req_echo'],
        'X-Trace-Id': ['trace-from-otel'],
      }),
    );

    final exception = mapDioException(
      DioException.badResponse(
        statusCode: 500,
        requestOptions: response.requestOptions,
        response: response,
      ),
    );

    // The sent request header takes priority over the echoed response header.
    expect(exception.requestId, 'tch_req_sent');
    // X-Trace-Id header maps to traceId.
    expect(exception.traceId, 'trace-from-otel');
  });
}
