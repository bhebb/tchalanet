import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../tickets/data/models/cashier_sell_catalog_models.dart';
import '../../../tickets/data/services/cashier_sell_catalog_service.dart';
import '../../data/models/cashier_home_models.dart';
import '../../data/services/cashier_home_service.dart';
import '../../data/services/terminal_stats_service.dart';

/// Full cashier home payload. Refreshable — call ref.invalidate(cashierHomeProvider)
/// after operational context selection or session open/close.
final cashierHomeProvider = FutureProvider<CashierHomeResponse>((ref) async {
  return ref.watch(cashierHomeServiceProvider).fetchHome();
});

/// Readiness badges and blockers — polled separately so the home screen
/// can show attention indicators without reloading the full home payload.
final cashierReadinessProvider =
    FutureProvider<CashierReadinessResponse>((ref) async {
  return ref.watch(cashierHomeServiceProvider).fetchReadiness();
});

/// Today's ticket count + sales total for the authenticated SELLER_TERMINAL.
final terminalDailyStatsProvider = FutureProvider<TerminalDailyStats>((ref) async {
  return ref.watch(terminalStatsServiceProvider).fetchDailyStats();
});

/// Stats for a specific ISO date (YYYY-MM-DD). Null = today.
final terminalStatsByDateProvider =
    FutureProvider.family<TerminalDailyStats, String?>((ref, date) async {
  return ref.watch(terminalStatsServiceProvider).fetchDailyStats(date: date);
});

/// All open draws — used by SellerTerminal home to show the full draw list.
final availableDrawsProvider = FutureProvider<List<CashierAvailableDrawView>>((ref) async {
  final draws = await ref.watch(cashierSellCatalogServiceProvider).fetchAvailableDraws();
  return draws.where((d) => d.isOpen).toList();
});
