import 'package:dio/dio.dart';

import 'api_notice.dart';

class ApiNoticeInterceptor extends Interceptor {
  ApiNoticeInterceptor(this._onNotice);

  final void Function(ApiNotice notice) _onNotice;

  @override
  void onResponse(
    Response<dynamic> response,
    ResponseInterceptorHandler handler,
  ) {
    for (final notice in extractApiNotices(response)) {
      _onNotice(notice);
    }
    handler.next(response);
  }
}

List<ApiNotice> extractApiNotices(Response<dynamic> response) {
  final body = response.data;
  if (body is! Map || body['notices'] is! List) return const [];
  final traceId = response.headers.value('X-Request-Id');
  return [
    for (final raw in body['notices'] as List)
      if (raw is Map)
        ApiNotice.fromJson(Map<String, dynamic>.from(raw), traceId: traceId),
  ];
}
