import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/draw_models.dart';

class DrawResultService {
  const DrawResultService(this._dio);

  final Dio _dio;

  Future<List<DrawSlotView>> fetchSlots({int historyLimit = 1}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/draw-results/slots/details',
        queryParameters: {'historyLimit': historyLimit},
      );
      final items = (response.data?['data']?['items'] as List<dynamic>?) ?? [];
      return items
          .map((e) => DrawSlotView.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final drawResultServiceProvider = Provider<DrawResultService>(
  (ref) => DrawResultService(ref.watch(apiClientProvider)),
);
