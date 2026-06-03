import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/op_context_options.dart';
import '../storage/op_context_storage.dart';

class CashierOpContextService {
  const CashierOpContextService(this._dio, this._storage);

  final Dio _dio;
  final OpContextStorage _storage;

  Future<OpContextOptionsView> fetchOptions() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/operational-context/options',
      );
      final data = response.data?['data'] as Map<String, dynamic>?;
      if (data == null) throw const FormatException('empty options payload');
      return OpContextOptionsView.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Saves the selection to secure storage so headers are sent on all requests.
  /// Does NOT call POST /select (session not available yet at setup time).
  Future<void> saveSelection({
    required String outletId,
    required String terminalId,
  }) =>
      _storage.saveSelection(outletId: outletId, terminalId: terminalId);

  Future<void> clearSelection() => _storage.clear();
}

final cashierOpContextServiceProvider = Provider<CashierOpContextService>(
  (ref) => CashierOpContextService(
    ref.watch(apiClientProvider),
    ref.watch(opContextStorageProvider),
  ),
);
