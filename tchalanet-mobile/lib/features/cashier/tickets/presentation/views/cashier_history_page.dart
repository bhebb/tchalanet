import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/draw_identity_label.dart';
import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../cashier/home/presentation/views/seller_terminal_nav_bar.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_ticket_service.dart';
import '../print_ticket_action.dart';

enum _DateFilter { today, yesterday, custom }

final _historyProvider = FutureProvider.autoDispose
    .family<List<CashierTicketSummaryView>, _TicketHistoryQuery>((ref, query) {
      return ref
          .watch(cashierTicketServiceProvider)
          .listRecent(
            size: query.size,
            query: query.search,
            fromDate: query.fromDate,
            toDate: query.toDate,
          );
    });

class _TicketHistoryQuery {
  const _TicketHistoryQuery({
    required this.search,
    required this.fromDate,
    required this.toDate,
    required this.size,
  });

  final String search;
  final DateTime fromDate;
  final DateTime toDate;
  final int size;

  @override
  bool operator ==(Object other) =>
      other is _TicketHistoryQuery &&
      other.search == search &&
      other.fromDate == fromDate &&
      other.toDate == toDate &&
      other.size == size;

  @override
  int get hashCode => Object.hash(search, fromDate, toDate, size);
}

class CashierHistoryPage extends ConsumerStatefulWidget {
  const CashierHistoryPage({super.key});

  @override
  ConsumerState<CashierHistoryPage> createState() => _CashierHistoryPageState();
}

class _CashierHistoryPageState extends ConsumerState<CashierHistoryPage> {
  final _searchController = TextEditingController();
  _DateFilter _filter = _DateFilter.today;
  DateTime? _customDate;
  int _historySize = 50;
  String _search = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  _TicketHistoryQuery get _query {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final date = switch (_filter) {
      _DateFilter.today => today,
      _DateFilter.yesterday => today.subtract(const Duration(days: 1)),
      _DateFilter.custom => _customDate ?? today,
    };
    return _TicketHistoryQuery(
      search: _search,
      fromDate: date,
      toDate: date,
      size: _historySize,
    );
  }

  void _applySearch(String value) {
    final normalized = value.trim();
    if (normalized == _search) return;
    setState(() {
      _search = normalized;
      _historySize = 50;
    });
  }

  Future<void> _selectFilter(
    _DateFilter filter,
    I18nBundle translations,
  ) async {
    if (filter == _DateFilter.custom) {
      final initialDate = _customDate ?? DateTime.now();
      final picked = await showDatePicker(
        context: context,
        initialDate: initialDate,
        firstDate: DateTime.now().subtract(const Duration(days: 365)),
        lastDate: DateTime.now(),
        helpText: translations.translate('pos.tickets.pick_date'),
      );
      if (picked == null) return;
      setState(() {
        _filter = _DateFilter.custom;
        _customDate = DateTime(picked.year, picked.month, picked.day);
        _historySize = 50;
      });
      return;
    }
    setState(() {
      _filter = filter;
      _historySize = 50;
    });
  }

  Future<void> _refresh(_TicketHistoryQuery query) async {
    ref.invalidate(_historyProvider(query));
    await ref.read(_historyProvider(query).future);
  }

  @override
  Widget build(BuildContext context) {
    final query = _query;
    final historyAsync = ref.watch(_historyProvider(query));
    final translations = ref.watch(i18nBundleProvider);
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.tickets.history_title')),
        actions: [
          IconButton(
            tooltip: translations.translate('common.refresh'),
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () => ref.invalidate(_historyProvider(query)),
          ),
        ],
      ),
      body: Column(
        children: [
          _HistoryFilters(
            selected: _filter,
            customDate: _customDate,
            searchController: _searchController,
            translations: translations,
            onFilterChanged: (filter) => _selectFilter(filter, translations),
            onSearch: _applySearch,
            onClearSearch: () {
              _searchController.clear();
              _applySearch('');
            },
          ),
          Expanded(
            child: historyAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (_, _) => Center(
                child: FeedbackState(
                  kind: FeedbackStateKind.offline,
                  title: translations.translate('pos.tickets.load_error'),
                  actionLabel: translations.translate('common.retry'),
                  onAction: () => ref.invalidate(_historyProvider(query)),
                ),
              ),
              data: (tickets) {
                if (tickets.isEmpty) {
                  final emptyKey = _search.isNotEmpty
                      ? 'pos.tickets.empty_search'
                      : switch (_filter) {
                          _DateFilter.today => 'pos.tickets.empty_today',
                          _DateFilter.yesterday =>
                            'pos.tickets.empty_yesterday',
                          _DateFilter.custom => 'pos.tickets.empty_date',
                        };
                  return RefreshIndicator(
                    onRefresh: () => _refresh(query),
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      children: [
                        SizedBox(
                          height: MediaQuery.sizeOf(context).height * 0.6,
                          child: Center(
                            child: FeedbackState(
                              kind: FeedbackStateKind.empty,
                              title: translations.translate(emptyKey),
                              compact: true,
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                }
                final canLoadMore = tickets.length >= query.size;
                return RefreshIndicator(
                  onRefresh: () => _refresh(query),
                  child: ListView.separated(
                    itemCount: tickets.length + (canLoadMore ? 1 : 0),
                    separatorBuilder: (_, _) =>
                        Divider(height: 1, color: scheme.outlineVariant),
                    itemBuilder: (context, index) {
                      if (canLoadMore && index == tickets.length) {
                        return _HistoryMoreFooter(
                          shownCount: tickets.length,
                          translations: translations,
                          onLoadMore: () => setState(() => _historySize += 50),
                        );
                      }
                      final ticket = tickets[index];
                      return _TicketRow(
                        ticket: ticket,
                        translations: translations,
                        onTap: () => context.push('/pos/tickets/${ticket.id}'),
                        onPrint: () =>
                            requestTicketReprint(context, ref, ticket.id),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 1),
    );
  }
}

class _HistoryFilters extends StatelessWidget {
  const _HistoryFilters({
    required this.selected,
    required this.customDate,
    required this.searchController,
    required this.translations,
    required this.onFilterChanged,
    required this.onSearch,
    required this.onClearSearch,
  });

  final _DateFilter selected;
  final DateTime? customDate;
  final TextEditingController searchController;
  final I18nBundle translations;
  final ValueChanged<_DateFilter> onFilterChanged;
  final ValueChanged<String> onSearch;
  final VoidCallback onClearSearch;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        border: Border(bottom: BorderSide(color: scheme.outlineVariant)),
      ),
      child: Column(
        children: [
          SegmentedButton<_DateFilter>(
            segments: [
              ButtonSegment(
                value: _DateFilter.today,
                label: Text(translations.translate('pos.tickets.today')),
              ),
              ButtonSegment(
                value: _DateFilter.yesterday,
                label: Text(translations.translate('pos.tickets.yesterday')),
              ),
              ButtonSegment(
                value: _DateFilter.custom,
                label: Text(
                  customDate == null
                      ? translations.translate('pos.tickets.other_date')
                      : MaterialLocalizations.of(
                          context,
                        ).formatMediumDate(customDate!),
                ),
              ),
            ],
            selected: {selected},
            onSelectionChanged: (value) => onFilterChanged(value.first),
            showSelectedIcon: false,
          ),
          const SizedBox(height: TchSpacing.s12),
          TextField(
            controller: searchController,
            textInputAction: TextInputAction.search,
            onSubmitted: onSearch,
            decoration: InputDecoration(
              hintText: translations.translate('pos.tickets.search'),
              prefixIcon: const Icon(Icons.search_rounded, size: 20),
              suffixIcon: searchController.text.isEmpty
                  ? null
                  : IconButton(
                      icon: const Icon(Icons.close_rounded),
                      onPressed: onClearSearch,
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _HistoryMoreFooter extends StatelessWidget {
  const _HistoryMoreFooter({
    required this.shownCount,
    required this.translations,
    required this.onLoadMore,
  });

  final int shownCount;
  final I18nBundle translations;
  final VoidCallback onLoadMore;

  @override
  Widget build(BuildContext context) {
    final message = translations
        .translate('pos.tickets.showing_first')
        .replaceAll('{{count}}', shownCount.toString());
    return Padding(
      padding: const EdgeInsets.all(TchSpacing.s16),
      child: Column(
        children: [
          Text(
            message,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: TchSpacing.s8),
          OutlinedButton.icon(
            onPressed: onLoadMore,
            icon: const Icon(Icons.expand_more_rounded),
            label: Text(translations.translate('pos.tickets.show_more')),
          ),
        ],
      ),
    );
  }
}

class _TicketRow extends StatelessWidget {
  const _TicketRow({
    required this.ticket,
    required this.translations,
    required this.onTap,
    required this.onPrint,
  });

  final CashierTicketSummaryView ticket;
  final I18nBundle translations;
  final VoidCallback onTap;
  final VoidCallback onPrint;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final isCancelled =
        ticket.status == 'CANCELLED' || ticket.status == 'VOIDED';
    final time = ticket.placedAt == null
        ? '—'
        : '${ticket.placedAt!.toLocal().hour.toString().padLeft(2, '0')}:'
              '${ticket.placedAt!.toLocal().minute.toString().padLeft(2, '0')}';
    final drawLabel = localizedDrawIdentityLabel(
      providerCode: ticket.resultProvider,
      slotKey: ticket.resultSlotKey,
      fallback: ticket.drawLabel,
      translations: translations,
    );

    return InkWell(
      onTap: onTap,
      child: Opacity(
        opacity: isCancelled ? 0.7 : 1.0,
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: TchSpacing.s16,
            vertical: TchSpacing.s12,
          ),
          child: Row(
            children: [
              SizedBox(
                width: 44,
                child: Text(
                  time,
                  style: textTheme.labelMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isCancelled
                        ? scheme.onSurfaceVariant
                        : scheme.onSurface,
                  ),
                ),
              ),
              const SizedBox(width: TchSpacing.s8),
              TchProviderLogo(
                providerCode:
                    ticket.resultProvider ??
                    TchProviderLogo.providerCodeFromLabel(
                      ticket.drawChannelName,
                    ),
                size: 32,
              ),
              const SizedBox(width: TchSpacing.s12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      ticket.displayCode,
                      style: textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: isCancelled
                            ? scheme.onSurfaceVariant
                            : scheme.primary,
                      ),
                    ),
                    Text(
                      drawLabel,
                      style: textTheme.labelSmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: TchSpacing.s8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    ticket.formattedAmount,
                    style: textTheme.labelMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: isCancelled
                          ? scheme.onSurfaceVariant
                          : scheme.onSurface,
                    ),
                  ),
                  const SizedBox(height: TchSpacing.s4),
                  _StatusBadge(
                    status: ticket.status,
                    translations: translations,
                  ),
                ],
              ),
              const SizedBox(width: TchSpacing.s4),
              IconButton(
                tooltip: translations.translate('pos.tickets.reprint_confirm'),
                icon: const Icon(Icons.print_rounded, size: 20),
                onPressed: onPrint,
                color: scheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status, required this.translations});

  final String status;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (bgColor, fgColor, key) = switch (status) {
      'PLACED' => (
        TchColors.successContainer,
        TchColors.success,
        'pos.tickets.status_placed',
      ),
      'CANCELLED' => (
        scheme.errorContainer,
        scheme.onErrorContainer,
        'pos.tickets.status_cancelled',
      ),
      'VOIDED' => (
        scheme.surfaceContainerHighest,
        scheme.onSurfaceVariant,
        'pos.tickets.status_voided',
      ),
      _ => (
        scheme.surfaceContainerHigh,
        scheme.onSurface,
        'pos.tickets.status_unknown',
      ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s8,
        vertical: 3,
      ),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.xs),
      ),
      child: Text(
        translations.translate(key),
        style: TextStyle(
          color: fgColor,
          fontWeight: FontWeight.w700,
          fontSize: 12,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}
