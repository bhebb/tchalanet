import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/cashier_session_models.dart';

class CashierSessionService {
  const CashierSessionService(this._dio);

  final Dio _dio;

  /// 204 if no open session for this terminal.
  Future<CashierSessionView?> fetchCurrent(String terminalId) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/session/current',
        queryParameters: {'terminalId': terminalId},
      );
      if (response.statusCode == 204 || response.data == null) return null;
      final data = response.data!['data'] as Map<String, dynamic>?;
      return data != null ? CashierSessionView.fromJson(data) : null;
    } on DioException catch (e) {
      if (e.response?.statusCode == 204) return null;
      throw mapDioException(e);
    }
  }

  Future<CashierSessionView> open(OpenSessionRequest request) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tenant/cashier/session/open',
        data: request.toJson(),
      );
      final data = response.data!['data'] as Map<String, dynamic>;
      return CashierSessionView.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<CashierSessionView> close(CloseSessionRequest request) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tenant/cashier/session/close',
        data: request.toJson(),
      );
      final data = response.data!['data'] as Map<String, dynamic>;
      return CashierSessionView.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final cashierSessionServiceProvider = Provider<CashierSessionService>(
  (ref) => CashierSessionService(ref.watch(apiClientProvider)),
);
