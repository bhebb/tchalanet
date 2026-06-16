import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../design_system/components/stat_card.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/services/terminal_stats_service.dart';
import '../view_models/cashier_home_providers.dart';
import 'seller_terminal_nav_bar.dart';

String _isoDate(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-'
    '${d.month.toString().padLeft(2, '0')}-'
    '${d.day.toString().padLeft(2, '0')}';

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

  @override
  Widget build(BuildContext context) {
    final isoDate = _isoDate(_selectedDate);
    final statsAsync = ref.watch(terminalStatsByDateProvider(isoDate));

    return Scaffold(
      appBar: AppBar(title: const Text('Statistiques')),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 2),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _DateToggleBar(
              selected: _selectedDate,
              onChanged: _onDateChanged,
            ),
            Expanded(
              child: statsAsync.when(
                loading: () =>
                    const Center(child: CircularProgressIndicator()),
                error: (e, _) => _StatsError(
                  onRetry: () =>
                      ref.invalidate(terminalStatsByDateProvider(isoDate)),
                ),
                data: (stats) => _StatsBody(
                  stats: stats,
                  drawFilter: _drawFilter,
                  onDrawFilter: (id) => setState(() => _drawFilter = id),
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
  const _DateToggleBar({required this.selected, required this.onChanged});

  final DateTime selected;
  final ValueChanged<DateTime> onChanged;

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));
    final selDay = DateTime(selected.year, selected.month, selected.day);

    final isToday = selDay == today;

    return Padding(
      padding: const EdgeInsets.fromLTRB(
          TchSpacing.s16, TchSpacing.s12, TchSpacing.s16, 0),
      child: SegmentedButton<String>(
        segments: const [
          ButtonSegment(value: 'today', label: Text("Aujourd'hui")),
          ButtonSegment(value: 'yesterday', label: Text('Hier')),
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
    required this.drawFilter,
    required this.onDrawFilter,
  });

  final TerminalDailyStats stats;
  final String? drawFilter;
  final ValueChanged<String?> onDrawFilter;

  @override
  Widget build(BuildContext context) {
    final filtered = drawFilter == null
        ? stats.breakdown
        : stats.breakdown.where((b) => b.drawId == drawFilter).toList();

    return ListView(
      padding: const EdgeInsets.all(TchSpacing.s16),
      children: [
        const _SectionLabel('TOTAL'),
        const SizedBox(height: TchSpacing.s12),
        Row(
          children: [
            Expanded(
              child: StatCardLarge(
                label: 'Ventes',
                value: (stats.salesTotalCents / 100.0).toStringAsFixed(2),
                unit: stats.currency,
              ),
            ),
            const SizedBox(width: TchSpacing.s12),
            SizedBox(
              width: 100,
              child: StatCard(
                label: 'Tickets',
                value: stats.ticketCount.toString(),
              ),
            ),
          ],
        ),
        if (stats.breakdown.isNotEmpty) ...[
          const SizedBox(height: TchSpacing.s24),
          const _SectionLabel('PAR TIRAGE'),
          const SizedBox(height: TchSpacing.s12),
          _DrawFilterChips(
            breakdown: stats.breakdown,
            selected: drawFilter,
            onSelected: onDrawFilter,
          ),
          const SizedBox(height: TchSpacing.s12),
          for (final line in filtered)
            _DrawStatRow(line: line, currency: stats.currency),
        ],
        if (stats.ticketCount == 0)
          Padding(
            padding: const EdgeInsets.only(top: TchSpacing.s32),
            child: Center(
              child: Text(
                'Aucune vente pour cette journée',
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
    required this.selected,
    required this.onSelected,
  });

  final List<DrawStatLine> breakdown;
  final String? selected;
  final ValueChanged<String?> onSelected;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          FilterChip(
            label: const Text('Tous'),
            selected: selected == null,
            onSelected: (_) => onSelected(null),
          ),
          for (final line in breakdown) ...[
            const SizedBox(width: TchSpacing.s8),
            FilterChip(
              label: Text(line.channelLabel),
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
  const _DrawStatRow({required this.line, required this.currency});

  final DrawStatLine line;
  final String currency;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return Container(
      margin: const EdgeInsets.only(bottom: TchSpacing.s8),
      padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s16, vertical: TchSpacing.s12),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              line.channelLabel,
              style:
                  textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          Text(
            '${line.ticketCount} tickets',
            style:
                textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
          const SizedBox(width: TchSpacing.s16),
          Text(
            '${line.totalAmount.toStringAsFixed(2)} $currency',
            style: textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w700,
              color: TchColors.success,
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Error ────────────────────────────────────────────────────────────────────

class _StatsError extends StatelessWidget {
  const _StatsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
          const SizedBox(height: TchSpacing.s16),
          const Text('Impossible de charger les statistiques'),
          const SizedBox(height: TchSpacing.s12),
          TextButton(onPressed: onRetry, child: const Text('Réessayer')),
        ],
      ),
    );
  }
}
