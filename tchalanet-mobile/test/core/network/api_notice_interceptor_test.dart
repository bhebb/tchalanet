import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_notice.dart';
import 'package:tchalanet_mobile/core/network/api_notice_interceptor.dart';

void main() {
  test('extracts ApiResponse notices with the response request id', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/sell'),
      headers: Headers.fromMap({
        'X-Request-Id': ['trace-123'],
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
    expect(notice.traceId, 'trace-123');
  });

  test('ignores responses without a valid notices list', () {
    final response = Response<Map<String, dynamic>>(
      requestOptions: RequestOptions(path: '/summary'),
      data: {'status': 'SUCCESS', 'data': {}, 'notices': null},
    );

    expect(extractApiNotices(response), isEmpty);
  });
}
