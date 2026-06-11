import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/notifications/data/models/notification_models.dart';
import 'package:tchalanet_mobile/features/notifications/presentation/view_models/notification_summary_controller.dart';

void main() {
  test('runtime summary replaces the app-scoped notification summary', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(notificationSummaryProvider.notifier);

    controller.applyRuntimeSummary(unreadCount: 4, criticalCount: 1);

    final state = container.read(notificationSummaryProvider);
    expect(state.summary.unreadCount, 4);
    expect(state.summary.criticalCount, 1);
    expect(state.lastUpdatedAt, isNotNull);
  });

  test('summary reset clears previous-session notification state', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(notificationSummaryProvider.notifier);

    controller.applyRuntimeSummary(unreadCount: 9, criticalCount: 1);
    controller.reset();

    final state = container.read(notificationSummaryProvider);
    expect(state.summary, same(NotificationSummary.empty));
    expect(state.lastUpdatedAt, isNull);
  });
}
