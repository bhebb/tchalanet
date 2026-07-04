import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/notification_models.dart';
import '../services/notification_service.dart';
import 'notification_repository.dart';

class NotificationRepositoryImpl implements NotificationRepository {
  const NotificationRepositoryImpl(this._service);

  final NotificationService _service;

  @override
  Future<void> archive(String id) => _service.archive(id);

  @override
  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  }) => _service.fetchNotifications(page: page, size: size, status: status);

  @override
  Future<void> markRead(String id) => _service.markRead(id);

  @override
  Future<void> markAllRead() => _service.markAllRead();
}

final notificationRepositoryProvider = Provider<NotificationRepository>(
  (ref) => NotificationRepositoryImpl(ref.watch(notificationServiceProvider)),
);
