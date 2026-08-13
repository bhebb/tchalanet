import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/pos_draw_detail_models.dart';

class PosDrawDetailService {
  const PosDrawDetailService(this._dio);

  final Dio _dio;

  Future<PosDrawDetailResponse> fetchDrawDetail(String drawId) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/draws/$drawId/detail',
      );
      final data = response.data?['data'] as Map<String, dynamic>?;
      if (data == null) {
        throw const FormatException('empty draw detail payload');
      }
      return PosDrawDetailResponse.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final posDrawDetailServiceProvider = Provider<PosDrawDetailService>((ref) {
  return PosDrawDetailService(ref.watch(apiClientProvider));
});
