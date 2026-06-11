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

  test('X-Request-Id is used when ProblemDetail has no traceId', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/tickets'),
      statusCode: 500,
      data: {'detail': 'Server error'},
      headers: Headers.fromMap({
        'X-Request-Id': ['header-trace'],
      }),
    );

    final exception = mapDioException(
      DioException.badResponse(
        statusCode: 500,
        requestOptions: response.requestOptions,
        response: response,
      ),
    );

    expect(exception.traceId, 'header-trace');
  });
}
