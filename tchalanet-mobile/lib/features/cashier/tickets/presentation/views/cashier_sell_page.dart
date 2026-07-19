import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/observability/diagnostic_info.dart';
import '../../../../../core/observability/diagnostic_repository.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../home/data/models/cashier_home_models.dart';
import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../../data/models/cashier_sell_catalog_models.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../view_models/sell_controller.dart';

class CashierSellPage extends ConsumerStatefulWidget {
  const CashierSellPage({super.key, this.preselectedDrawId});

  /// When set (e.g., tapped from home draw list), this draw is pre-selected
  /// before the catalog loads. Falls back to home.primaryDraw if null.
  final String? preselectedDrawId;

  @override
  ConsumerState<CashierSellPage> createState() => _CashierSellPageState();
}

class _CashierSellPageState extends ConsumerState<CashierSellPage> {
  final _stakeController = TextEditingController();

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      final home = ref
          .read(cashierHomeProvider)
          .when(data: (h) => h, loading: () => null, error: (_, _) => null);
      ref
          .read(sellControllerProvider.notifier)
          .loadCatalog(
            preselectedDrawId:
                widget.preselectedDrawId ?? home?.primaryDraw?.drawId,
            currency: home?.currency,
          );
    });
  }

  @override
  void dispose() {
    _stakeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(sellControllerProvider);
    final translations = ref.watch(i18nBundleProvider);
    final homeAsync = ref.watch(cashierHomeProvider);
    final lastDiagnostic = ref.watch(diagnosticRepositoryProvider).last;
    final opCtx = homeAsync.when(
      data: (h) => h.operationalContext,
      loading: () => null,
      error: (_, _) => null,
    );

    ref.listen<SellState>(sellControllerProvider, (_, next) {
      if (next is SellSuccess) {
        if (next.response.ticketCode.isEmpty) {
          context.pushReplacement('/pos/tickets/${next.response.ticketId}');
          return;
        }
        context.pushReplacement(
          '/pos/sell/success',
          extra: {
            'ticketId': next.response.ticketId,
            'ticketCode': next.response.ticketCode,
            'publicCode': next.response.publicCode,
            'shareableText': next.response.backup?.shareableText,
          },
        );
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Vendre un Ticket'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          tooltip: 'Annuler',
          onPressed: () => context.pop(),
        ),
      ),
      // Reactive capability gate: `ready` = server `canSell && no requiredStep`.
      // It auto-refreshes via the runtime rebootstrap, so a terminal suspended
      // mid-session is refused here without waiting for a 403 on confirm.
      // Only block when we positively know it cannot sell (null = still loading).
      body: (opCtx != null && !opCtx.ready)
          ? _ErrorBody(
              message: 'Ce terminal ne peut pas vendre pour le moment.',
              onRetry: () => ref.invalidate(cashierHomeProvider),
            )
          : switch (state) {
              SellLoadingCatalog() => const Center(
                child: CircularProgressIndicator(),
              ),
              SellCatalogError(:final errorKeys) => _ErrorBody(
                message: _translateError(translations, errorKeys),
                diagnostic: lastDiagnostic,
                onRetry: () =>
                    ref.read(sellControllerProvider.notifier).loadCatalog(),
              ),
              SellSuccess() => const Center(child: CircularProgressIndicator()),
              SellReady(:final form, :final previewResult, :final errorKeys) =>
                _SellBody(
                  form: form,
                  previewResult: previewResult,
                  isPreviewing: false,
                  isConfirming: false,
                  error: errorKeys.isEmpty
                      ? null
                      : _translateError(translations, errorKeys),
                  diagnostic: errorKeys.isNotEmpty ? lastDiagnostic : null,
                  opCtx: opCtx,
                  stakeController: _stakeController,
                  onSelectDraw: (id) =>
                      ref.read(sellControllerProvider.notifier).selectDraw(id),
                  onSelectGame: (g) =>
                      ref.read(sellControllerProvider.notifier).selectGame(g),
                  onSelectBetOption: (o) => ref
                      .read(sellControllerProvider.notifier)
                      .selectBetOption(o),
                  onSelectionChanged: (v) => ref
                      .read(sellControllerProvider.notifier)
                      .updateSelection(v),
                  onStakeChanged: (v) =>
                      ref.read(sellControllerProvider.notifier).updateStake(v),
                  onAddLine: () {
                    ref.read(sellControllerProvider.notifier).addLine();
                    _stakeController.clear();
                  },
                  onRemoveLine: (i) =>
                      ref.read(sellControllerProvider.notifier).removeLine(i),
                  onPreview: () {
                    if (opCtx?.sellerTerminalId == null) return;
                    ref.read(sellControllerProvider.notifier).prepare();
                  },
                  onConfirm: () {
                    if (opCtx?.sellerTerminalId == null) return;
                    ref.read(sellControllerProvider.notifier).confirmSell();
                  },
                ),
              SellPreviewing(:final form) => _SellBody(
                form: form,
                previewResult: null,
                isPreviewing: true,
                isConfirming: false,
                error: null,
                opCtx: opCtx,
                stakeController: _stakeController,
                onSelectDraw: (_) {},
                onSelectGame: (_) {},
                onSelectBetOption: (_) {},
                onSelectionChanged: (_) {},
                onStakeChanged: (_) {},
                onAddLine: () {},
                onRemoveLine: (_) {},
                onPreview: () {},
                onConfirm: () {},
              ),
              SellConfirming(:final form) => _SellBody(
                form: form,
                previewResult: null,
                isPreviewing: false,
                isConfirming: true,
                error: null,
                opCtx: opCtx,
                stakeController: _stakeController,
                onSelectDraw: (_) {},
                onSelectGame: (_) {},
                onSelectBetOption: (_) {},
                onSelectionChanged: (_) {},
                onStakeChanged: (_) {},
                onAddLine: () {},
                onRemoveLine: (_) {},
                onPreview: () {},
                onConfirm: () {
                  if (opCtx?.sellerTerminalId == null) return;
                  ref.read(sellControllerProvider.notifier).confirmSell();
                },
              ),
            },
    );
  }
}

String _translateError(I18nBundle bundle, List<String> keys) {
  for (final key in keys) {
    final translated = bundle.translate(key);
    if (translated != key) return translated;
  }
  return bundle.translate('common.error.unknown');
}

// ─── Sell body ────────────────────────────────────────────────────────────────

class _SellBody extends StatelessWidget {
  const _SellBody({
    required this.form,
    required this.isPreviewing,
    required this.isConfirming,
    required this.stakeController,
    required this.onSelectDraw,
    required this.onSelectGame,
    required this.onSelectBetOption,
    required this.onSelectionChanged,
    required this.onStakeChanged,
    required this.onPreview,
    required this.onConfirm,
    required this.onAddLine,
    required this.onRemoveLine,
    this.previewResult,
    this.error,
    this.diagnostic,
    this.opCtx,
  });

  final SellFormData form;
  final CashierTicketPreviewResponse? previewResult;
  final bool isPreviewing;
  final bool isConfirming;
  final String? error;
  final DiagnosticInfo? diagnostic;
  final CashierHomeOpCtx? opCtx;
  final TextEditingController stakeController;
  final ValueChanged<String> onSelectDraw;
  final ValueChanged<CashierGameOptionResponse> onSelectGame;
  final ValueChanged<int> onSelectBetOption;
  final ValueChanged<String> onSelectionChanged;
  final ValueChanged<double> onStakeChanged;
  final VoidCallback onPreview;
  final VoidCallback onConfirm;
  final VoidCallback onAddLine;
  final ValueChanged<int> onRemoveLine;

  bool get _isLoading => isPreviewing || isConfirming;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(TchSpacing.s16),
            children: [
              // ── Draw chips ──────────────────────────────────────────
              _Section(
                label: 'TIRAGE',
                child: form.draws.isEmpty
                    ? Text(
                        'Aucun tirage disponible',
                        style: textTheme.bodySmall?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      )
                    : _DrawPicker(
                        draws: form.draws,
                        selected: form.selectedDrawId,
                        enabled: !_isLoading,
                        onSelect: onSelectDraw,
                      ),
              ),

              // ── Game chips ──────────────────────────────────────────
              if (form.games.isNotEmpty)
                _Section(
                  label: 'JEU',
                  child: Wrap(
                    spacing: TchSpacing.s8,
                    runSpacing: TchSpacing.s8,
                    // Deduplicate: one chip per unique gameLabel.
                    // Lot variants (1er lot, 2ème lot…) share the same label;
                    // the payout engine handles lot attribution — sellers don't
                    // need to pre-select a lot.
                    children: () {
                      final seen = <String>{};
                      return form.games.where((g) => seen.add(g.gameLabel)).map(
                        (g) {
                          final selected =
                              form.selectedGameCode == g.gameCode &&
                              form.selectedBetType == g.betType;
                          return _Chip(
                            label: g.gameLabel,
                            selected: selected,
                            enabled: !_isLoading,
                            onTap: () => onSelectGame(g),
                          );
                        },
                      ).toList();
                    }(),
                  ),
                ),

              // ── Bet option chips ────────────────────────────────────
              if (form.selectedGame?.requiresOption == true &&
                  form.selectedGame!.options.isNotEmpty)
                _Section(
                  label: form.selectedGame!.betTypeLabel.toUpperCase(),
                  child: Wrap(
                    spacing: TchSpacing.s8,
                    runSpacing: TchSpacing.s8,
                    children: form.selectedGame!.options.map((o) {
                      final selected = form.selectedBetOption == o.code;
                      return _Chip(
                        label: o.label,
                        selected: selected,
                        enabled: !_isLoading,
                        onTap: () => onSelectBetOption(o.code),
                      );
                    }).toList(),
                  ),
                ),

              // ── Selection input ─────────────────────────────────────
              if (form.selectedGameCode != null)
                _Section(
                  label: 'NUMÉRO / SÉLECTION',
                  child: _SelectionInput(
                    key: ValueKey(
                      '${form.selectedGameCode}:${form.selectedBetType}:${form.selectedBetOption}',
                    ),
                    game: form.selectedGame!,
                    betOption: form.selectedBetOption,
                    value: form.selection,
                    enabled: !_isLoading,
                    onChanged: onSelectionChanged,
                  ),
                ),

              // ── Stake input ─────────────────────────────────────────
              if (form.selectedGameCode != null)
                _Section(
                  label: 'MISE',
                  child: TextField(
                    controller: stakeController,
                    enabled: !_isLoading,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[\d.,]')),
                    ],
                    onChanged: (v) => onStakeChanged(
                      double.tryParse(v.replaceAll(',', '.')) ?? 0,
                    ),
                    decoration: InputDecoration(
                      hintText: '0.00',
                      suffixText: form.currency,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(TchRadius.md),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: TchSpacing.s16,
                        vertical: TchSpacing.s12,
                      ),
                    ),
                  ),
                ),

              // ── Committed lines list ────────────────────────────────
              if (form.committedLines.isNotEmpty) ...[
                const SizedBox(height: TchSpacing.s8),
                _Section(
                  label: 'LIGNES DU TICKET (${form.committedLines.length})',
                  child: Column(
                    children: [
                      for (var i = 0; i < form.committedLines.length; i++)
                        Padding(
                          padding: const EdgeInsets.only(bottom: TchSpacing.s8),
                          child: Row(
                            children: [
                              Expanded(
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: TchSpacing.s12,
                                    vertical: TchSpacing.s8,
                                  ),
                                  decoration: BoxDecoration(
                                    color: scheme.surfaceContainerHighest,
                                    borderRadius: BorderRadius.circular(
                                      TchRadius.md,
                                    ),
                                  ),
                                  child: Text(
                                    '#${i + 1}  ${form.committedLines[i].displayLabel}',
                                    style: textTheme.bodySmall?.copyWith(
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ),
                              ),
                              if (!_isLoading) ...[
                                const SizedBox(width: TchSpacing.s4),
                                IconButton(
                                  icon: const Icon(
                                    Icons.close_rounded,
                                    size: 18,
                                  ),
                                  onPressed: () => onRemoveLine(i),
                                  tooltip: 'Supprimer la ligne',
                                  padding: EdgeInsets.zero,
                                  constraints: const BoxConstraints(
                                    minWidth: 32,
                                    minHeight: 32,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
              ],

              // ── Add line button — always visible once a game is selected,
              //    enabled only when the current entry (number + stake) is valid.
              if (form.selectedGameCode != null && !_isLoading) ...[
                const SizedBox(height: TchSpacing.s4),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: form.canAddLine ? onAddLine : null,
                    icon: const Icon(Icons.add_rounded, size: 18),
                    label: Text(
                      form.canAddLine
                          ? 'Ajouter une ligne'
                          : 'Entrez un numéro et une mise',
                    ),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                        vertical: TchSpacing.s12,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(TchRadius.md),
                      ),
                    ),
                  ),
                ),
              ],

              // ── Preview result ──────────────────────────────────────
              if (previewResult != null) ...[
                const SizedBox(height: TchSpacing.s8),
                _PreviewCard(result: previewResult!),
              ],

              // ── Error ───────────────────────────────────────────────
              if (error != null) ...[
                const SizedBox(height: TchSpacing.s8),
                Container(
                  padding: const EdgeInsets.all(TchSpacing.s12),
                  decoration: BoxDecoration(
                    color: scheme.errorContainer,
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.error_outline_rounded,
                            size: 18,
                            color: scheme.onErrorContainer,
                          ),
                          const SizedBox(width: TchSpacing.s8),
                          Expanded(
                            child: Text(
                              error!,
                              style: textTheme.bodySmall?.copyWith(
                                color: scheme.onErrorContainer,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (diagnostic != null && diagnostic!.hasAny) ...[
                        const SizedBox(height: TchSpacing.s8),
                        _CopyDiagnosticButton(diagnostic: diagnostic!),
                      ],
                    ],
                  ),
                ),
              ],

              // Extra bottom padding so the add-line button clears the sticky CTA.
              const SizedBox(height: 88),
            ],
          ),
        ),

        // ── Bottom actions ─────────────────────────────────────────────
        _BottomActions(
          form: form,
          previewResult: previewResult,
          isPreviewing: isPreviewing,
          isConfirming: isConfirming,
          onPreview: onPreview,
          onConfirm: onConfirm,
        ),
      ],
    );
  }
}

// ─── Preview card ─────────────────────────────────────────────────────────────

class _PreviewCard extends ConsumerWidget {
  const _PreviewCard({required this.result});

  final CashierTicketPreviewResponse result;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final scheme = Theme.of(context).colorScheme;
    final accepted = result.isAccepted;
    final bgColor = accepted
        ? TchColors.successContainer
        : scheme.errorContainer;
    final fgColor = accepted ? TchColors.success : scheme.onErrorContainer;
    final icon = accepted
        ? Icons.check_circle_outline_rounded
        : Icons.cancel_outlined;

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(
          color: accepted
              ? TchColors.successContainer.withValues(alpha: 0.5)
              : scheme.error.withValues(alpha: 0.3),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: fgColor, size: 20),
              const SizedBox(width: TchSpacing.s8),
              Text(
                accepted ? 'Vente acceptée' : 'Vente refusée',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: fgColor,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          if (result.totalAmount != null) ...[
            const SizedBox(height: TchSpacing.s8),
            Text(
              _preparedTotalLabel(translations, result),
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: fgColor),
            ),
          ],
          if (result.promotionLines.isNotEmpty) ...[
            const SizedBox(height: TchSpacing.s12),
            for (final line in result.promotionLines)
              _PromotionLine(line: line, color: fgColor),
          ],
          if (result.notices.isNotEmpty) ...[
            const SizedBox(height: TchSpacing.s12),
            for (final notice in result.notices)
              Text(
                _noticeLabel(translations, notice),
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: fgColor),
              ),
          ],
          if (result.issues.isNotEmpty) ...[
            const SizedBox(height: TchSpacing.s8),
            for (final _ in result.issues)
              Text(
                _genericNoticeLabel(translations),
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: fgColor),
              ),
          ],
        ],
      ),
    );
  }
}

String _preparedTotalLabel(
  I18nBundle translations,
  CashierTicketPreviewResponse result,
) =>
    '${translations.translate(CashierPreparationCopy.totalKey)}: '
    '${result.totalAmount!.toStringAsFixed(2)} ${result.currency ?? ''}';

String _noticeLabel(I18nBundle translations, CashierPreparationNotice notice) =>
    '• ${translations.translate(notice.translationKey)}';

String _genericNoticeLabel(I18nBundle translations) =>
    '• ${translations.translate(CashierPreparationCopy.genericNoticeKey)}';

class _PromotionLine extends ConsumerWidget {
  const _PromotionLine({required this.line, required this.color});

  final CashierPreparationPromotionLine line;
  final Color color;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final label = line.isMaryaj
        ? translations.translate(CashierPreparationCopy.maryajFreeKey)
        : translations.translate(CashierPreparationCopy.freeGameKey);
    final source = line.isGenerated
        ? translations.translate(CashierPreparationCopy.autoSelectionKey)
        : translations.translate(CashierPreparationCopy.sellerSelectionKey);
    return Padding(
      padding: const EdgeInsets.only(bottom: TchSpacing.s4),
      child: Text(
        _promotionLabel(translations, line, label, source),
        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: color),
      ),
    );
  }
}

String _promotionLabel(
  I18nBundle translations,
  CashierPreparationPromotionLine line,
  String label,
  String source,
) =>
    '$label: ${line.selection} — '
    '${translations.translate(CashierPreparationCopy.freeKey)} ($source)';

// ─── Bottom actions ───────────────────────────────────────────────────────────

class _BottomActions extends StatelessWidget {
  const _BottomActions({
    required this.form,
    required this.isPreviewing,
    required this.isConfirming,
    required this.onPreview,
    required this.onConfirm,
    this.previewResult,
  });

  final SellFormData form;
  final CashierTicketPreviewResponse? previewResult;
  final bool isPreviewing;
  final bool isConfirming;
  final VoidCallback onPreview;
  final VoidCallback onConfirm;

  @override
  Widget build(BuildContext context) {
    final canPreview = form.canPreview && !isPreviewing && !isConfirming;
    final canConfirm = previewResult?.isAccepted == true && !isConfirming;
    final showConfirm = previewResult?.isAccepted == true;

    return Container(
      padding: const EdgeInsets.fromLTRB(
        TchSpacing.s16,
        TchSpacing.s8,
        TchSpacing.s16,
        TchSpacing.s24,
      ),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          top: BorderSide(color: Theme.of(context).colorScheme.outlineVariant),
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (showConfirm) ...[
            SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton.icon(
                onPressed: canConfirm ? onConfirm : null,
                icon: isConfirming
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: TchColors.onPrimary,
                        ),
                      )
                    : const Icon(Icons.sell_rounded),
                label: Text(
                  isConfirming ? 'VENTE EN COURS…' : 'CONFIRMER LA VENTE',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                  ),
                ),
                style: FilledButton.styleFrom(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                ),
              ),
            ),
            const SizedBox(height: TchSpacing.s8),
          ],
          SizedBox(
            width: double.infinity,
            height: 50,
            child: OutlinedButton.icon(
              onPressed: canPreview ? onPreview : null,
              icon: isPreviewing
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.preview_rounded),
              label: Text(
                isPreviewing ? 'VÉRIFICATION…' : 'APERÇU',
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(TchRadius.md),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Section ──────────────────────────────────────────────────────────────────

class _Section extends StatelessWidget {
  const _Section({required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: TchSpacing.s20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                label,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                  letterSpacing: 0.5,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s8),
          child,
        ],
      ),
    );
  }
}

// ─── Draw picker ──────────────────────────────────────────────────────────────

class _DrawPicker extends ConsumerWidget {
  const _DrawPicker({
    required this.draws,
    required this.selected,
    required this.enabled,
    required this.onSelect,
  });

  final List<CashierAvailableDrawView> draws;
  final String? selected;
  final bool enabled;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (int i = 0; i < draws.length; i++) ...[
            if (i > 0) const SizedBox(width: TchSpacing.s8),
            _DrawChip(
              draw: draws[i],
              localLabel: translations.translate('pos.sale.draw.local_time'),
              providerLabel: translations.translate(
                'pos.sale.draw.provider_time',
              ),
              selected: draws[i].drawId == selected,
              enabled: enabled,
              onTap: () => onSelect(draws[i].drawId),
            ),
          ],
        ],
      ),
    );
  }
}

class _DrawChip extends StatelessWidget {
  const _DrawChip({
    required this.draw,
    required this.localLabel,
    required this.providerLabel,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  final CashierAvailableDrawView draw;
  final String localLabel;
  final String providerLabel;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: enabled ? onTap : null,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s16,
          vertical: TchSpacing.s8,
        ),
        decoration: BoxDecoration(
          color: selected ? scheme.primary : scheme.surfaceContainerLow,
          borderRadius: BorderRadius.circular(TchRadius.pill),
          border: Border.all(
            color: selected ? scheme.primary : scheme.outlineVariant,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              draw.channelLabel,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: selected ? scheme.onPrimary : scheme.onSurface,
                fontWeight: FontWeight.w700,
              ),
            ),
            if (draw.localScheduleLabel.isNotEmpty) ...[
              const SizedBox(height: TchSpacing.s4),
              _DrawTimeLine(
                label: localLabel,
                value: draw.localScheduleLabel,
                color: selected
                    ? scheme.onPrimary.withValues(alpha: 0.84)
                    : scheme.onSurfaceVariant,
              ),
            ],
            if (draw.providerScheduleLabel.isNotEmpty) ...[
              const SizedBox(height: TchSpacing.s4),
              _DrawTimeLine(
                label: providerLabel,
                value: draw.providerScheduleLabel,
                color: selected
                    ? scheme.onPrimary.withValues(alpha: 0.72)
                    : scheme.outline,
              ),
            ],
            if (draw.formattedCutoff.isNotEmpty) ...[
              const SizedBox(height: TchSpacing.s4),
              Text(
                draw.formattedCutoff,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: selected
                      ? scheme.onPrimary.withValues(alpha: 0.8)
                      : scheme.outline,
                  fontSize: 10,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _DrawTimeLine extends StatelessWidget {
  const _DrawTimeLine({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) => Text.rich(
    TextSpan(
      children: [
        TextSpan(text: label),
        const TextSpan(text: ': '),
        TextSpan(text: value),
      ],
    ),
    style: Theme.of(
      context,
    ).textTheme.labelSmall?.copyWith(color: color, fontSize: 10),
  );
}

// ─── Structured number entry ─────────────────────────────────────────────────

class _SelectionInput extends StatelessWidget {
  const _SelectionInput({
    super.key,
    required this.game,
    required this.betOption,
    required this.value,
    required this.enabled,
    required this.onChanged,
  });

  final CashierGameOptionResponse game;
  final int? betOption;
  final String value;
  final bool enabled;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final shape = game.selectionShapeFor(betOption);
    return _DigitSelectionInput(
      shape: shape,
      value: value,
      enabled: enabled,
      onChanged: onChanged,
    );
  }
}

class _DigitSelectionInput extends StatefulWidget {
  const _DigitSelectionInput({
    required this.shape,
    required this.value,
    required this.enabled,
    required this.onChanged,
  });

  final CashierSelectionShape shape;
  final String value;
  final bool enabled;
  final ValueChanged<String> onChanged;

  @override
  State<_DigitSelectionInput> createState() => _DigitSelectionInputState();
}

class _DigitSelectionInputState extends State<_DigitSelectionInput> {
  late List<TextEditingController> _controllers;
  late List<FocusNode> _focusNodes;

  int get _fieldCount => widget.shape.digits * widget.shape.segments;

  @override
  void initState() {
    super.initState();
    _resetControllers();
  }

  @override
  void didUpdateWidget(covariant _DigitSelectionInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.shape.digits != widget.shape.digits ||
        oldWidget.shape.segments != widget.shape.segments) {
      _disposeControllers();
      _resetControllers();
      return;
    }
    if (oldWidget.value != widget.value && _value() != widget.value) {
      _applyValue(widget.value);
    }
  }

  void _resetControllers() {
    _controllers = List.generate(_fieldCount, (_) => TextEditingController());
    _focusNodes = List.generate(_fieldCount, (_) => FocusNode());
    _applyValue(widget.value);
  }

  void _applyValue(String value) {
    final digits = value.replaceAll(RegExp(r'\D'), '');
    for (var index = 0; index < _controllers.length; index++) {
      _controllers[index].text = index < digits.length ? digits[index] : '';
    }
  }

  String _value() {
    final groups = <String>[];
    for (var segment = 0; segment < widget.shape.segments; segment++) {
      final start = segment * widget.shape.digits;
      groups.add(
        _controllers
            .skip(start)
            .take(widget.shape.digits)
            .map((controller) => controller.text)
            .join(),
      );
    }
    if (groups.any((group) => group.length != widget.shape.digits)) return '';
    return groups.join('-');
  }

  void _changed(int index, String next) {
    final digit = next.replaceAll(RegExp(r'\D'), '');
    if (digit.length > 1) {
      _applyValue(digit);
    } else {
      _controllers[index].text = digit;
      _controllers[index].selection = TextSelection.collapsed(
        offset: digit.length,
      );
    }
    if (digit.isNotEmpty && index < _focusNodes.length - 1) {
      _focusNodes[index + 1].requestFocus();
    }
    widget.onChanged(_value());
  }

  @override
  void dispose() {
    _disposeControllers();
    super.dispose();
  }

  void _disposeControllers() {
    for (final controller in _controllers) {
      controller.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Wrap(
      spacing: TchSpacing.s8,
      runSpacing: TchSpacing.s8,
      children: [
        for (var segment = 0; segment < widget.shape.segments; segment++) ...[
          if (segment > 0)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s4),
              child: Text(
                String.fromCharCode(0x00d7),
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          _DigitGroup(
            start: segment * widget.shape.digits,
            digits: widget.shape.digits,
            controllers: _controllers,
            focusNodes: _focusNodes,
            enabled: widget.enabled,
            onChanged: _changed,
          ),
        ],
      ],
    );
  }
}

class _DigitGroup extends StatelessWidget {
  const _DigitGroup({
    required this.start,
    required this.digits,
    required this.controllers,
    required this.focusNodes,
    required this.enabled,
    required this.onChanged,
  });

  final int start;
  final int digits;
  final List<TextEditingController> controllers;
  final List<FocusNode> focusNodes;
  final bool enabled;
  final void Function(int, String) onChanged;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      for (var offset = 0; offset < digits; offset++) ...[
        if (offset > 0) const SizedBox(width: TchSpacing.s4),
        SizedBox(
          width: 48,
          child: TextField(
            controller: controllers[start + offset],
            focusNode: focusNodes[start + offset],
            enabled: enabled,
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            maxLength: 1,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            onChanged: (value) => onChanged(start + offset, value),
            decoration: InputDecoration(
              counterText: '',
              contentPadding: const EdgeInsets.symmetric(
                vertical: TchSpacing.s12,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
            ),
          ),
        ),
      ],
    ],
  );
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.label,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: enabled ? onTap : null,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s16,
          vertical: TchSpacing.s8,
        ),
        decoration: BoxDecoration(
          color: selected
              ? scheme.primaryContainer
              : scheme.surfaceContainerLow,
          borderRadius: BorderRadius.circular(TchRadius.pill),
          border: Border.all(
            color: selected ? scheme.primary : scheme.outlineVariant,
          ),
        ),
        child: Text(
          label,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: selected ? scheme.onPrimaryContainer : scheme.onSurface,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}

// ─── Error body ───────────────────────────────────────────────────────────────

class _ErrorBody extends StatelessWidget {
  const _ErrorBody({
    required this.message,
    required this.onRetry,
    this.diagnostic,
  });

  final String message;
  final VoidCallback onRetry;
  final DiagnosticInfo? diagnostic;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(TchSpacing.s24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
            const SizedBox(height: TchSpacing.s16),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: TchSpacing.s24),
            FilledButton.tonal(
              onPressed: onRetry,
              child: const Text('Réessayer'),
            ),
            if (diagnostic != null && diagnostic!.hasAny) ...[
              const SizedBox(height: TchSpacing.s12),
              _CopyDiagnosticButton(diagnostic: diagnostic!),
            ],
          ],
        ),
      ),
    );
  }
}

// ─── Copy diagnostic ──────────────────────────────────────────────────────────

class _CopyDiagnosticButton extends StatefulWidget {
  const _CopyDiagnosticButton({required this.diagnostic});

  final DiagnosticInfo diagnostic;

  @override
  State<_CopyDiagnosticButton> createState() => _CopyDiagnosticButtonState();
}

class _CopyDiagnosticButtonState extends State<_CopyDiagnosticButton> {
  bool _copied = false;

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
      onPressed: _copy,
      icon: Icon(
        _copied ? Icons.check_rounded : Icons.content_copy_rounded,
        size: 16,
      ),
      label: Text(_copied ? 'Copié' : 'Copier diagnostic'),
      style: TextButton.styleFrom(visualDensity: VisualDensity.compact),
    );
  }

  Future<void> _copy() async {
    await Clipboard.setData(
      ClipboardData(text: widget.diagnostic.toCopyText()),
    );
    if (!mounted) return;
    setState(() => _copied = true);
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted) setState(() => _copied = false);
    });
  }
}
