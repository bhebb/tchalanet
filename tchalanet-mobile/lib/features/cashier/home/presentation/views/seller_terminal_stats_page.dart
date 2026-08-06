import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/draw_identity_label.dart';
import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/stat_card.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../tickets/data/models/cashier_sell_catalog_models.dart';
import '../../../tickets/data/models/cashier_ticket_models.dart';
import '../../../tickets/data/services/cashier_ticket_service.dart';
import '../../../tickets/presentation/cashier_draw_label.dart';
import '../../data/services/terminal_stats_service.dart';
import '../view_models/cashier_home_providers.dart';
import 'seller_terminal_nav_bar.dart';

String _isoDate(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-'
    '${d.month.toString().padLeft(2, '0')}-'
    '${d.day.toString().padLeft(2, '0')}';

final _reportTicketsProvider = FutureProvider.autoDispose
    .family<List<CashierTicketSummaryView>, String>((ref, isoDate) async {
      final date = DateTime.parse(isoDate);
      return ref
          .watch(cashierTicketServiceProvider)
          .listRecent(size: 50, fromDate: date, toDate: date);
    });

class SellerTerminalStatsPage extends ConsumerStatefulWidget {
  const SellerTerminalStatsPage({super.key});

  @override
  ConsumerState<SellerTerminalStatsPage> createState() =>
      _SellerTerminalStatsPageState();
}

class _SellerTerminalStatsPageState
    extends ConsumerState<SellerTerminalStatsPage> {
  late DateTime _selectedDate;
  String? _drawFilter;

  @override
  void initState() {
    super.initState();
    _selectedDate = DateTime.now();
  }

  void _onDateChanged(DateTime d) {
    setState(() {
      _selectedDate = d;
      _drawFilter = null;
    });
  }

  Future<void> _refresh(String isoDate) async {
    ref.invalidate(terminalStatsByDateProvider(isoDate));
    ref.invalidate(_reportTicketsProvider(isoDate));
    await ref.read(terminalStatsByDateProvider(isoDate).future);
  }

  @override
  Widget build(BuildContext context) {
    final isoDate = _isoDate(_selectedDate);
    final statsAsync = ref.watch(terminalStatsByDateProvider(isoDate));
    final reportTicketsAsync = ref.watch(_reportTicketsProvider(isoDate));
    final availableDraws = ref
        .watch(availableDrawsProvider)
        .when(
          data: (draws) => draws,
          loading: () => const <CashierAvailableDrawView>[],
          error: (_, _) => const <CashierAvailableDrawView>[],
        );
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(title: Text(translations.translate('pos.reports.title'))),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 3),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _DateToggleBar(
              selected: _selectedDate,
              onChanged: _onDateChanged,
              translations: translations,
            ),
            Expanded(
              child: statsAsync.when(
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (e, _) => _StatsError(
                  onRetry: () =>
                      ref.invalidate(terminalStatsByDateProvider(isoDate)),
                ),
                data: (stats) => RefreshIndicator(
                  onRefresh: () => _refresh(isoDate),
                  child: _StatsBody(
                    stats: stats,
                    reportTicketsAsync: reportTicketsAsync,
                    availableDraws: availableDraws,
                    translations: translations,
                    drawFilter: _drawFilter,
                    onDrawFilter: (id) => setState(() => _drawFilter = id),
                    isoDate: isoDate,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Date toggle ──────────────────────────────────────────────────────────────

class _DateToggleBar extends StatelessWidget {
  const _DateToggleBar({
    required this.selected,
    required this.onChanged,
    required this.translations,
  });

  final DateTime selected;
  final ValueChanged<DateTime> onChanged;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));
    final selDay = DateTime(selected.year, selected.month, selected.day);

    final isToday = selDay == today;

    return Padding(
      padding: const EdgeInsets.fromLTRB(
        TchSpacing.s16,
        TchSpacing.s12,
        TchSpacing.s16,
        0,
      ),
      child: SegmentedButton<String>(
        segments: [
          ButtonSegment(
            value: 'today',
            label: Text(translations.translate('pos.reports.today')),
          ),
          ButtonSegment(
            value: 'yesterday',
            label: Text(translations.translate('pos.reports.yesterday')),
          ),
        ],
        selected: {isToday ? 'today' : 'yesterday'},
        onSelectionChanged: (s) {
          onChanged(s.contains('yesterday') ? yesterday : today);
        },
      ),
    );
  }
}

// ─── Stats body ───────────────────────────────────────────────────────────────

class _StatsBody extends StatelessWidget {
  const _StatsBody({
    required this.stats,
    required this.reportTicketsAsync,
    required this.availableDraws,
    required this.translations,
    required this.drawFilter,
    required this.onDrawFilter,
    required this.isoDate,
  });

  final TerminalDailyStats stats;
  final AsyncValue<List<CashierTicketSummaryView>> reportTicketsAsync;
  final List<CashierAvailableDrawView> availableDraws;
  final I18nBundle translations;
  final String? drawFilter;
  final ValueChanged<String?> onDrawFilter;
  final String isoDate;

  @override
  Widget build(BuildContext context) {
    if (!stats.available) {
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
    final filtered = drawFilter == null
        ? stats.breakdown
        : stats.breakdown.where((b) => b.drawId == drawFilter).toList();

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(TchSpacing.s16),
      children: [
        _SectionLabel(translations.translate('common.cashier_stats.total')),
        const SizedBox(height: TchSpacing.s12),
        Row(
          children: [
            Expanded(
              child: StatCardLarge(
                label: translations.translate(
                  'common.cashier_stats.sales_today',
                ),
                value: (stats.salesTotalCents / 100.0).toStringAsFixed(2),
                unit: stats.currency,
              ),
            ),
            const SizedBox(width: TchSpacing.s12),
            SizedBox(
              width: 100,
              child: StatCard(
                label: translations.translate('common.cashier_stats.tickets'),
                value: stats.ticketCount.toString(),
              ),
            ),
          ],
        ),
        const SizedBox(height: TchSpacing.s12),
        StatCard(
          label: translations.translate(
            'common.cashier_stats.commission_today',
          ),
          value: (stats.sellerCommissionTotalCents / 100.0).toStringAsFixed(2),
          unit: stats.currency,
        ),
        if (stats.breakdown.isNotEmpty) ...[
          const SizedBox(height: TchSpacing.s24),
          _SectionLabel(translations.translate('pos.reports.by_draw')),
          const SizedBox(height: TchSpacing.s12),
          _DrawFilterChips(
            breakdown: stats.breakdown,
            availableDraws: availableDraws,
            translations: translations,
            selected: drawFilter,
            onSelected: onDrawFilter,
            allLabel: translations.translate('pos.reports.all'),
          ),
          const SizedBox(height: TchSpacing.s12),
          for (final line in filtered)
            _DrawStatRow(
              line: line,
              availableDraws: availableDraws,
              translations: translations,
              currency: stats.currency,
              ticketLabel: translations.translate(
                'common.cashier_stats.tickets',
              ),
              isoDate: isoDate,
            ),
        ],
        const SizedBox(height: TchSpacing.s24),
        _SectionLabel(
          translations.translate(
            'pos.reports.tickets',
            fallback: translations.translate('common.cashier_stats.tickets'),
          ),
        ),
        const SizedBox(height: TchSpacing.s12),
        _ReportTicketsSection(
          ticketsAsync: reportTicketsAsync,
          translations: translations,
        ),
        if (stats.ticketCount == 0)
          Padding(
            padding: const EdgeInsets.only(top: TchSpacing.s32),
            child: Center(
              child: Text(
                translations.translate('pos.reports.empty'),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: Theme.of(context).textTheme.labelSmall?.copyWith(
        color: Theme.of(context).colorScheme.onSurfaceVariant,
        letterSpacing: 0.5,
        fontWeight: FontWeight.w700,
      ),
    );
  }
}

class _DrawFilterChips extends StatelessWidget {
  const _DrawFilterChips({
    required this.breakdown,
    required this.availableDraws,
    required this.translations,
    required this.selected,
    required this.onSelected,
    required this.allLabel,
  });

  final List<DrawStatLine> breakdown;
  final List<CashierAvailableDrawView> availableDraws;
  final I18nBundle translations;
  final String? selected;
  final ValueChanged<String?> onSelected;
  final String allLabel;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          FilterChip(
            label: Text(allLabel),
            selected: selected == null,
            onSelected: (_) => onSelected(null),
          ),
          for (final line in breakdown) ...[
            const SizedBox(width: TchSpacing.s8),
            FilterChip(
              label: Text(_reportDrawLabel(line, availableDraws, translations)),
              selected: selected == line.drawId,
              onSelected: (_) =>
                  onSelected(selected == line.drawId ? null : line.drawId),
            ),
          ],
        ],
      ),
    );
  }
}

class _DrawStatRow extends StatelessWidget {
  const _DrawStatRow({
    required this.line,
    required this.availableDraws,
    required this.translations,
    required this.currency,
    required this.ticketLabel,
    required this.isoDate,
  });

  final DrawStatLine line;
  final List<CashierAvailableDrawView> availableDraws;
  final I18nBundle translations;
  final String currency;
  final String ticketLabel;
  final String isoDate;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final label = _reportDrawLabel(line, availableDraws, translations);
    final radius = BorderRadius.circular(TchRadius.md);

    return Container(
      margin: const EdgeInsets.only(bottom: TchSpacing.s8),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: radius,
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: InkWell(
        key: ValueKey('report-draw-row:${line.drawId}'),
        borderRadius: radius,
        onTap: line.drawId.isEmpty
            ? null
            : () => context.push(
                '/pos/reports/draw/${line.drawId}',
                extra: <String, String?>{
                  'isoDate': isoDate,
                  'drawLabel': label,
                },
              ),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: TchSpacing.s16,
            vertical: TchSpacing.s12,
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  label,
                  style: textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Text(
                '${line.ticketCount} $ticketLabel',
                style: textTheme.bodySmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(width: TchSpacing.s16),
              Text(
                '${line.totalAmount.toStringAsFixed(2)} $currency',
                style: textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: TchColors.success,
                ),
              ),
              const SizedBox(width: TchSpacing.s4),
              Icon(
                Icons.chevron_right_rounded,
                size: 18,
                color: scheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReportTicketsSection extends StatelessWidget {
  const _ReportTicketsSection({
    required this.ticketsAsync,
    required this.translations,
  });

  final AsyncValue<List<CashierTicketSummaryView>> ticketsAsync;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) => ticketsAsync.when(
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
          translations.translate('pos.reports.empty'),
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        );
      }
      return Column(
        children: [
          for (final ticket in tickets)
            _ReportTicketRow(ticket: ticket, translations: translations),
        ],
      );
    },
  );
}

class _ReportTicketRow extends StatelessWidget {
  const _ReportTicketRow({required this.ticket, required this.translations});

  final CashierTicketSummaryView ticket;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final drawLabel = localizedDrawIdentityLabel(
      providerCode: ticket.resultProvider,
      slotKey: ticket.resultSlotKey,
      fallback: ticket.drawLabel,
      translations: translations,
    );
    final code = ticket.ticketCode.isNotEmpty
        ? ticket.ticketCode
        : ticket.displayCode;

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
                  code,
                  style: textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: scheme.primary,
                  ),
                ),
                const SizedBox(height: TchSpacing.s4),
                Text(
                  drawLabel,
                  style: textTheme.bodySmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
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

String _reportDrawLabel(
  DrawStatLine line,
  List<CashierAvailableDrawView> availableDraws,
  I18nBundle translations,
) {
  final label = line.channelLabel.trim();
  final matchingDraw = availableDraws
      .where(
        (draw) =>
            draw.drawId == line.drawId ||
            (draw.drawChannelId != null && draw.drawChannelId == label),
      )
      .firstOrNull;
  if (matchingDraw != null) {
    return localizedCashierDrawLabel(matchingDraw, translations);
  }
  if (label.isNotEmpty && !_looksLikeTechnicalId(label)) return label;
  return translations.translate('pos.reports.draw', fallback: 'Tiraj');
}

bool _looksLikeTechnicalId(String value) => RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
).hasMatch(value.trim());

// ─── Error ────────────────────────────────────────────────────────────────────

class _StatsError extends ConsumerWidget {
  const _StatsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final translations = ref.watch(i18nBundleProvider);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
          const SizedBox(height: TchSpacing.s16),
          Text(translations.translate('pos.reports.load_error')),
          const SizedBox(height: TchSpacing.s12),
          TextButton(
            onPressed: onRetry,
            child: Text(translations.translate('common.retry')),
          ),
        ],
      ),
    );
  }
}
