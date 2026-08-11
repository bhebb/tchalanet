import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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

final _ticketDetailProvider =
    FutureProvider.family<CashierTicketDetailsView, String>(
      (ref, ticketId) =>
          ref.watch(cashierTicketServiceProvider).getDetails(ticketId),
    );

/// Customer-safe receipt detail shown from the ticket history.
class CashierTicketDetailPage extends ConsumerWidget {
  const CashierTicketDetailPage({super.key, required this.ticketId});

  final String ticketId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(_ticketDetailProvider(ticketId));
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.tickets.detail_title')),
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 1),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => Center(
          child: FeedbackState(
            kind: FeedbackStateKind.offline,
            title: translations.translate('pos.tickets.load_error'),
            actionLabel: translations.translate('common.retry'),
            onAction: () => ref.invalidate(_ticketDetailProvider(ticketId)),
          ),
        ),
        data: (detail) => _DetailBody(
          detail: detail,
          ticketId: ticketId,
          translations: translations,
        ),
      ),
    );
  }
}

class _DetailBody extends ConsumerWidget {
  const _DetailBody({
    required this.detail,
    required this.ticketId,
    required this.translations,
  });

  final CashierTicketDetailsView detail;
  final String ticketId;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(
        TchSpacing.s16,
        TchSpacing.s16,
        TchSpacing.s16,
        TchSpacing.s24,
      ),
      children: [
        _ReceiptCard(detail: detail, translations: translations),
        const SizedBox(height: TchSpacing.s16),
        _ActionButton(
          icon: Icons.print_rounded,
          label: translations.translate('pos.tickets.reprint_confirm'),
          primary: true,
          onTap: () => requestTicketReprint(context, ref, ticketId),
        ),
      ],
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
      'APPROVED' || 'PLACED' => (
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
        TchColors.warningContainer,
        TchColors.warning,
        'pos.tickets.status_unknown',
      ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s12,
        vertical: TchSpacing.s4,
      ),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.pill),
      ),
      child: Text(
        translations.translate(key),
        style: TextStyle(
          color: fgColor,
          fontWeight: FontWeight.w700,
          fontSize: 12,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

class _ReceiptCard extends StatelessWidget {
  const _ReceiptCard({required this.detail, required this.translations});

  final CashierTicketDetailsView detail;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final displayCode = detail.publicCode ?? detail.ticketCode;
    final drawLabel = localizedDrawIdentityLabel(
      providerCode: detail.resultProvider,
      slotKey: detail.resultSlotKey,
      fallback: detail.drawLabel,
      translations: translations,
    );

    return Container(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(TchRadius.lg),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(TchSpacing.s16),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: scheme.outlineVariant)),
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
                            translations.translate('pos.tickets.public_code'),
                            style: textTheme.labelSmall?.copyWith(
                              color: scheme.onSurfaceVariant,
                              letterSpacing: 0.5,
                            ),
                          ),
                          const SizedBox(height: TchSpacing.s4),
                          InkWell(
                            onTap: () {
                              Clipboard.setData(
                                ClipboardData(text: displayCode),
                              );
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    translations.translate(
                                      'pos.tickets.copied',
                                    ),
                                  ),
                                  duration: const Duration(seconds: 1),
                                  behavior: SnackBarBehavior.floating,
                                ),
                              );
                            },
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Flexible(
                                  child: Text(
                                    displayCode,
                                    style: textTheme.headlineMedium?.copyWith(
                                      color: scheme.primary,
                                      fontWeight: FontWeight.w700,
                                    ),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                                const SizedBox(width: TchSpacing.s8),
                                Icon(
                                  Icons.copy_rounded,
                                  size: 16,
                                  color: scheme.onSurfaceVariant,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    _StatusBadge(
                      status: detail.status,
                      translations: translations,
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s12),
                Row(
                  children: [
                    Expanded(
                      child: _MetaItem(
                        label: translations.translate('pos.tickets.sale_time'),
                        value: detail.placedAt == null
                            ? '—'
                            : _fmtDateTime(context, detail.placedAt!),
                      ),
                    ),
                    Expanded(
                      child: _MetaItem(
                        label: translations.translate('pos.tickets.draw'),
                        value: drawLabel,
                        alignRight: true,
                      ),
                    ),
                  ],
                ),
                if (detail.outletName != null ||
                    detail.terminalCode != null) ...[
                  const SizedBox(height: TchSpacing.s8),
                  Row(
                    children: [
                      if (detail.outletName != null)
                        Expanded(
                          child: _MetaItem(
                            label: translations.translate('pos.tickets.outlet'),
                            value: detail.outletName!,
                          ),
                        ),
                      if (detail.terminalCode != null)
                        Expanded(
                          child: _MetaItem(
                            label: translations.translate(
                              'pos.tickets.terminal',
                            ),
                            value: detail.terminalCode!,
                            alignRight: true,
                          ),
                        ),
                    ],
                  ),
                ],
                if (detail.sellerDisplayName != null) ...[
                  const SizedBox(height: TchSpacing.s8),
                  _MetaItem(
                    label: translations.translate('pos.tickets.seller'),
                    value: detail.sellerDisplayName!,
                  ),
                ],
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(TchSpacing.s16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  translations.translate('pos.tickets.game_details'),
                  style: textTheme.labelSmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                    letterSpacing: 0.5,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: TchSpacing.s12),
                if (detail.lines.isEmpty)
                  Text('—', style: textTheme.bodySmall)
                else
                  for (var index = 0; index < detail.lines.length; index++) ...[
                    if (index > 0)
                      Divider(color: scheme.outlineVariant, height: 1),
                    _LineRow(
                      line: detail.lines[index],
                      currency: detail.currency,
                    ),
                  ],
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.all(TchSpacing.s16),
            decoration: BoxDecoration(
              color: scheme.surfaceContainerLow,
              border: Border(top: BorderSide(color: scheme.outlineVariant)),
            ),
            child: Column(
              children: [
                _SummaryRow(
                  label: translations.translate('pos.tickets.stake'),
                  value: '${detail.formattedStake} ${detail.currency}',
                ),
                for (final charge in detail.charges) ...[
                  const SizedBox(height: TchSpacing.s4),
                  _SummaryRow(
                    label: charge.displayLabel,
                    value: '${charge.formattedAmount} ${detail.currency}',
                  ),
                ],
                const Divider(height: TchSpacing.s24),
                _SummaryRow(
                  label: translations.translate('pos.tickets.total'),
                  value: detail.formattedAmount,
                  emphasized: true,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _fmtDateTime(BuildContext context, DateTime value) {
    final local = value.toLocal();
    final material = MaterialLocalizations.of(context);
    final date = material.formatMediumDate(local);
    final time = material.formatTimeOfDay(
      TimeOfDay.fromDateTime(local),
      alwaysUse24HourFormat: MediaQuery.alwaysUse24HourFormatOf(context),
    );
    return '$date $time';
  }
}

class _LineRow extends StatelessWidget {
  const _LineRow({required this.line, required this.currency});

  final CashierTicketLineDetail line;
  final String currency;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final gameLabel = line.betTypeLabel?.isNotEmpty == true
        ? '${line.gameLabel} · ${line.betTypeLabel}'
        : line.gameLabel;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: TchSpacing.s12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: line.promotional
                  ? scheme.tertiaryContainer
                  : scheme.surfaceContainerLow,
              border: Border.all(color: scheme.outlineVariant),
              borderRadius: BorderRadius.circular(TchRadius.sm),
            ),
            child: Text(
              line.selection.length > 4
                  ? line.selection.substring(0, 4)
                  : line.selection,
              style: textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  gameLabel,
                  style: textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (line.promotional && line.promotionLabel != null)
                  Text(
                    line.promotionLabel!,
                    style: textTheme.labelSmall?.copyWith(
                      color: scheme.tertiary,
                    ),
                  ),
              ],
            ),
          ),
          Text(
            '${line.formattedStake} $currency',
            style: textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _MetaItem extends StatelessWidget {
  const _MetaItem({
    required this.label,
    required this.value,
    this.alignRight = false,
  });

  final String label;
  final String value;
  final bool alignRight;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: alignRight
          ? CrossAxisAlignment.end
          : CrossAxisAlignment.start,
      children: [
        Text(label, style: textTheme.labelSmall),
        const SizedBox(height: TchSpacing.s4),
        Text(
          value,
          textAlign: alignRight ? TextAlign.right : TextAlign.left,
          style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    this.emphasized = false,
  });

  final String label;
  final String value;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final style = emphasized
        ? Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)
        : Theme.of(context).textTheme.bodyMedium;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: style),
        Text(value, style: style),
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
  });

  final IconData icon;
  final String label;
  final bool primary;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => primary
      ? FilledButton.icon(
          onPressed: onTap,
          icon: Icon(icon),
          label: Text(label),
        )
      : OutlinedButton.icon(
          onPressed: onTap,
          icon: Icon(icon),
          label: Text(label),
        );
}
