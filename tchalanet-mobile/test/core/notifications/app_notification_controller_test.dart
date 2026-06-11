import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_exception.dart';
import 'package:tchalanet_mobile/core/network/api_notice.dart';
import 'package:tchalanet_mobile/core/notifications/app_notification.dart';
import 'package:tchalanet_mobile/core/notifications/app_notification_controller.dart';

void main() {
  test('notifications are queued and dismissed exactly once', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(appNotificationProvider.notifier);

    controller.show(
      kind: AppNotificationKind.error,
      messageKey: 'common.error.network',
    );
    controller.show(
      kind: AppNotificationKind.success,
      messageKey: 'ticket.print.success',
    );

    final queued = container.read(appNotificationProvider);
    expect(queued.map((notification) => notification.messageKey), [
      'common.error.network',
      'ticket.print.success',
    ]);

    controller.dismiss(queued.first.id);
    controller.dismiss(queued.first.id);

    expect(
      container.read(appNotificationProvider).single.messageKey,
      'ticket.print.success',
    );
  });

  test('duplicate notifications are ignored', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(appNotificationProvider.notifier);

    controller.show(
      kind: AppNotificationKind.error,
      messageKey: 'common.error.network',
    );
    controller.show(
      kind: AppNotificationKind.error,
      messageKey: 'common.error.network',
    );

    expect(container.read(appNotificationProvider), hasLength(1));
  });

  test(
    'an error burst keeps the visible error and only the latest pending one',
    () {
      final container = ProviderContainer();
      addTearDown(container.dispose);
      final controller = container.read(appNotificationProvider.notifier);

      for (var index = 1; index <= 5; index++) {
        controller.show(
          kind: AppNotificationKind.error,
          messageKey: 'error.$index',
        );
      }

      expect(
        container.read(appNotificationProvider).map((item) => item.messageKey),
        ['error.1', 'error.5'],
      );
    },
  );

  test('the retained notification queue never exceeds its strict limit', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(appNotificationProvider.notifier);

    controller
      ..show(kind: AppNotificationKind.info, messageKey: 'info.1')
      ..show(kind: AppNotificationKind.success, messageKey: 'success.1')
      ..show(kind: AppNotificationKind.warning, messageKey: 'warning.1')
      ..show(kind: AppNotificationKind.error, messageKey: 'error.1');

    final notifications = container.read(appNotificationProvider);
    expect(
      notifications,
      hasLength(AppNotificationController.maxRetainedNotifications),
    );
    expect(notifications.first.messageKey, 'info.1');
    expect(notifications.last.messageKey, 'error.1');
  });

  test('API notice is immediate and uses its server message as fallback', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    container
        .read(appNotificationProvider.notifier)
        .showApiNotice(
          const ApiNotice(
            code: 'LIMIT_WARN',
            message: 'Stake exceeds recommended limit',
            severity: ApiNoticeSeverity.warning,
            traceId: 'notice-trace',
          ),
        );

    final notification = container.read(appNotificationProvider).single;
    expect(notification.origin, AppNotificationOrigin.apiNotice);
    expect(notification.kind, AppNotificationKind.warning);
    expect(notification.messageKey, 'api.notice.LIMIT_WARN');
    expect(notification.messageFallback, 'Stake exceeds recommended limit');
    expect(notification.supportReference?.traceId, 'notice-trace');
  });

  test('API error carries a hidden support reference when traceId exists', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    container
        .read(appNotificationProvider.notifier)
        .showApiError(
          ApiException(
            message: 'raw technical message',
            statusCode: 503,
            traceId: 'trace-123',
            errorId: 'error-456',
            code: 'SERVICE_UNAVAILABLE',
          ),
        );

    final notification = container.read(appNotificationProvider).single;
    expect(notification.origin, AppNotificationOrigin.apiError);
    expect(notification.messageKey, 'common.error.unknown');
    expect(notification.messageFallback, isNull);
    expect(notification.supportReference?.traceId, 'trace-123');
    expect(
      notification.supportReference?.toClipboardText(),
      contains('traceId: trace-123'),
    );
  });
}
