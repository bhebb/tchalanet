import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/notifications/data/models/notification_models.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository.dart';
import 'package:tchalanet_mobile/features/notifications/data/repositories/notification_repository_impl.dart';
import 'package:tchalanet_mobile/features/notifications/presentation/view_models/notification_center_view_model.dart';

class _FakeNotificationRepository implements NotificationRepository {
  final pages = <NotificationStatus, List<NotificationItem>>{};
  final markedRead = <String>[];
  final archived = <String>[];

  @override
  Future<void> archive(String id) async => archived.add(id);

  @override
  Future<NotificationPage> fetchNotifications({
    int page = 0,
    int size = 20,
    NotificationStatus? status,
  }) async => NotificationPage(
    items: pages[status] ?? const [],
    page: page,
    totalPages: 1,
    hasNext: false,
  );

  @override
  Future<void> markRead(String id) async => markedRead.add(id);
}

NotificationItem _item(String id, NotificationStatus status) =>
    NotificationItem(
      id: id,
      severity: NotificationSeverity.info,
      kind: NotificationKind.info,
      category: NotificationCategory.system,
      status: status,
      titleText: 'Title',
      messageText: 'Message',
      createdAt: DateTime.utc(2026, 6, 11),
    );

void main() {
  test('loads unread notifications when the center opens', () async {
    final repository = _FakeNotificationRepository()
      ..pages[NotificationStatus.unread] = [
        _item('unread-1', NotificationStatus.unread),
      ];
    final container = ProviderContainer(
      overrides: [notificationRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final subscription = container.listen(
      notificationCenterProvider,
      (_, _) {},
      fireImmediately: true,
    );
    addTearDown(subscription.close);

    await Future<void>.delayed(Duration.zero);
    await Future<void>.delayed(Duration.zero);

    final state = container.read(notificationCenterProvider);
    expect(state.loading, isFalse);
    expect(state.status, NotificationStatus.unread);
    expect(state.items.single.id, 'unread-1');
  });

  test('read and archive actions update the current filtered list', () async {
    final repository = _FakeNotificationRepository()
      ..pages[NotificationStatus.unread] = [
        _item('read-me', NotificationStatus.unread),
        _item('archive-me', NotificationStatus.unread),
      ];
    final container = ProviderContainer(
      overrides: [notificationRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final subscription = container.listen(
      notificationCenterProvider,
      (_, _) {},
      fireImmediately: true,
    );
    addTearDown(subscription.close);
    await Future<void>.delayed(Duration.zero);
    await Future<void>.delayed(Duration.zero);
    final viewModel = container.read(notificationCenterProvider.notifier);

    await viewModel.markRead('read-me');
    await viewModel.archive('archive-me');

    expect(repository.markedRead, ['read-me']);
    expect(repository.archived, ['archive-me']);
    expect(container.read(notificationCenterProvider).items, isEmpty);
  });

  test('switching status reloads the selected server filter', () async {
    final repository = _FakeNotificationRepository()
      ..pages[NotificationStatus.read] = [
        _item('read-1', NotificationStatus.read),
      ];
    final container = ProviderContainer(
      overrides: [notificationRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final subscription = container.listen(
      notificationCenterProvider,
      (_, _) {},
      fireImmediately: true,
    );
    addTearDown(subscription.close);
    await Future<void>.delayed(Duration.zero);
    await Future<void>.delayed(Duration.zero);

    await container
        .read(notificationCenterProvider.notifier)
        .selectStatus(NotificationStatus.read);

    final state = container.read(notificationCenterProvider);
    expect(state.status, NotificationStatus.read);
    expect(state.items.single.id, 'read-1');
  });
}
