import 'package:dio/dio.dart';

import '../config/app_config.dart';

/// Attaches the device-binding header on `/tenant/**` calls.
///
/// The SellerTerminal identity and its operational context are derived
/// server-side from the auth token — the client no longer sends outlet,
/// terminal or sales-session headers.
class OpContextInterceptor extends Interceptor {
  const OpContextInterceptor();

  static const _headerDeviceBinding = 'X-Device-Binding';

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) {
    if (options.path.startsWith('/tenant/') && posDeviceBinding.isNotEmpty) {
      options.headers[_headerDeviceBinding] = posDeviceBinding;
    }
    handler.next(options);
  }
}
