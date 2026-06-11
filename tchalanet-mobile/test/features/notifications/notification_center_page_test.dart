import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/design_system/theme/tch_theme.dart';
import 'package:tchalanet_mobile/features/notifications/data/models/notification_models.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository_impl.dart';
import 'package:tchalanet_mobile/features/notifications/presentation/views/notification_center_page.dart';

class _FakeNotificationRepository implements NotificationRepository {
  var unread = [
    NotificationItem(
      id: 'notification-1',
      severity: NotificationSeverity.warning,
      kind: NotificationKind.actionRequired,
      category: NotificationCategory.system,
      status: NotificationStatus.unread,
      titleText: 'Fèmti demen',
      messageText: 'Pwen vant la ap fèmen demen.',
      createdAt: DateTime.utc(2026, 6, 11, 12),
    ),
  ];

  @override
  Future<void> archive(String id) async {
    unread = unread.where((item) => item.id != id).toList();
  }

  @override
  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  }) async => NotificationPage(
    items: status == NotificationStatus.unread ? unread : const [],
    page: page,
    totalPages: 1,
    hasNext: false,
  );

  @override
  Future<void> markRead(String id) async {
    unread = unread.where((item) => item.id != id).toList();
  }
}

void main() {
  testWidgets('notification center renders and marks an unread item as read', (
    tester,
  ) async {
    final repository = _FakeNotificationRepository();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          notificationRepositoryProvider.overrideWithValue(repository),
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'common.loading': 'Chajman',
                'common.retry': 'Eseye ankò',
                'notifications.center.title': 'Notifikasyon',
                'notifications.center.refresh': 'Rafrechi',
                'notifications.center.empty': 'Pa gen notifikasyon',
                'notifications.center.empty.unread': 'Pa gen nouvo mesaj.',
                'notifications.center.load_error': 'Erè chajman',
                'notifications.center.load_more': 'Chaje plis',
                'notifications.status.unread': 'Nouvo',
                'notifications.status.read': 'Li',
                'notifications.status.archived': 'Achive',
                'notifications.action.mark_read': 'Make kòm li',
                'notifications.action.archive': 'Achive',
                'notifications.action.open': 'Louvri',
                'notifications.severity.warning': 'Atansyon',
              },
            ),
          ),
        ],
        child: MaterialApp(
          theme: TchTheme.light(),
          home: const NotificationCenterPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Fèmti demen'), findsOneWidget);
    expect(find.text('Pwen vant la ap fèmen demen.'), findsOneWidget);
    expect(find.text('Atansyon'), findsOneWidget);

    await tester.tap(find.text('Make kòm li'));
    await tester.pumpAndSettle();

    expect(find.text('Fèmti demen'), findsNothing);
    expect(find.text('Pa gen notifikasyon'), findsOneWidget);
  });
}
