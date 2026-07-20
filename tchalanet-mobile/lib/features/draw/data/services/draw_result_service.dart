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

  Future<List<PublicDrawResultSlot>> fetchResultSlots() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/draw-results/slots',
      );
      final items = (response.data?['data']?['items'] as List<dynamic>?) ?? [];
      return items
          .map(
            (item) =>
                PublicDrawResultSlot.fromJson(item as Map<String, dynamic>),
          )
          .where((slot) => slot.slotKey.isNotEmpty)
          .toList(growable: false);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<PublicDrawResultHistory> fetchHistory({
    required DateTime from,
    required DateTime to,
    String? slotKey,
    int page = 0,
    int size = 50,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/draw-results/history',
        queryParameters: {
          if (slotKey != null && slotKey.isNotEmpty) 'slotKeys': [slotKey],
          'from': _isoDate(from),
          'to': _isoDate(to),
          'page': page,
          'size': size,
        },
      );
      return PublicDrawResultHistory.fromJson(
        response.data?['data'] as Map<String, dynamic>? ?? const {},
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

String _isoDate(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-'
    '${value.month.toString().padLeft(2, '0')}-'
    '${value.day.toString().padLeft(2, '0')}';

final drawResultServiceProvider = Provider<DrawResultService>(
  (ref) => DrawResultService(ref.watch(apiClientProvider)),
);
