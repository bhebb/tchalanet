import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/cashier_sell_catalog_models.dart';

class CashierSellCatalogService {
  const CashierSellCatalogService(this._dio);

  final Dio _dio;

  Future<List<CashierAvailableDrawView>> fetchAvailableDraws({
    int lookaheadHours = 24,
    int limit = 20,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/draws/available',
        queryParameters: {
          'lookaheadHours': lookaheadHours,
          'limit': limit,
        },
      );
      final items = response.data?['data'] as List<dynamic>? ?? [];
      return items
          .map((e) => CashierAvailableDrawView.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<List<CashierGameOptionResponse>> fetchAvailableGames() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/games/available',
      );
      final items = response.data?['data'] as List<dynamic>? ?? [];
      return items
          .map((e) => CashierGameOptionResponse.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final cashierSellCatalogServiceProvider = Provider<CashierSellCatalogService>(
  (ref) => CashierSellCatalogService(ref.watch(apiClientProvider)),
);
