import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/op_context_interceptor.dart';

void main() {
  test('adds POS surface and device binding to tenant requests', () {
    const interceptor = OpContextInterceptor();
    final options = RequestOptions(path: '/tenant/sales/preparations');

    interceptor.onRequest(options, RequestInterceptorHandler());

    expect(options.headers['X-Tch-Surface'], 'MOBILE_POS');
    expect(options.headers['X-Device-Binding'], 'e2e-cred-dev');
  });

  test('does not override an explicit tenant surface', () {
    const interceptor = OpContextInterceptor();
    final options = RequestOptions(
      path: '/tenant/cashier/home',
      headers: {'X-Tch-Surface': 'CUSTOM_POS'},
    );

    interceptor.onRequest(options, RequestInterceptorHandler());

    expect(options.headers['X-Tch-Surface'], 'CUSTOM_POS');
  });

  test('does not add POS context to public requests', () {
    const interceptor = OpContextInterceptor();
    final options = RequestOptions(path: '/public/runtime/bootstrap');

    interceptor.onRequest(options, RequestInterceptorHandler());

    expect(options.headers.containsKey('X-Tch-Surface'), isFalse);
    expect(options.headers.containsKey('X-Device-Binding'), isFalse);
  });
}
