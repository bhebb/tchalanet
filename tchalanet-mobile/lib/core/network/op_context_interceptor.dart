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
  static const _headerSurface = 'X-Tch-Surface';
  static const _mobilePosSurface = 'MOBILE_POS';

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (options.path.startsWith('/tenant/')) {
      options.headers.putIfAbsent(_headerSurface, () => _mobilePosSurface);
      if (posDeviceBinding.isNotEmpty) {
        options.headers[_headerDeviceBinding] = posDeviceBinding;
      }
    }
    handler.next(options);
  }
}
