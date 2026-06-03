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
  final slots = ref.watch(homeDrawSlotsProvider).when(
    data: (d) => d,
    loading: () => <DrawSlotView>[],
    error: (_, _) => <DrawSlotView>[],
  );
  final upcoming = slots
      .where((s) => s.next != null && s.next!.isUpcoming)
      .toList()
    ..sort((a, b) =>
        a.next!.countdownSeconds.compareTo(b.next!.countdownSeconds));
  return upcoming.isEmpty ? null : upcoming.first;
});
