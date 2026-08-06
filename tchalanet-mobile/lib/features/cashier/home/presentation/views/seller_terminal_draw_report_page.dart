import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../tickets/data/models/cashier_ticket_models.dart';
import '../view_models/cashier_home_providers.dart';

/// Per-draw drill-down of the seller report. Reached by tapping a draw, either
/// from the home draw sheet or from a row of the daily report breakdown.
class SellerTerminalDrawReportPage extends ConsumerWidget {
  const SellerTerminalDrawReportPage({
    super.key,
    required this.drawId,
    required this.isoDate,
    this.drawLabel,
    this.providerSchedule,
    this.providerZone,
    this.localSchedule,
    this.localZone,
  });

  final String drawId;
  final String isoDate;
  final String? drawLabel;

  /// Draw schedule, passed through from the caller so the report can show the
  /// draw's own identity without a second round-trip.
  final String? providerSchedule;
  final String? providerZone;
  final String? localSchedule;
  final String? localZone;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final statsAsync = ref.watch(terminalStatsByDateProvider(isoDate));
    final ticketsAsync = ref.watch(
      drawReportTicketsProvider((isoDate: isoDate, drawId: drawId)),
    );

    final label = drawLabel?.trim();
    final title = (label != null && label.isNotEmpty)
        ? label
        : translations.translate('pos.reports.draw');

    Future<void> refresh() async {
      ref.invalidate(terminalStatsByDateProvider(isoDate));
      ref.invalidate(
        drawReportTicketsProvider((isoDate: isoDate, drawId: drawId)),
      );
      await ref.read(terminalStatsByDateProvider(isoDate).future);
    }

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(title, overflow: TextOverflow.ellipsis),
            Text(
              isoDate,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: translations.translate('common.refresh'),
            onPressed: refresh,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: statsAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => Center(
            child: FeedbackState(
              kind: FeedbackStateKind.error,
              title: translations.translate('pos.reports.load_error'),
              actionLabel: translations.translate('common.retry'),
              onAction: refresh,
              compact: true,
            ),
          ),
          data: (stats) {
            // Reads primitives only: the view must not depend on data-layer types.
            final line = stats.breakdown
                .where((entry) => entry.drawId == drawId)
                .firstOrNull;
            return RefreshIndicator(
              onRefresh: refresh,
              child: _DrawReportBody(
                available: stats.available,
                currency: stats.currency,
                totalCents: line?.totalCents ?? 0,
                ticketCount: line?.ticketCount ?? 0,
                winningsCents: line?.winningsCents ?? 0,
                commissionCents: line?.sellerCommissionCents ?? 0,
                netRevenueCents: line?.netRevenueCents ?? 0,
                providerSchedule: providerSchedule,
                providerZone: providerZone,
                localSchedule: localSchedule,
                localZone: localZone,
                ticketsAsync: ticketsAsync,
                translations: translations,
              ),
            );
          },
        ),
      ),
    );
  }
}

class _DrawReportBody extends StatelessWidget {
  const _DrawReportBody({
    required this.available,
    required this.currency,
    required this.totalCents,
    required this.ticketCount,
    required this.winningsCents,
    required this.commissionCents,
    required this.netRevenueCents,
    required this.providerSchedule,
    required this.providerZone,
    required this.localSchedule,
    required this.localZone,
    required this.ticketsAsync,
    required this.translations,
  });

  final bool available;
  final String currency;
  final int totalCents;
  final int ticketCount;
  final int winningsCents;
  final int commissionCents;
  final int netRevenueCents;
  final String? providerSchedule;
  final String? providerZone;
  final String? localSchedule;
  final String? localZone;
  final AsyncValue<List<CashierTicketSummaryView>> ticketsAsync;
  final I18nBundle translations;

  bool get _hasSchedule =>
      (providerSchedule != null && providerSchedule!.isNotEmpty) ||
      (localSchedule != null && localSchedule!.isNotEmpty);

  static String _money(int cents) => (cents / 100.0).toStringAsFixed(2);

  @override
  Widget build(BuildContext context) {
    if (!available) {
      // RefreshIndicator needs a scrollable descendant to detect the pull
      // gesture even when there is nothing to scroll.
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          Padding(
            padding: const EdgeInsets.all(TchSpacing.s24),
            child: Text(
              translations.translate('pos.reports.analytics_unavailable'),
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      );
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(TchSpacing.s16),
      children: [
        if (_hasSchedule) ...[
          _DrawScheduleBlock(
            providerSchedule: providerSchedule,
            providerZone: providerZone,
            localSchedule: localSchedule,
            localZone: localZone,
            translations: translations,
          ),
          const SizedBox(height: TchSpacing.s16),
        ],
        Row(
          children: [
            Expanded(
              child: StatCardLarge(
                label: translations.translate('pos.reports.draw_sales'),
                value: _money(totalCents),
                unit: currency,
              ),
            ),
            const SizedBox(width: TchSpacing.s12),
            SizedBox(
              width: 100,
              child: StatCard(
                label: translations.translate('common.cashier_stats.tickets'),
                value: ticketCount.toString(),
              ),
            ),
          ],
        ),
        const SizedBox(height: TchSpacing.s12),
        Row(
          children: [
            Expanded(
              child: StatCard(
                label: translations.translate('pos.reports.draw_commission'),
                value: _money(commissionCents),
                unit: currency,
              ),
            ),
            const SizedBox(width: TchSpacing.s12),
            Expanded(
              child: StatCard(
                label: translations.translate('pos.reports.draw_winnings'),
                value: _money(winningsCents),
                unit: currency,
              ),
            ),
          ],
        ),
        const SizedBox(height: TchSpacing.s12),
        // StatCard.accentColor is unusable: it builds a non-uniform border with
        // a borderRadius, which trips a Flutter paint assertion.
        StatCard(
          label: translations.translate('pos.reports.draw_net_revenue'),
          value: _money(netRevenueCents),
          unit: currency,
        ),
        const SizedBox(height: TchSpacing.s8),
        Text(
          translations.translate('pos.reports.draw_net_revenue_hint'),
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: TchSpacing.s24),
        Text(
          translations.translate('pos.reports.tickets'),
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            letterSpacing: 0.5,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: TchSpacing.s12),
        ticketsAsync.when(
          loading: () => const Padding(
            padding: EdgeInsets.symmetric(vertical: TchSpacing.s16),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (_, _) => Text(
            translations.translate('pos.reports.load_error'),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          data: (tickets) {
            if (tickets.isEmpty) {
              return Text(
                translations.translate('pos.reports.draw_empty'),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              );
            }
            return Column(
              children: [
                for (final ticket in tickets)
                  _DrawReportTicketRow(ticket: ticket),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _DrawScheduleBlock extends StatelessWidget {
  const _DrawScheduleBlock({
    required this.providerSchedule,
    required this.providerZone,
    required this.localSchedule,
    required this.localZone,
    required this.translations,
  });

  final String? providerSchedule;
  final String? providerZone;
  final String? localSchedule;
  final String? localZone;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.onSurfaceVariant;
    final style = Theme.of(
      context,
    ).textTheme.bodySmall?.copyWith(color: color, fontWeight: FontWeight.w600);

    Widget line(IconData icon, String label, String value, String? zone) => Row(
      children: [
        Icon(icon, size: 15, color: color),
        const SizedBox(width: TchSpacing.s4),
        Expanded(
          child: Text(
            zone == null || zone.isEmpty
                ? '$label $value'
                : '$label $value · $zone',
            overflow: TextOverflow.ellipsis,
            style: style,
          ),
        ),
      ],
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (providerSchedule != null && providerSchedule!.isNotEmpty)
          line(
            Icons.public_rounded,
            translations.translate('pos.dashboard.provider_time'),
            providerSchedule!,
            providerZone,
          ),
        if (localSchedule != null && localSchedule!.isNotEmpty) ...[
          const SizedBox(height: TchSpacing.s4),
          line(
            Icons.location_on_outlined,
            translations.translate('pos.dashboard.local_time'),
            localSchedule!,
            localZone,
          ),
        ],
      ],
    );
  }
}

class _DrawReportTicketRow extends StatelessWidget {
  const _DrawReportTicketRow({required this.ticket});

  final CashierTicketSummaryView ticket;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final placedAt = ticket.placedAt?.toLocal();
    final time = placedAt == null
        ? ''
        : '${placedAt.hour.toString().padLeft(2, '0')}:'
              '${placedAt.minute.toString().padLeft(2, '0')}';

    return Container(
      margin: const EdgeInsets.only(bottom: TchSpacing.s8),
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s12,
      ),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  ticket.displayCode,
                  style: textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: scheme.primary,
                  ),
                ),
                if (time.isNotEmpty) ...[
                  const SizedBox(height: TchSpacing.s4),
                  Text(
                    time,
                    style: textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: TchSpacing.s12),
          Text(
            ticket.formattedAmount,
            style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}
