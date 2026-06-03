import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/cashier_home_models.dart';

// Must match ClientSurfaceResolver.HEADER_NAME on the server
const _surfaceHeader = 'X-Tch-Surface';
const _mobilePosValue = 'MOBILE_POS';

class CashierHomeService {
  const CashierHomeService(this._dio);

  final Dio _dio;

  Future<CashierHomeResponse> fetchHome() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/home',
        options: Options(headers: {_surfaceHeader: _mobilePosValue}),
      );
      final data = response.data?['data'] as Map<String, dynamic>?;
      if (data == null) throw const FormatException('empty home payload');
      return CashierHomeResponse.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<CashierReadinessResponse> fetchReadiness() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/readiness',
        options: Options(headers: {_surfaceHeader: _mobilePosValue}),
      );
      final data = response.data?['data'] as Map<String, dynamic>?;
      if (data == null) throw const FormatException('empty readiness payload');
      return CashierReadinessResponse.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final cashierHomeServiceProvider = Provider<CashierHomeService>(
  (ref) => CashierHomeService(ref.watch(apiClientProvider)),
);
