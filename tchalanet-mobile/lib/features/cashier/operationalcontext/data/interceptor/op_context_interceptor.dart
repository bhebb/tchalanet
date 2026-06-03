import 'package:dio/dio.dart';

import '../storage/op_context_storage.dart';

/// Injects X-Tch-* operational context headers on every authenticated request.
///
/// Headers injected:
///   X-Tch-Terminal-Id      — terminal UUID (when set)
///   X-Tch-Outlet-Id        — outlet UUID (when set)
///   X-Tch-Sales-Session-Id — session UUID (when set, cleared between sessions)
///
/// Source: CLIENT_CLAIM (WEAK trust). STRONG trust requires device binding
/// via the phone terminal activation flow (future T12).
class OpContextInterceptor extends Interceptor {
  OpContextInterceptor(this._storage);

  final OpContextStorage _storage;

  static const _headerTerminalId = 'X-Tch-Terminal-Id';
  static const _headerOutletId = 'X-Tch-Outlet-Id';
  static const _headerSessionId = 'X-Tch-Sales-Session-Id';

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

    handler.next(options);
  }
}
