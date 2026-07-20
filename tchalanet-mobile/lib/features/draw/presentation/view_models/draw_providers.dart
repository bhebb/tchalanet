import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/draw_models.dart';
import '../../data/services/draw_result_service.dart';

/// Public draw slots — no auth required.
/// Returns empty list silently on error (login screen must not block on this).
final homeDrawSlotsProvider = FutureProvider<List<DrawSlotView>>((ref) async {
  try {
    return await ref.watch(drawResultServiceProvider).fetchSlots();
  } catch (_) {
    return [];
  }
});

/// First upcoming slot (smallest positive countdown).
final nextDrawProvider = Provider<DrawSlotView?>((ref) {
  final slots = ref
      .watch(homeDrawSlotsProvider)
      .when(
        data: (d) => d,
        loading: () => <DrawSlotView>[],
        error: (_, _) => <DrawSlotView>[],
      );
  final upcoming =
      slots.where((s) => s.next != null && s.next!.isUpcoming).toList()..sort(
        (a, b) => a.next!.countdownSeconds.compareTo(b.next!.countdownSeconds),
      );
  return upcoming.isEmpty ? null : upcoming.first;
});

class PublicDrawResultHistoryQuery {
  const PublicDrawResultHistoryQuery({
    required this.from,
    required this.to,
    this.provider,
    this.slotKey,
  });

  final DateTime from;
  final DateTime to;
  final String? provider;
  final String? slotKey;

  @override
  bool operator ==(Object other) =>
      other is PublicDrawResultHistoryQuery &&
      other.from == from &&
      other.to == to &&
      other.provider == provider &&
      other.slotKey == slotKey;

  @override
  int get hashCode => Object.hash(from, to, provider, slotKey);
}

final publicDrawResultSlotsProvider =
    FutureProvider.autoDispose<List<PublicDrawResultSlot>>(
      (ref) => ref.watch(drawResultServiceProvider).fetchResultSlots(),
    );

final publicDrawResultHistoryProvider = FutureProvider.autoDispose
    .family<PublicDrawResultHistory, PublicDrawResultHistoryQuery>(
      (ref, query) => ref
          .watch(drawResultServiceProvider)
          .fetchHistory(
            from: query.from,
            to: query.to,
            provider: query.provider,
            slotKey: query.slotKey,
          ),
    );
