import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/notification_models.dart';
import '../../data/repositories/notification_repository.dart';
import '../../data/repositories/notification_repository_impl.dart';

const notificationPollingInterval = Duration(minutes: 30);

class NotificationSummaryState {
  const NotificationSummaryState({
    required this.summary,
    this.lastUpdatedAt,
    this.refreshing = false,
  });

  final NotificationSummary summary;
  final DateTime? lastUpdatedAt;
  final bool refreshing;

  NotificationSummaryState copyWith({
    NotificationSummary? summary,
    DateTime? lastUpdatedAt,
    bool? refreshing,
  }) => NotificationSummaryState(
    summary: summary ?? this.summary,
    lastUpdatedAt: lastUpdatedAt ?? this.lastUpdatedAt,
    refreshing: refreshing ?? this.refreshing,
  );
}

class NotificationSummaryController extends Notifier<NotificationSummaryState> {
  late NotificationRepository _repository;
  int _revision = 0;

  @override
  NotificationSummaryState build() {
    _repository = ref.watch(notificationRepositoryProvider);
    return const NotificationSummaryState(summary: NotificationSummary.empty);
  }

  Future<void> refresh({bool force = false}) async {
    if (state.refreshing) return;
    final lastUpdatedAt = state.lastUpdatedAt;
    if (!force &&
        lastUpdatedAt != null &&
        DateTime.now().difference(lastUpdatedAt) <
            notificationPollingInterval) {
      return;
    }

    final revision = _revision;
    state = state.copyWith(refreshing: true);
    try {
      final summary = await _repository.fetchSummary();
      if (revision != _revision) return;
      state = NotificationSummaryState(
        summary: summary,
        lastUpdatedAt: DateTime.now(),
      );
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(refreshing: false);
    }
  }

  void reset() {
    _revision++;
    state = const NotificationSummaryState(summary: NotificationSummary.empty);
  }
}

final notificationSummaryProvider =
    NotifierProvider<NotificationSummaryController, NotificationSummaryState>(
      NotificationSummaryController.new,
    );
