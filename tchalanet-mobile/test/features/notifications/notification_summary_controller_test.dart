import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/notifications/data/models/notification_models.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository_impl.dart';
import 'package:tchalanet_mobile/features/notifications/presentation/view_models/notification_summary_controller.dart';

class _FakeNotificationRepository implements NotificationRepository {
  var summaryCalls = 0;

  @override
  Future<void> archive(String id) async {}

  @override
  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  }) async => const NotificationPage(
    items: [],
    page: 0,
    totalPages: 0,
    hasNext: false,
  );

  @override
  Future<NotificationSummary> fetchSummary() async {
    summaryCalls++;
    return NotificationSummary(
      unreadCount: summaryCalls,
      criticalCount: 0,
      actionRequiredCount: 0,
      hasActionRequired: false,
    );
  }

  @override
  Future<void> markRead(String id) async {}
}

class _DelayedNotificationRepository extends _FakeNotificationRepository {
  final completer = Completer<NotificationSummary>();

  @override
  Future<NotificationSummary> fetchSummary() => completer.future;
}

void main() {
  test('summary refresh respects cooldown and supports forced refresh', () async {
    final repository = _FakeNotificationRepository();
    final container = ProviderContainer(
      overrides: [
        notificationRepositoryProvider.overrideWithValue(repository),
      ],
    );
    addTearDown(container.dispose);
    final controller = container.read(notificationSummaryProvider.notifier);

    await controller.refresh();
    await controller.refresh();
    expect(repository.summaryCalls, 1);

    await controller.refresh(force: true);
    expect(repository.summaryCalls, 2);
    expect(
      container.read(notificationSummaryProvider).summary.unreadCount,
      2,
    );
  });

  test('summary reset clears previous-session notification state', () async {
    final repository = _FakeNotificationRepository();
    final container = ProviderContainer(
      overrides: [
        notificationRepositoryProvider.overrideWithValue(repository),
      ],
    );
    addTearDown(container.dispose);
    final controller = container.read(notificationSummaryProvider.notifier);

    await controller.refresh();
    controller.reset();

    final state = container.read(notificationSummaryProvider);
    expect(state.summary, same(NotificationSummary.empty));
    expect(state.lastUpdatedAt, isNull);
  });

  test('late polling response cannot restore previous-session summary', () async {
    final repository = _DelayedNotificationRepository();
    final container = ProviderContainer(
      overrides: [
        notificationRepositoryProvider.overrideWithValue(repository),
      ],
    );
    addTearDown(container.dispose);
    final controller = container.read(notificationSummaryProvider.notifier);

    final refresh = controller.refresh();
    controller.reset();
    repository.completer.complete(
      const NotificationSummary(
        unreadCount: 9,
        criticalCount: 1,
        actionRequiredCount: 0,
        hasActionRequired: false,
      ),
    );
    await refresh;

    expect(
      container.read(notificationSummaryProvider).summary,
      same(NotificationSummary.empty),
    );
  });
}
