import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/notification_models.dart';

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
  @override
  NotificationSummaryState build() =>
      const NotificationSummaryState(summary: NotificationSummary.empty);

  void applyRuntimeSummary({
    required int unreadCount,
    required int criticalCount,
  }) {
    state = NotificationSummaryState(
      summary: NotificationSummary(
        unreadCount: unreadCount,
        criticalCount: criticalCount,
        actionRequiredCount: 0,
        hasActionRequired: false,
      ),
      lastUpdatedAt: DateTime.now(),
    );
  }

  void reset() {
    state = const NotificationSummaryState(summary: NotificationSummary.empty);
  }
}

final notificationSummaryProvider =
    NotifierProvider<NotificationSummaryController, NotificationSummaryState>(
      NotificationSummaryController.new,
    );
