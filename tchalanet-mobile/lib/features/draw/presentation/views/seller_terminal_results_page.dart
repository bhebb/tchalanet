import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/i18n/i18n_models.dart';
import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/components/components.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../../../cashier/home/presentation/views/seller_terminal_nav_bar.dart';
import '../../data/models/draw_models.dart';
import '../draw_result_label.dart';
import '../view_models/draw_providers.dart';

enum _ResultPeriod { sevenDays, thirtyDays }

class SellerTerminalResultsPage extends ConsumerStatefulWidget {
  const SellerTerminalResultsPage({super.key});

  @override
  ConsumerState<SellerTerminalResultsPage> createState() =>
      _SellerTerminalResultsPageState();
}

class _SellerTerminalResultsPageState
    extends ConsumerState<SellerTerminalResultsPage> {
  _ResultPeriod _period = _ResultPeriod.sevenDays;
  String? _slotKey;

  PublicDrawResultHistoryQuery get _query {
    final today = _startOfDay(DateTime.now());
    final days = _period == _ResultPeriod.sevenDays ? 7 : 30;
    return PublicDrawResultHistoryQuery(
      from: today.subtract(Duration(days: days - 1)),
      to: today,
      slotKey: _slotKey,
    );
  }

  void _refresh() {
    ref.invalidate(publicDrawResultSlotsProvider);
    ref.invalidate(publicDrawResultHistoryProvider(_query));
  }

  @override
  Widget build(BuildContext context) {
    final translations = ref.watch(i18nBundleProvider);
    final slots = ref
        .watch(publicDrawResultSlotsProvider)
        .when(
          data: (slots) => slots,
          loading: () => const <PublicDrawResultSlot>[],
          error: (_, _) => const <PublicDrawResultSlot>[],
        );
    final historyAsync = ref.watch(publicDrawResultHistoryProvider(_query));

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.results.title')),
        actions: [
          IconButton(
            tooltip: translations.translate('common.refresh'),
            onPressed: _refresh,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            _ResultsFilters(
              period: _period,
              slotKey: _slotKey,
              slots: slots,
              translations: translations,
              onPeriodChanged: (period) => setState(() => _period = period),
              onSlotChanged: (slotKey) => setState(() => _slotKey = slotKey),
            ),
            Expanded(
              child: historyAsync.when(
                loading: () => Center(
                  child: FeedbackState(
                    kind: FeedbackStateKind.loading,
                    title: translations.translate('pos.results.loading'),
                    compact: true,
                  ),
                ),
                error: (_, _) => Center(
                  child: FeedbackState(
                    kind: FeedbackStateKind.error,
                    title: translations.translate('pos.results.load_error'),
                    actionLabel: translations.translate('common.retry'),
                    onAction: _refresh,
                    compact: true,
                  ),
                ),
                data: (history) =>
                    _ResultsList(history: history, translations: translations),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 2),
    );
  }
}

class _ResultsFilters extends StatelessWidget {
  const _ResultsFilters({
    required this.period,
    required this.slotKey,
    required this.slots,
    required this.translations,
    required this.onPeriodChanged,
    required this.onSlotChanged,
  });

  final _ResultPeriod period;
  final String? slotKey;
  final List<PublicDrawResultSlot> slots;
  final I18nBundle translations;
  final ValueChanged<_ResultPeriod> onPeriodChanged;
  final ValueChanged<String?> onSlotChanged;

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.fromLTRB(
      TchSpacing.s16,
      TchSpacing.s12,
      TchSpacing.s16,
      TchSpacing.s12,
    ),
    decoration: BoxDecoration(
      border: Border(
        bottom: BorderSide(color: Theme.of(context).colorScheme.outlineVariant),
      ),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SegmentedButton<_ResultPeriod>(
          segments: [
            ButtonSegment(
              value: _ResultPeriod.sevenDays,
              label: Text(translations.translate('pos.results.last_7_days')),
            ),
            ButtonSegment(
              value: _ResultPeriod.thirtyDays,
              label: Text(translations.translate('pos.results.last_30_days')),
            ),
          ],
          selected: {period},
          onSelectionChanged: (selection) => onPeriodChanged(selection.first),
        ),
        const SizedBox(height: TchSpacing.s12),
        DropdownButtonFormField<String?>(
          key: ValueKey(slotKey),
          initialValue: slotKey,
          decoration: InputDecoration(
            labelText: translations.translate('pos.results.draw_filter'),
            prefixIcon: const Icon(Icons.filter_alt_outlined),
          ),
          items: [
            DropdownMenuItem<String?>(
              value: null,
              child: Text(translations.translate('pos.results.all_draws')),
            ),
            for (final slot in slots)
              DropdownMenuItem<String?>(
                value: slot.slotKey,
                child: Text(
                  localizedPublicDrawResultSlotLabel(slot, translations),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
          ],
          onChanged: onSlotChanged,
        ),
      ],
    ),
  );
}

class _ResultsList extends StatelessWidget {
  const _ResultsList({required this.history, required this.translations});

  final PublicDrawResultHistory history;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    if (history.items.isEmpty) {
      return Center(
        child: FeedbackState(
          kind: FeedbackStateKind.empty,
          title: translations.translate('pos.results.empty'),
          compact: true,
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(TchSpacing.s16),
      itemCount: history.items.length,
      separatorBuilder: (_, _) => const SizedBox(height: TchSpacing.s12),
      itemBuilder: (context, index) =>
          _ResultCard(result: history.items[index], translations: translations),
    );
  }
}

class _ResultCard extends StatelessWidget {
  const _ResultCard({required this.result, required this.translations});

  final PublicDrawResultRow result;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final status = _resultStatus(result.status, translations);
    final quality = _resultQuality(result.quality, translations);

    return SurfaceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TchProviderLogo(providerCode: result.provider, size: 36),
              const SizedBox(width: TchSpacing.s12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      localizedPublicDrawResultRowLabel(result, translations),
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s4),
                    Text(
                      _resultOccurrenceLabel(result),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  StatusBadge(label: status.label, kind: status.kind),
                  if (quality != null) ...[
                    const SizedBox(height: TchSpacing.s4),
                    StatusBadge(label: quality.label, kind: quality.kind),
                  ],
                ],
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s16),
          if (result.hasKnownLots)
            Wrap(
              spacing: TchSpacing.s8,
              runSpacing: TchSpacing.s8,
              children: [
                for (final lot in const ['LOT1', 'LOT2', 'LOT3', 'LOT4'])
                  _LotValue(
                    label: translations.translate(
                      'pos.results.${lot.toLowerCase()}',
                    ),
                    value: result.lotValue(lot),
                  ),
              ],
            )
          // Existing API deployments expose only `numbers`. Keep results usable
          // during the API rollout; named lots supersede this once available.
          else if (result.numbers.isNotEmpty)
            Wrap(
              spacing: TchSpacing.s8,
              runSpacing: TchSpacing.s8,
              children: [
                for (final number in result.numbers) _ResultNumber(value: number),
              ],
            )
          else
            Text(
              translations.translate('pos.results.no_numbers'),
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
        ],
      ),
    );
  }
}

class _ResultNumber extends StatelessWidget {
  const _ResultNumber({required this.value});

  final String value;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      constraints: const BoxConstraints(minWidth: 48),
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s12,
        vertical: TchSpacing.s8,
      ),
      decoration: BoxDecoration(
        color: scheme.primary,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        value,
        textAlign: TextAlign.center,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          color: scheme.onPrimary,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _LotValue extends StatelessWidget {
  const _LotValue({required this.label, required this.value});

  final String label;
  final String? value;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isMissing = value == null || value!.isEmpty;
    return Container(
      width: 72,
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s8,
        vertical: TchSpacing.s8,
      ),
      decoration: BoxDecoration(
        color: isMissing ? scheme.surfaceContainerHigh : scheme.primary,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: isMissing ? scheme.onSurfaceVariant : scheme.onPrimary,
            ),
          ),
          const SizedBox(height: TchSpacing.s4),
          Text(
            isMissing ? '×' : value!,
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
              color: isMissing ? scheme.onSurfaceVariant : scheme.onPrimary,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

({String label, StatusBadgeKind kind}) _resultStatus(
  String status,
  I18nBundle translations,
) => switch (drawResultDisplayStatus(status)) {
  DrawResultDisplayStatus.confirmed => (
    label: translations.translate('pos.results.status_confirmed'),
    kind: StatusBadgeKind.ready,
  ),
  DrawResultDisplayStatus.provisional => (
    label: translations.translate('pos.results.status_provisional'),
    kind: StatusBadgeKind.warning,
  ),
  DrawResultDisplayStatus.corrected => (
    label: translations.translate('pos.results.status_corrected'),
    kind: StatusBadgeKind.warning,
  ),
  DrawResultDisplayStatus.unavailable => (
    label: translations.translate('pos.results.status_unavailable'),
    kind: StatusBadgeKind.blocked,
  ),
  DrawResultDisplayStatus.unknown => (
    label: translations.translate('pos.results.status_unknown'),
    kind: StatusBadgeKind.neutral,
  ),
};

({String label, StatusBadgeKind kind})? _resultQuality(
  String? quality,
  I18nBundle translations,
) {
  final displayQuality = drawResultDisplayQuality(quality);
  if (displayQuality == null) return null;
  return switch (displayQuality) {
    DrawResultDisplayQuality.complete => (
      label: translations.translate('pos.results.quality_complete'),
      kind: StatusBadgeKind.ready,
    ),
    DrawResultDisplayQuality.suspect => (
      label: translations.translate('pos.results.quality_suspect'),
      kind: StatusBadgeKind.warning,
    ),
    DrawResultDisplayQuality.invalid => (
      label: translations.translate('pos.results.quality_invalid'),
      kind: StatusBadgeKind.blocked,
    ),
    DrawResultDisplayQuality.unknown => (
      label: translations.translate('pos.results.quality_unknown'),
      kind: StatusBadgeKind.missing,
    ),
  };
}

String _resultOccurrenceLabel(PublicDrawResultRow result) => [
  result.resultDate,
  result.displayDrawTime,
].where((value) => value.isNotEmpty).join(' · ');

DateTime _startOfDay(DateTime value) =>
    DateTime(value.year, value.month, value.day);
