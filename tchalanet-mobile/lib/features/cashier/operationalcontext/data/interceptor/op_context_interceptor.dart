import 'package:dio/dio.dart';

import '../../../../../core/config/app_config.dart';
import '../storage/op_context_storage.dart';

/// Injects X-Tch-* operational context headers on every authenticated request.
///
/// Headers injected:
///   X-Tch-Terminal-Id      — terminal UUID (when set)
///   X-Tch-Outlet-Id        — outlet UUID (when set)
///   X-Tch-Sales-Session-Id — session UUID (when set, cleared between sessions)
///   X-Device-Binding       — device credential (when [posDeviceBinding] is set)
///
/// Trust level: CLIENT_CLAIM (WEAK) by default. When X-Device-Binding matches
/// the backend's seeded terminal_binding.credential_hash, the context is
/// upgraded to STRONG — required for selling. The dev binding default lives in
/// [posDeviceBinding]; the real per-device credential will come from the phone
/// terminal activation flow (future T12).
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
