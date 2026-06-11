import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/notification_models.dart';
import '../../data/repositories/notification_repository.dart';
import '../../data/repositories/notification_repository_impl.dart';
import 'notification_summary_controller.dart';

class NotificationCenterState {
  const NotificationCenterState({
    this.items = const [],
    this.status = NotificationStatus.unread,
    this.loading = true,
    this.loadingMore = false,
    this.hasNext = false,
    this.nextPage = 0,
    this.busyIds = const {},
    this.errorKey,
  });

  final List<NotificationItem> items;
  final NotificationStatus status;
  final bool loading;
  final bool loadingMore;
  final bool hasNext;
  final int nextPage;
  final Set<String> busyIds;
  final String? errorKey;

  NotificationCenterState copyWith({
    List<NotificationItem>? items,
    NotificationStatus? status,
    bool? loading,
    bool? loadingMore,
    bool? hasNext,
    int? nextPage,
    Set<String>? busyIds,
    String? errorKey,
    bool clearError = false,
  }) => NotificationCenterState(
    items: items ?? this.items,
    status: status ?? this.status,
    loading: loading ?? this.loading,
    loadingMore: loadingMore ?? this.loadingMore,
    hasNext: hasNext ?? this.hasNext,
    nextPage: nextPage ?? this.nextPage,
    busyIds: busyIds ?? this.busyIds,
    errorKey: clearError ? null : errorKey ?? this.errorKey,
  );
}

class NotificationCenterViewModel extends Notifier<NotificationCenterState> {
  late NotificationRepository _repository;
  int _revision = 0;

  @override
  NotificationCenterState build() {
    _repository = ref.watch(notificationRepositoryProvider);
    ref.onDispose(() => _revision++);
    Future.microtask(load);
    return const NotificationCenterState();
  }

  Future<void> selectStatus(NotificationStatus status) async {
    if (status == state.status) return;
    _revision++;
    state = NotificationCenterState(status: status);
    await load();
  }

  Future<void> refresh() => load();

  Future<void> load() async {
    final revision = ++_revision;
    state = state.copyWith(loading: true, clearError: true);
    try {
      final page = await _repository.fetchNotifications(status: state.status);
      if (revision != _revision) return;
      state = state.copyWith(
        items: List.unmodifiable(page.items),
        loading: false,
        hasNext: page.hasNext,
        nextPage: page.page + 1,
        clearError: true,
      );
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(
        loading: false,
        errorKey: 'notifications.center.load_error',
      );
    }
  }

  Future<void> loadMore() async {
    if (!state.hasNext || state.loading || state.loadingMore) return;
    final revision = _revision;
    state = state.copyWith(loadingMore: true, clearError: true);
    try {
      final page = await _repository.fetchNotifications(
        page: state.nextPage,
        status: state.status,
      );
      if (revision != _revision) return;
      state = state.copyWith(
        items: List.unmodifiable([...state.items, ...page.items]),
        loadingMore: false,
        hasNext: page.hasNext,
        nextPage: page.page + 1,
        clearError: true,
      );
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(
        loadingMore: false,
        errorKey: 'notifications.center.load_more_error',
      );
    }
  }

  Future<void> markRead(String id) async {
    if (state.busyIds.contains(id)) return;
    final revision = _revision;
    _setBusy(id, true);
    try {
      await _repository.markRead(id);
      if (revision != _revision) return;
      final now = DateTime.now();
      state = state.copyWith(
        items: List.unmodifiable([
          for (final item in state.items)
            if (item.id != id)
              item
            else if (state.status != NotificationStatus.unread)
              item.copyWith(status: NotificationStatus.read, readAt: now),
        ]),
        clearError: true,
      );
      await ref.read(notificationSummaryProvider.notifier).refresh(force: true);
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(errorKey: 'notifications.center.action_error');
    } finally {
      if (revision == _revision) _setBusy(id, false);
    }
  }

  Future<void> archive(String id) async {
    if (state.busyIds.contains(id)) return;
    final revision = _revision;
    _setBusy(id, true);
    try {
      await _repository.archive(id);
      if (revision != _revision) return;
      state = state.copyWith(
        items: List.unmodifiable([
          for (final item in state.items)
            if (item.id != id) item,
        ]),
        clearError: true,
      );
      await ref.read(notificationSummaryProvider.notifier).refresh(force: true);
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(errorKey: 'notifications.center.action_error');
    } finally {
      if (revision == _revision) _setBusy(id, false);
    }
  }

  void _setBusy(String id, bool busy) {
    final ids = {...state.busyIds};
    if (busy) {
      ids.add(id);
    } else {
      ids.remove(id);
    }
    state = state.copyWith(busyIds: Set.unmodifiable(ids));
  }
}

final notificationCenterProvider =
    NotifierProvider.autoDispose<
      NotificationCenterViewModel,
      NotificationCenterState
    >(NotificationCenterViewModel.new);
