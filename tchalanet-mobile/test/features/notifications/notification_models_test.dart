import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/notifications/data/models/notification_models.dart';

void main() {
  test('notification item maps the platform.notification contract', () {
    final item = NotificationItem.fromJson({
      'id': 'notification-1',
      'severity': 'CRITICAL',
      'kind': 'ACTION_REQUIRED',
      'category': 'TENANT_CONFIG',
      'titleKey': 'notification.closure.title',
      'messageText': 'Closing tomorrow',
      'payload': {'outletId': 'outlet-1'},
      'action': {'type': 'ROUTE', 'url': '/pos/session'},
      'status': 'UNREAD',
      'createdAt': '2026-06-11T12:00:00Z',
    });

    expect(item.severity, NotificationSeverity.critical);
    expect(item.kind, NotificationKind.actionRequired);
    expect(item.category, NotificationCategory.tenantConfig);
    expect(item.status, NotificationStatus.unread);
    expect(item.action?.url, '/pos/session');
  });

  test('notification page maps the stable TchPage response', () {
    final page = NotificationPage.fromJson({
      'items': <Map<String, dynamic>>[],
      'page': 0,
      'totalPages': 2,
      'hasNext': true,
    });

    expect(page.items, isEmpty);
    expect(page.totalPages, 2);
    expect(page.hasNext, isTrue);
  });
}
