import '../models/notification_models.dart';

abstract interface class NotificationRepository {
  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  });

  Future<void> markRead(String id);

  Future<void> archive(String id);

  Future<void> markAllRead();
}
