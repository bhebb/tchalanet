import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../design_system/tokens/tch_colors.dart';

import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_ticket_service.dart';

final _ticketDetailProvider = FutureProvider.family<CashierTicketDetailsView, String>(
  (ref, ticketId) => ref.watch(cashierTicketServiceProvider).getDetails(ticketId),
);

/// Ticket detail / receipt view — shown from History tab, Scanner, or post-sell.
class CashierTicketDetailPage extends ConsumerWidget {
  const CashierTicketDetailPage({super.key, required this.ticketId});

  final String ticketId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(_ticketDetailProvider(ticketId));
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Détails du Ticket'),
      ),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
              const SizedBox(height: TchSpacing.s16),
              Text(e.toString(), textAlign: TextAlign.center),
              const SizedBox(height: TchSpacing.s24),
              FilledButton.tonal(
                onPressed: () => ref.invalidate(_ticketDetailProvider(ticketId)),
                child: const Text('Réessayer'),
              ),
            ],
          ),
        ),
        data: (detail) => _DetailBody(detail: detail),
      ),
    );
  }
}

class _DetailBody extends StatelessWidget {
  const _DetailBody({required this.detail});

  final CashierTicketDetailsView detail;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      body: ListView(
        padding: const EdgeInsets.fromLTRB(
          TchSpacing.s16, TchSpacing.s16, TchSpacing.s16, TchSpacing.s64,
        ),
        children: [
          _ReceiptCard(detail: detail),
          const SizedBox(height: TchSpacing.s16),
          Row(
            children: [
              Expanded(
                child: _ActionButton(
                  icon: Icons.share_rounded,
                  label: 'Partager',
                  primary: true,
                  onTap: () {},
                ),
              ),
              const SizedBox(width: TchSpacing.s12),
              Expanded(
                child: _ActionButton(
                  icon: Icons.print_rounded,
                  label: 'Imprimer',
                  primary: true,
                  onTap: () {},
                ),
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s12),
          _ActionButton(
            icon: Icons.cancel_outlined,
            label: 'Annuler le ticket',
            primary: false,
            isDestructive: true,
            onTap: () => _confirmCancel(context),
          ),
          const SizedBox(height: TchSpacing.s32),
          Center(
            child: Column(
              children: [
                Container(
                  width: 96,
                  height: 96,
                  padding: const EdgeInsets.all(TchSpacing.s8),
                  decoration: BoxDecoration(
                    color: scheme.surfaceContainerLowest,
                    border: Border.all(color: scheme.outlineVariant),
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                  child: Icon(
                    Icons.qr_code_2_rounded,
                    size: 64,
                    color: scheme.onSurface.withValues(alpha: 0.3),
                  ),
                ),
                const SizedBox(height: TchSpacing.s8),
                Text(
                  'POS VERIFICATION SECURE',
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: scheme.onSurfaceVariant.withValues(alpha: 0.5),
                        letterSpacing: 1,
                        fontWeight: FontWeight.w700,
                        fontSize: 10,
                      ),
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: _BottomNav(),
    );
  }

  void _confirmCancel(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Annuler le ticket ?'),
        content: const Text(
            'Cette action est irréversible. Le ticket sera annulé immédiatement.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Retour'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
            ),
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler le ticket'),
          ),
        ],
      ),
    );
  }
}

// ─── Receipt card ─────────────────────────────────────────────────────────────

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (bgColor, fgColor, label) = switch (status) {
      'PLACED' => (TchColors.successContainer, TchColors.success, 'ACTIF'),
      'CANCELLED' => (scheme.errorContainer, scheme.onErrorContainer, 'ANNULÉ'),
      'VOIDED' => (
          scheme.surfaceContainerHighest,
          scheme.onSurfaceVariant,
          'INVALIDÉ'
        ),
      _ => (TchColors.warningContainer, TchColors.warning, status),
    };
    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s12, vertical: TchSpacing.s4),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.pill),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: fgColor,
          fontWeight: FontWeight.w700,
          fontSize: 11,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

class _ReceiptCard extends StatelessWidget {
  const _ReceiptCard({required this.detail});

  final CashierTicketDetailsView detail;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    // Line entries not available in CashierTicketDetailsResponse V1 (no lines field)
    const entries = <_TicketEntry>[];
    final total = detail.formattedAmount;

    return Container(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(TchRadius.lg),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(TchSpacing.s16),
            decoration: BoxDecoration(
              border: Border(
                bottom: BorderSide(
                  color: scheme.outlineVariant,
                  style: BorderStyle.solid,
                ),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'RÉFÉRENCE TICKET',
                            style: textTheme.labelSmall?.copyWith(
                              color: scheme.onSurfaceVariant,
                              letterSpacing: 0.5,
                            ),
                          ),
                          const SizedBox(height: TchSpacing.s4),
                          Text(
                            detail.ticketCode,
                            style: textTheme.headlineMedium?.copyWith(
                              color: scheme.primary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                    _StatusBadge(status: detail.status),
                  ],
                ),
                const SizedBox(height: TchSpacing.s12),
                Row(
                  children: [
                    Expanded(
                      child: _MetaItem(
                        label: 'DATE ET HEURE',
                        value: detail.placedAt != null
                            ? detail.placedAt!.toLocal().toString().substring(0, 16)
                            : '—',
                      ),
                    ),
                    Expanded(
                      child: _MetaItem(
                        label: 'TIRAGE',
                        value: detail.drawId != null
                            ? detail.drawId!.substring(0, 8).toUpperCase()
                            : '—',
                        alignRight: true,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Entries
          Padding(
            padding: const EdgeInsets.all(TchSpacing.s16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'DÉTAIL DES JEUX',
                  style: textTheme.labelSmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                    letterSpacing: 0.5,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: TchSpacing.s12),
                for (int i = 0; i < entries.length; i++) ...[
                  if (i > 0)
                    Divider(color: scheme.surfaceContainerLow, height: 1),
                  _EntryRow(entry: entries[i]),
                ],
              ],
            ),
          ),

          // Financial summary
          Container(
            padding: const EdgeInsets.all(TchSpacing.s16),
            decoration: BoxDecoration(
              color: scheme.surfaceContainerLow,
              border: Border(
                  top: BorderSide(color: scheme.outlineVariant)),
            ),
            child: Column(
              children: [
                const _SummaryRow(label: 'Sous-total', value: '500 HTG'),
                const SizedBox(height: TchSpacing.s4),
                const _SummaryRow(label: 'Frais', value: '0 HTG'),
                const Divider(height: TchSpacing.s24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'TOTAL',
                      style: textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700),
                    ),
                    Text(
                      total,
                      style: textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: scheme.primary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Bottom nav ───────────────────────────────────────────────────────────────

class _BottomNav extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: 1, // Historique active
      onDestinationSelected: (_) {},
      destinations: const [
        NavigationDestination(
            icon: Icon(Icons.confirmation_number_rounded), label: 'Ventes'),
        NavigationDestination(
            icon: Icon(Icons.history_rounded), label: 'Historique'),
        NavigationDestination(
            icon: Icon(Icons.qr_code_scanner_rounded), label: 'Scanner'),
        NavigationDestination(
            icon: Icon(Icons.person_rounded), label: 'Profil'),
      ],
    );
  }
}

// ─── Small widgets ────────────────────────────────────────────────────────────

class _TicketEntry {
  const _TicketEntry({
    required this.number,
    required this.game,
    required this.amount,
  });

  final String number;
  final String game;
  final String amount;
}

class _EntryRow extends StatelessWidget {
  const _EntryRow({required this.entry});

  final _TicketEntry entry;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: TchSpacing.s12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: scheme.surfaceContainerLow,
              border: Border.all(
                  color: scheme.outlineVariant.withValues(alpha: 0.5)),
              borderRadius: BorderRadius.circular(TchRadius.sm),
            ),
            child: Text(
              entry.number.length > 4 ? '…' : entry.number,
              style: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Text(
              entry.game,
              style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          Text(
            entry.amount,
            style:
                textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _MetaItem extends StatelessWidget {
  const _MetaItem(
      {required this.label, required this.value, this.alignRight = false});

  final String label;
  final String value;
  final bool alignRight;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment:
          alignRight ? CrossAxisAlignment.end : CrossAxisAlignment.start,
      children: [
        Text(label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                color: scheme.onSurfaceVariant, fontSize: 10, letterSpacing: 0.5)),
        const SizedBox(height: 2),
        Text(value,
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(fontWeight: FontWeight.w500)),
      ],
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label,
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(color: scheme.onSurfaceVariant)),
        Text(value,
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(fontWeight: FontWeight.w600)),
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.icon,
    required this.label,
    required this.primary,
    required this.onTap,
    this.isDestructive = false,
  });

  final IconData icon;
  final String label;
  final bool primary;
  final bool isDestructive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (isDestructive) {
      return OutlinedButton.icon(
        onPressed: onTap,
        icon: Icon(icon, color: scheme.error),
        label: Text(label, style: TextStyle(color: scheme.error)),
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          side: BorderSide(color: scheme.error.withValues(alpha: 0.3)),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(TchRadius.md)),
        ),
      );
    }
    return FilledButton.icon(
      onPressed: onTap,
      icon: Icon(icon),
      label: Text(label),
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(TchRadius.md)),
      ),
    );
  }
}
