import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../tickets/data/models/cashier_sell_catalog_models.dart';
import '../../../tickets/data/models/cashier_ticket_models.dart';
import '../../../tickets/data/services/cashier_sell_catalog_service.dart';
import '../../../tickets/data/services/cashier_ticket_service.dart';
import '../../data/models/cashier_home_models.dart';
import '../../data/models/pos_draw_detail_models.dart';
import '../../data/models/pos_profile_models.dart';
import '../../data/services/cashier_home_service.dart';
import '../../data/repositories/pos_draw_detail_repository.dart';
import '../../data/services/pos_profile_service.dart';
import '../../data/services/terminal_stats_service.dart';

/// Full cashier home payload. Refreshable — call ref.invalidate(cashierHomeProvider)
/// after operational context selection or session open/close.
final cashierHomeProvider = FutureProvider<CashierHomeResponse>((ref) async {
  return ref.watch(cashierHomeServiceProvider).fetchHome();
});

/// Readiness badges and blockers — polled separately so the home screen
/// can show attention indicators without reloading the full home payload.
final cashierReadinessProvider = FutureProvider<CashierReadinessResponse>((
  ref,
) async {
  return ref.watch(cashierHomeServiceProvider).fetchReadiness();
});

/// One compact account payload for POS profile and settings.
final posProfileProvider = FutureProvider<PosProfileResponse>((ref) async {
  return ref.watch(posProfileServiceProvider).fetchProfile();
});

final posProfileSettingsUpdaterProvider = Provider<PosProfileSettingsUpdater>(
  PosProfileSettingsUpdater.new,
);

class PosProfileSettingsUpdater {
  const PosProfileSettingsUpdater(this._ref);

  final Ref _ref;

  Future<void> update({
    bool? receiptAutoPrint,
    int? receiptCopyCount,
    bool? receiptQuickSale,
    String? receiptPrinterMode,
    String? receiptPaperSize,
    String? receiptAdapterPreference,
    bool? notificationsEnabled,
    bool? notificationsCriticalOnly,
  }) async {
    await _ref
        .read(posProfileServiceProvider)
        .updateSettings(
          receiptAutoPrint: receiptAutoPrint,
          receiptCopyCount: receiptCopyCount,
          receiptQuickSale: receiptQuickSale,
          receiptPrinterMode: receiptPrinterMode,
          receiptPaperSize: receiptPaperSize,
          receiptAdapterPreference: receiptAdapterPreference,
          notificationsEnabled: notificationsEnabled,
          notificationsCriticalOnly: notificationsCriticalOnly,
        );
    _ref.invalidate(posProfileProvider);
  }
}

/// Today's ticket count + sales total for the authenticated SELLER_TERMINAL.
final terminalDailyStatsProvider = FutureProvider<TerminalDailyStats>((
  ref,
) async {
  return ref.watch(terminalStatsServiceProvider).fetchDailyStats();
});

/// Stats for a specific ISO date (YYYY-MM-DD). Null = today.
final terminalStatsByDateProvider =
    FutureProvider.family<TerminalDailyStats, String?>((ref, date) async {
      return ref
          .watch(terminalStatsServiceProvider)
          .fetchDailyStats(date: date);
    });

/// `YYYY-MM-DD` in local time — the shape `/tenant/cashier/tickets` expects.
String reportIsoDate(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-'
    '${value.month.toString().padLeft(2, '0')}-'
    '${value.day.toString().padLeft(2, '0')}';

/// Tickets sold by this seller terminal for one draw on one day — backs the
/// per-draw report drill-down.
final drawReportTicketsProvider = FutureProvider.autoDispose
    .family<List<CashierTicketSummaryView>, ({String isoDate, String drawId})>((
      ref,
      key,
    ) async {
      final date = DateTime.parse(key.isoDate);
      return ref
          .watch(cashierTicketServiceProvider)
          .listRecent(
            size: 100,
            fromDate: date,
            toDate: date,
            drawId: key.drawId,
          );
    });

/// All open draws — used by SellerTerminal home to show the full draw list.
final availableDrawsProvider = FutureProvider<List<CashierAvailableDrawView>>((
  ref,
) async {
  final draws = await ref
      .watch(cashierSellCatalogServiceProvider)
      .fetchAvailableDraws();
  return draws.where((d) => d.isOpen).toList();
});

/// Draw detail for a specific draw: top selections + exposure alerts (SELLER_TERMINAL scope).
/// Returns null if the draw is not open so the caller can skip the block silently.
final posDrawDetailProvider = FutureProvider.autoDispose
    .family<PosDrawDetailResponse?, String>((ref, drawId) async {
      final isOpen = ref
          .watch(availableDrawsProvider)
          .maybeWhen(
            data: (draws) => draws.any((draw) => draw.drawId == drawId),
            orElse: () => false,
          );
      if (!isOpen) return null;
      return ref.watch(posDrawDetailRepositoryProvider).fetchDrawDetail(drawId);
    });

/// The most recently sold ticket, kept separate from the draw catalogue so a
/// temporary history failure never prevents a cashier from starting a sale.
final latestTicketProvider = FutureProvider<CashierTicketSummaryView?>((
  ref,
) async {
  final tickets = await ref
      .watch(cashierTicketServiceProvider)
      .listRecent(size: 1);
  return tickets.isEmpty ? null : tickets.first;
});
