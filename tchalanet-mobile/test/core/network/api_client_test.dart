import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_client.dart';
import 'package:tchalanet_mobile/core/network/api_exception.dart';

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
        'category': 'service_unavailable',
        'retryPolicy': 'RETRY_SAME_INTENT',
        'retryable': true,
        'params': {'retryAfterSeconds': 30},
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
    expect(exception.category, 'service_unavailable');
    expect(exception.retryPolicy, 'RETRY_SAME_INTENT');
    expect(exception.retryable, isTrue);
    expect(exception.params['retryAfterSeconds'], 30);
    expect(exception.message, 'Erreur serveur');
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

  test('network failures resolve to stable user-safe translation keys', () {
    final exception = mapDioException(
      DioException.connectionError(
        requestOptions: RequestOptions(path: '/tenant/sales/preparations'),
        reason: 'unreachable',
      ),
    );

    expect(exception.code, 'client.network.unavailable');
    expect(exception.retryable, isTrue);
    expect(
      userErrorTranslationKeys(exception),
      contains('common.error.network'),
    );
  });

  test(
    'shared public/admin ProblemDetail fixtures map to mobile-safe ApiException',
    () {
      final publicProblem = _contractFixture(
        'problem-details/access-denied.json',
      );

      final publicException = mapDioException(
        DioException.badResponse(
          statusCode: 403,
          requestOptions: RequestOptions(path: '/public/tickets/verify'),
          response: Response<Map<String, dynamic>>(
            requestOptions: RequestOptions(path: '/public/tickets/verify'),
            statusCode: 403,
            data: publicProblem,
          ),
        ),
      );

      expect(publicException.code, 'access.denied');
      expect(publicException.category, 'access_denied');
      expect(publicException.retryPolicy, 'NEVER');
      expect(publicException.retryable, isFalse);
      expect(publicException.requestId, 'req_contract_denied');
      expect(publicException.errorId, 'err_contract_denied');
      expect(publicException.message, 'Erreur serveur');
      expect(publicException.message, isNot(publicProblem['detail']));

      final adminProblem = _contractFixture(
        'problem-details/validation-failed.json',
      );
      final adminException = mapDioException(
        DioException.badResponse(
          statusCode: 400,
          requestOptions: RequestOptions(path: '/admin/seller-terminals'),
          response: Response<Map<String, dynamic>>(
            requestOptions: RequestOptions(path: '/admin/seller-terminals'),
            statusCode: 400,
            data: adminProblem,
          ),
        ),
      );

      expect(adminException.code, 'validation.failed');
      expect(adminException.category, 'validation');
      expect(adminException.retryPolicy, 'AFTER_USER_ACTION');
      expect(adminException.retryable, isTrue);
      expect(userErrorTranslationKeys(adminException), [
        'common.errors.codes.validation.failed.message',
        'common.errors.categories.validation.message',
        'common.error.unknown',
      ]);
    },
  );
}

Map<String, dynamic> _contractFixture(String relativePath) {
  final file = File(
    '../tchalanet-server/testing/contracts/error-contract/v1/$relativePath',
  );
  return jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
}
