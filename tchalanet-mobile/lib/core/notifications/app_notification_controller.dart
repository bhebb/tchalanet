import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/api_exception.dart';
import '../network/api_notice.dart';
import 'api_notification_mapper.dart';
import 'app_notification.dart';
import 'support_reference.dart';

class AppNotificationController extends Notifier<List<AppNotification>> {
  static const maxRetainedNotifications = 3;

  int _nextId = 0;

  @override
  List<AppNotification> build() => const [];

  void show({
    required AppNotificationKind kind,
    required String messageKey,
    String? messageFallback,
    AppNotificationOrigin origin = AppNotificationOrigin.local,
    String? titleKey,
    String? titleFallback,
    String? actionKey,
    void Function()? onAction,
    SupportReference? supportReference,
    Duration duration = const Duration(seconds: 5),
  }) {
    final notification = AppNotification(
      id: _nextId++,
      kind: kind,
      messageKey: messageKey,
      messageFallback: messageFallback,
      origin: origin,
      titleKey: titleKey,
      titleFallback: titleFallback,
      actionKey: actionKey,
      onAction: onAction,
      supportReference: supportReference,
      duration: duration,
    );

    if (state.any((current) => current.hasSameContentAs(notification))) return;

    final next = [...state];
    final pendingSameKindIndex = next.indexWhere(
      (current) => current != next.first && current.kind == kind,
    );
    if (pendingSameKindIndex >= 0) {
      next[pendingSameKindIndex] = notification;
    } else {
      next.add(notification);
    }

    while (next.length > maxRetainedNotifications) {
      next.removeAt(1);
    }
    state = next;
  }

  void showApiNotice(ApiNotice notice) {
    show(
      kind: ApiNotificationMapper.kindForNotice(notice),
      origin: AppNotificationOrigin.apiNotice,
      messageKey: 'api.notice.${notice.code}',
      messageFallback: notice.message,
      supportReference: ApiNotificationMapper.supportReferenceForNotice(notice),
    );
  }

  void showApiError(
    ApiException exception, {
    String messageKey = 'common.error.unknown',
  }) {
    show(
      kind: AppNotificationKind.error,
      origin: AppNotificationOrigin.apiError,
      messageKey: messageKey,
      supportReference: ApiNotificationMapper.supportReferenceForException(
        exception,
      ),
    );
  }

  void dismiss(int id) {
    state = [
      for (final notification in state)
        if (notification.id != id) notification,
    ];
  }
}

final appNotificationProvider =
    NotifierProvider<AppNotificationController, List<AppNotification>>(
      AppNotificationController.new,
    );
