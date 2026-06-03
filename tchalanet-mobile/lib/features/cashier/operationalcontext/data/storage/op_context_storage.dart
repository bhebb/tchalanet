import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

const _keyOutletId = 'op_ctx_outlet_id';
const _keyTerminalId = 'op_ctx_terminal_id';
const _keySalesSessionId = 'op_ctx_sales_session_id';

/// Persists the seller's selected operational context (outlet, terminal,
/// optional session) across app restarts.
///
/// Values are sent as X-Tch-* headers on every authenticated request via
/// [OpContextInterceptor]. Cleared on logout.
class OpContextStorage {
  const OpContextStorage(this._storage);

  final FlutterSecureStorage _storage;

  Future<String?> readOutletId() => _storage.read(key: _keyOutletId);
  Future<String?> readTerminalId() => _storage.read(key: _keyTerminalId);
  Future<String?> readSalesSessionId() => _storage.read(key: _keySalesSessionId);

  Future<void> saveSelection({
    required String outletId,
    required String terminalId,
  }) async {
    await _storage.write(key: _keyOutletId, value: outletId);
    await _storage.write(key: _keyTerminalId, value: terminalId);
  }

  Future<void> saveSessionId(String sessionId) =>
      _storage.write(key: _keySalesSessionId, value: sessionId);

  Future<void> clearSessionId() => _storage.delete(key: _keySalesSessionId);

  Future<void> clear() async {
    await _storage.delete(key: _keyOutletId);
    await _storage.delete(key: _keyTerminalId);
    await _storage.delete(key: _keySalesSessionId);
  }
}

final opContextStorageProvider = Provider<OpContextStorage>(
  (_) => const OpContextStorage(FlutterSecureStorage()),
);
