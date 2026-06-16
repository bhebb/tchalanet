import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../cashier/home/presentation/views/seller_terminal_nav_bar.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_ticket_service.dart';

enum _DateFilter { today, yesterday }

final _historyProvider = FutureProvider<List<CashierTicketSummaryView>>(
  (ref) => ref.watch(cashierTicketServiceProvider).listRecent(size: 50),
);

class CashierHistoryPage extends ConsumerStatefulWidget {
  const CashierHistoryPage({super.key});

  @override
  ConsumerState<CashierHistoryPage> createState() => _CashierHistoryPageState();
}

class _CashierHistoryPageState extends ConsumerState<CashierHistoryPage> {
  _DateFilter _filter = _DateFilter.today;
  String _search = '';

  List<CashierTicketSummaryView> _applyFilter(
      List<CashierTicketSummaryView> all) {
    final now = DateTime.now();
    final todayStart = DateTime(now.year, now.month, now.day);
    final yesterdayStart = todayStart.subtract(const Duration(days: 1));

    return all.where((t) {
      if (t.placedAt == null) return false;
      final local = t.placedAt!.toLocal();
      final inRange = _filter == _DateFilter.today
          ? !local.isBefore(todayStart)
          : !local.isBefore(yesterdayStart) && local.isBefore(todayStart);
      if (!inRange) return false;
      if (_search.isEmpty) return true;
      final q = _search.toUpperCase();
      return t.ticketCode.toUpperCase().contains(q) ||
          (t.publicCode?.toUpperCase().contains(q) ?? false) ||
          (t.drawChannelName?.toUpperCase().contains(q) ?? false);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final historyAsync = ref.watch(_historyProvider);
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Historique Des Tickets'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () => ref.invalidate(_historyProvider),
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter bar
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: TchSpacing.s16,
              vertical: TchSpacing.s12,
            ),
            decoration: BoxDecoration(
              color: scheme.surfaceContainerLowest,
              border: Border(
                  bottom: BorderSide(color: scheme.outlineVariant)),
            ),
            child: Row(
              children: [
                // Segmented filter
                Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    color: scheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      _FilterTab(
                        label: "Aujourd'hui",
                        selected: _filter == _DateFilter.today,
                        onTap: () => setState(() => _filter = _DateFilter.today),
                      ),
                      const SizedBox(width: 4),
                      _FilterTab(
                        label: 'Hier',
                        selected: _filter == _DateFilter.yesterday,
                        onTap: () =>
                            setState(() => _filter = _DateFilter.yesterday),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: TchSpacing.s12),
                // Search
                Expanded(
                  child: TextField(
                    onChanged: (v) => setState(() => _search = v),
                    decoration: InputDecoration(
                      hintText: 'Rechercher ticket…',
                      prefixIcon: const Icon(Icons.search_rounded, size: 18),
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(
                        vertical: TchSpacing.s8,
                        horizontal: TchSpacing.s12,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(TchRadius.md),
                        borderSide: BorderSide(color: scheme.outlineVariant),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(TchRadius.md),
                        borderSide: BorderSide(color: scheme.outlineVariant),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // List
          Expanded(
            child: historyAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.cloud_off_rounded,
                        size: 48, color: scheme.error),
                    const SizedBox(height: TchSpacing.s16),
                    Text(e.toString(), textAlign: TextAlign.center),
                    const SizedBox(height: TchSpacing.s24),
                    FilledButton.tonal(
                      onPressed: () => ref.invalidate(_historyProvider),
                      child: const Text('Réessayer'),
                    ),
                  ],
                ),
              ),
              data: (all) {
                final filtered = _applyFilter(all);
                if (filtered.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.receipt_long_rounded,
                            size: 48,
                            color: scheme.onSurface.withValues(alpha: 0.2)),
                        const SizedBox(height: TchSpacing.s16),
                        Text(
                          _search.isNotEmpty
                              ? 'Aucun ticket correspondant'
                              : _filter == _DateFilter.today
                                  ? 'Aucun ticket aujourd\'hui'
                                  : 'Aucun ticket hier',
                          style: textTheme.bodyMedium?.copyWith(
                            color: scheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  );
                }
                return ListView.separated(
                  itemCount: filtered.length,
                  separatorBuilder: (_, _) =>
                      Divider(height: 1, color: scheme.outlineVariant),
                  itemBuilder: (context, i) => _TicketRow(
                    ticket: filtered[i],
                    onTap: () =>
                        context.push('/pos/tickets/${filtered[i].id}'),
                    onPrint: () {}, // TODO: wire print
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

// ─── Filter tab ───────────────────────────────────────────────────────────────

class _FilterTab extends StatelessWidget {
  const _FilterTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s16,
          vertical: TchSpacing.s8,
        ),
        decoration: BoxDecoration(
          color: selected ? scheme.surfaceContainerLowest : Colors.transparent,
          borderRadius: BorderRadius.circular(TchRadius.sm),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: scheme.shadow.withValues(alpha: 0.1),
                    blurRadius: 4,
                  )
                ]
              : null,
        ),
        child: Text(
          label,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: selected ? scheme.primary : scheme.onSurfaceVariant,
                fontWeight:
                    selected ? FontWeight.w600 : FontWeight.w400,
              ),
        ),
      ),
    );
  }
}

// ─── Ticket row ───────────────────────────────────────────────────────────────

class _TicketRow extends StatelessWidget {
  const _TicketRow({
    required this.ticket,
    required this.onTap,
    required this.onPrint,
  });

  final CashierTicketSummaryView ticket;
  final VoidCallback onTap;
  final VoidCallback onPrint;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final isCancelled = ticket.status == 'CANCELLED' || ticket.status == 'VOIDED';
    final time = ticket.placedAt != null
        ? '${ticket.placedAt!.toLocal().hour.toString().padLeft(2, '0')}:${ticket.placedAt!.toLocal().minute.toString().padLeft(2, '0')}'
        : '—';

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
              // Time
              SizedBox(
                width: 48,
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
              const SizedBox(width: TchSpacing.s12),

              // Public code + draw channel name
              Expanded(
                flex: 2,
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
                      ticket.drawLabel,
                      style: textTheme.labelSmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),

              // Amount
              Expanded(
                flex: 2,
                child: Text(
                  ticket.formattedAmount,
                  style: textTheme.labelMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isCancelled
                        ? scheme.onSurfaceVariant
                        : scheme.onSurface,
                  ),
                ),
              ),

              // Status badge
              _StatusBadge(status: ticket.status),
              const SizedBox(width: TchSpacing.s8),

              // Actions
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _IconBtn(
                    icon: Icons.visibility_rounded,
                    onTap: onTap,
                  ),
                  _IconBtn(
                    icon: Icons.print_rounded,
                    onTap: onPrint,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (bgColor, fgColor, label) = switch (status) {
      'PLACED' => (TchColors.successContainer, TchColors.success, 'VALIDÉ'),
      'CANCELLED' => (scheme.errorContainer, scheme.onErrorContainer, 'ANNULÉ'),
      'VOIDED' => (
          scheme.surfaceContainerHighest,
          scheme.onSurfaceVariant,
          'INVALIDÉ'
        ),
      _ => (scheme.surfaceContainerHigh, scheme.onSurface, status),
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
        label,
        style: TextStyle(
          color: fgColor,
          fontWeight: FontWeight.w700,
          fontSize: 10,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

class _IconBtn extends StatelessWidget {
  const _IconBtn({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(icon, size: 20),
      visualDensity: VisualDensity.compact,
      onPressed: onTap,
      color: Theme.of(context).colorScheme.onSurfaceVariant,
    );
  }
}
