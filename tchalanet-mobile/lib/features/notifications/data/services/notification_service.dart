import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../models/notification_models.dart';

class NotificationService {
  const NotificationService(this._dio);

  final Dio _dio;

  /// Seller-terminal notification inbox. This actor-scoped endpoint is the one
  /// that surfaces tenant broadcasts (admin → `TENANT_SELLER_TERMINALS`), not
  /// the generic `/tenant/me/notifications` (tenant-user) variant.
  static const _base = '/tenant/seller-terminal/me/notifications';

  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        _base,
        queryParameters: {
          'page': page,
          'size': size,
          if (status != null) 'status': status.name.toUpperCase(),
        },
      );
      return NotificationPage.fromJson(_data(response));
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }

  Future<void> markRead(String id) => _post('$_base/$id/read');

  Future<void> archive(String id) => _post('$_base/$id/archive');

  Future<void> markAllRead() => _post('$_base/read-all');

  Future<void> _post(String path) async {
    try {
      await _dio.post<Map<String, dynamic>>(path);
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }

  Map<String, dynamic> _data(Response<Map<String, dynamic>> response) =>
      response.data?['data'] as Map<String, dynamic>? ?? const {};
}

final notificationServiceProvider = Provider<NotificationService>(
  (ref) => NotificationService(ref.watch(apiClientProvider)),
);
