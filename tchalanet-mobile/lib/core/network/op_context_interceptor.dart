import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/op_context_storage.dart';

class OpContextInterceptor extends Interceptor {
  OpContextInterceptor(this._storage);

  final OpContextStorage _storage;

  static const _headerTerminalId = 'X-Tch-Terminal-Id';
  static const _headerOutletId = 'X-Tch-Outlet-Id';
  static const _headerSessionId = 'X-Tch-Sales-Session-Id';
  static const _headerDeviceBinding = 'X-Device-Binding';

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!options.path.startsWith('/tenant/')) {
      handler.next(options);
      return;
    }

    final terminalId = await _storage.readTerminalId();
    final outletId = await _storage.readOutletId();
    final sessionId = await _storage.readSalesSessionId();

    if (terminalId != null) options.headers[_headerTerminalId] = terminalId;
    if (outletId != null) options.headers[_headerOutletId] = outletId;
    if (sessionId != null) options.headers[_headerSessionId] = sessionId;
    if (posDeviceBinding.isNotEmpty) {
      options.headers[_headerDeviceBinding] = posDeviceBinding;
    }

    handler.next(options);
  }
}
