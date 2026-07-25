import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_notice.dart';
import 'package:tchalanet_mobile/core/network/api_notice_interceptor.dart';

void main() {
  test('extracts ApiResponse notices with the response trace id', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/sell'),
      headers: Headers.fromMap({
        'X-Request-Id': ['trace-123'],
        'X-Trace-Id': ['otel-trace-123'],
      }),
      data: {
        'status': 'SUCCESS_WITH_WARNINGS',
        'data': {'ticketId': 'ticket-1'},
        'notices': [
          {
            'code': 'LIMIT_WARN',
            'message': 'Limit almost reached',
            'domain': 'sales',
            'severity': 'WARN',
            'meta': {'remaining': 2},
          },
        ],
      },
    );

    final notice = extractApiNotices(response).single;

    expect(notice.code, 'LIMIT_WARN');
    expect(notice.severity, ApiNoticeSeverity.warning);
    expect(notice.domain, 'sales');
    expect(notice.meta['remaining'], 2);
    expect(notice.traceId, 'otel-trace-123');
  });

  test('ignores responses without a valid notices list', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/summary'),
      data: {'status': 'SUCCESS', 'data': {}, 'notices': null},
    );

    expect(extractApiNotices(response), isEmpty);
  });

  test('shared ApiResponse warning fixture keeps structured notice fields', () {
    final fixture = _contractFixture('api-responses/success-with-warning.json');
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/tenant/setup'),
      data: fixture,
    );

    final notice = extractApiNotices(response).single;

    expect(notice.code, 'tenant.setup.optional_step_missing');
    expect(notice.message, isEmpty);
    expect(notice.domain, 'tenant.setup');
    expect(notice.severity, ApiNoticeSeverity.warning);
    expect(notice.kind, 'BUSINESS');
    expect(notice.source, 'tenantSetup');
    expect(notice.target, 'setup.optional');
    expect(notice.params, {'completedSteps': 3, 'totalSteps': 5});
    expect(notice.requestId, 'req_contract_warning');
    expect(notice.traceId, 'trace-contract-warning');
    expect(notice.errorId, 'err_contract_warning');
  });

  test('shared ApiResponse partial fixture keeps degradation target local', () {
    final fixture = _contractFixture(
      'api-responses/partial-section-degradation.json',
    );
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/tenant/dashboard'),
      data: fixture,
    );

    final notice = extractApiNotices(response).single;

    expect(fixture['status'], 'PARTIAL');
    expect(notice.code, 'admin.dashboard.provider_results_unavailable');
    expect(notice.message, isEmpty);
    expect(notice.kind, 'DEGRADATION');
    expect(notice.source, 'providerResults');
    expect(notice.target, 'admin_dashboard.provider_results');
    expect(notice.traceId, 'trace-contract-partial');
    expect(notice.errorId, 'err_contract_partial');
  });
}

Map<String, dynamic> _contractFixture(String relativePath) {
  final file = File(
    '../tchalanet-server/testing/contracts/error-contract/v1/$relativePath',
  );
  return jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
}
