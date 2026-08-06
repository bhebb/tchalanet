import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/observability/diagnostic_info.dart';
import '../../../../../core/observability/diagnostic_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/layout/screen_size.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../home/data/models/cashier_home_models.dart';
import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../../data/models/cashier_sell_catalog_models.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../cashier_draw_label.dart';
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
  final _stakeFocusNode = FocusNode();

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
    _stakeFocusNode.dispose();
    _stakeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(sellControllerProvider);
    final translations = ref.watch(i18nBundleProvider);
    final homeAsync = ref.watch(cashierHomeProvider);
    final lastDiagnostic = ref.watch(diagnosticRepositoryProvider).last;
    final keyboardInset = MediaQuery.viewInsetsOf(context).bottom;
    final opCtx = homeAsync.when(
      data: (h) => h.operationalContext,
      loading: () => null,
      error: (_, _) => null,
    );

    ref.listen<SellState>(sellControllerProvider, (_, next) {
      if (next is SellSuccess) {
        ref.invalidate(cashierHomeProvider);
        ref.invalidate(terminalDailyStatsProvider);
        ref.invalidate(terminalStatsByDateProvider);
        ref.invalidate(latestTicketProvider);
        final ticketId = next.response.ticketId;
        if (ticketId == null) return;
        if (next.response.ticketCode.isEmpty) {
          context.pushReplacement('/pos/tickets/$ticketId');
          return;
        }
        context.pushReplacement(
          '/pos/sell/success',
          extra: {
            'ticketId': ticketId,
            'ticketCode': next.response.ticketCode,
            'publicCode': next.response.publicCode,
            'shareableText': next.response.backup?.shareableText,
          },
        );
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.sale.title')),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          tooltip: translations.translate('pos.sale.cancel'),
          onPressed: () => context.pop(),
        ),
      ),
      // Reactive capability gate: `ready` = server `canSell && no requiredStep`.
      // It auto-refreshes via the runtime rebootstrap, so a terminal suspended
      // mid-session is refused here without waiting for a 403 on confirm.
      // Only block when we positively know it cannot sell (null = still loading).
      body: (opCtx != null && !opCtx.ready)
          ? _ErrorBody(
              message: translations.translate('pos.sale.terminal_unavailable'),
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
                  keyboardInset: keyboardInset,
                  error: errorKeys.isEmpty
                      ? null
                      : _translateError(translations, errorKeys),
                  diagnostic: errorKeys.isNotEmpty ? lastDiagnostic : null,
                  opCtx: opCtx,
                  stakeController: _stakeController,
                  stakeFocusNode: _stakeFocusNode,
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
                  onCancelEntry: () {
                    FocusManager.instance.primaryFocus?.unfocus();
                    _stakeController.clear();
                    ref
                        .read(sellControllerProvider.notifier)
                        .cancelCurrentEntry();
                  },
                  onEditPreparedTicket: () => ref
                      .read(sellControllerProvider.notifier)
                      .editPreparedTicket(),
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
                keyboardInset: keyboardInset,
                error: null,
                opCtx: opCtx,
                stakeController: _stakeController,
                stakeFocusNode: _stakeFocusNode,
                onSelectDraw: (_) {},
                onSelectGame: (_) {},
                onSelectBetOption: (_) {},
                onSelectionChanged: (_) {},
                onStakeChanged: (_) {},
                onAddLine: () {},
                onCancelEntry: () {},
                onEditPreparedTicket: () {},
                onRemoveLine: (_) {},
                onPreview: () {},
                onConfirm: () {},
              ),
              SellConfirming(:final form, :final preview) => _SellBody(
                form: form,
                previewResult: preview,
                isPreviewing: false,
                isConfirming: true,
                keyboardInset: keyboardInset,
                error: null,
                opCtx: opCtx,
                stakeController: _stakeController,
                stakeFocusNode: _stakeFocusNode,
                onSelectDraw: (_) {},
                onSelectGame: (_) {},
                onSelectBetOption: (_) {},
                onSelectionChanged: (_) {},
                onStakeChanged: (_) {},
                onAddLine: () {},
                onCancelEntry: () {},
                onEditPreparedTicket: () {},
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

class _SellBody extends ConsumerStatefulWidget {
  const _SellBody({
    required this.form,
    required this.isPreviewing,
    required this.isConfirming,
    required this.keyboardInset,
    required this.stakeController,
    required this.stakeFocusNode,
    required this.onSelectDraw,
    required this.onSelectGame,
    required this.onSelectBetOption,
    required this.onSelectionChanged,
    required this.onStakeChanged,
    required this.onPreview,
    required this.onConfirm,
    required this.onAddLine,
    required this.onCancelEntry,
    required this.onEditPreparedTicket,
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
  final double keyboardInset;
  final String? error;
  final DiagnosticInfo? diagnostic;
  final CashierHomeOpCtx? opCtx;
  final TextEditingController stakeController;
  final FocusNode stakeFocusNode;
  final ValueChanged<String> onSelectDraw;
  final ValueChanged<CashierGameOptionResponse> onSelectGame;
  final ValueChanged<int> onSelectBetOption;
  final ValueChanged<String> onSelectionChanged;
  final ValueChanged<double> onStakeChanged;
  final VoidCallback onPreview;
  final VoidCallback onConfirm;
  final VoidCallback onAddLine;
  final VoidCallback onCancelEntry;
  final VoidCallback onEditPreparedTicket;
  final ValueChanged<int> onRemoveLine;

  @override
  ConsumerState<_SellBody> createState() => _SellBodyState();
}

class _SellBodyState extends ConsumerState<_SellBody> {
  final _scrollController = ScrollController();
  final _selectionFieldKey = GlobalKey();
  final _stakeFieldKey = GlobalKey();

  bool get _isLoading => widget.isPreviewing || widget.isConfirming;
  bool get _isTicketLocked => widget.previewResult != null;
  bool get _hasEntryInProgress =>
      widget.form.selection.trim().isNotEmpty || widget.form.stake > 0;

  @override
  void initState() {
    super.initState();
    widget.stakeFocusNode.addListener(_handleStakeFocus);
  }

  @override
  void didUpdateWidget(covariant _SellBody oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.stakeFocusNode != widget.stakeFocusNode) {
      oldWidget.stakeFocusNode.removeListener(_handleStakeFocus);
      widget.stakeFocusNode.addListener(_handleStakeFocus);
    }
  }

  @override
  void dispose() {
    widget.stakeFocusNode.removeListener(_handleStakeFocus);
    _scrollController.dispose();
    super.dispose();
  }

  void _handleStakeFocus() {
    if (widget.stakeFocusNode.hasFocus) {
      _scrollFocusedFieldIntoView(_stakeFieldKey);
    }
  }

  void _scrollFocusedFieldIntoView(GlobalKey key) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final context = key.currentContext;
      if (context == null || !mounted) return;
      Scrollable.ensureVisible(
        context,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOutCubic,
        alignment: 0.68,
        alignmentPolicy: ScrollPositionAlignmentPolicy.keepVisibleAtEnd,
      );
    });
  }

  void _addLine() {
    widget.onAddLine();
    FocusManager.instance.primaryFocus?.unfocus();
  }

  void _cancelEntry() {
    widget.onCancelEntry();
    FocusManager.instance.primaryFocus?.unfocus();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final translations = ref.watch(i18nBundleProvider);
    final isCompactEntry = widget.keyboardInset > 0 && !_isTicketLocked;
    final committedTotal = widget.form.committedLines.fold<double>(
      0,
      (total, line) => total + line.stake,
    );

    return Column(
      children: [
        Expanded(
          child: ListView(
            controller: _scrollController,
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            padding: EdgeInsets.fromLTRB(
              TchSpacing.s16,
              TchSpacing.s16,
              TchSpacing.s16,
              isCompactEntry ? TchSpacing.s16 : 164,
            ),
            children: [
              _Section(
                label: translations.translate('pos.sale.draw_label'),
                child: widget.form.draws.isEmpty
                    ? Text(
                        translations.translate('pos.sale.no_draws'),
                        style: textTheme.bodySmall?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      )
                    : _SelectedDraw(
                        draws: widget.form.draws,
                        selected: widget.form.selectedDrawId,
                        enabled: !_isLoading && !_isTicketLocked,
                        compact: isCompactEntry,
                        onSelect: widget.onSelectDraw,
                      ),
              ),

              if (widget.form.games.isNotEmpty)
                _Section(
                  label: translations.translate('pos.sale.game_label'),
                  child: Wrap(
                    spacing: TchSpacing.s8,
                    runSpacing: TchSpacing.s8,
                    // Deduplicate: one chip per unique gameLabel.
                    // Lot variants (1er lot, 2ème lot…) share the same label;
                    // the payout engine handles lot attribution — sellers don't
                    // need to pre-select a lot.
                    children: () {
                      final seen = <String>{};
                      return widget.form.games
                          .where((g) => seen.add(g.gameLabel))
                          .map((g) {
                            final selected =
                                widget.form.selectedGameCode == g.gameCode &&
                                widget.form.selectedBetType == g.betType;
                            return _Chip(
                              label: g.gameLabel,
                              selected: selected,
                              enabled: !_isLoading && !_isTicketLocked,
                              onTap: () => widget.onSelectGame(g),
                            );
                          })
                          .toList();
                    }(),
                  ),
                ),

              if (widget.form.selectedGame?.requiresOption == true &&
                  widget.form.selectedGame!.options.isNotEmpty)
                _Section(
                  label: widget.form.selectedGame!.betTypeLabel.toUpperCase(),
                  child: Wrap(
                    spacing: TchSpacing.s8,
                    runSpacing: TchSpacing.s8,
                    children: widget.form.selectedGame!.options.map((o) {
                      final selected = widget.form.selectedBetOption == o.code;
                      return _Chip(
                        label: o.label,
                        selected: selected,
                        enabled: !_isLoading && !_isTicketLocked,
                        onTap: () => widget.onSelectBetOption(o.code),
                      );
                    }).toList(),
                  ),
                ),

              if (widget.form.selectedGameCode != null)
                _Section(
                  key: _selectionFieldKey,
                  label: translations.translate('pos.sale.selection_label'),
                  child: _SelectionInput(
                    key: ValueKey(
                      '${widget.form.selectedGameCode}:${widget.form.selectedBetType}:${widget.form.selectedBetOption}',
                    ),
                    game: widget.form.selectedGame!,
                    betOption: widget.form.selectedBetOption,
                    value: widget.form.selection,
                    enabled: !_isLoading && !_isTicketLocked,
                    onFocus: () =>
                        _scrollFocusedFieldIntoView(_selectionFieldKey),
                    onChanged: widget.onSelectionChanged,
                  ),
                ),

              if (widget.form.selectedGameCode != null)
                _Section(
                  key: _stakeFieldKey,
                  label: translations.translate('pos.sale.stake_label'),
                  child: TextField(
                    controller: widget.stakeController,
                    focusNode: widget.stakeFocusNode,
                    enabled: !_isLoading && !_isTicketLocked,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[\d.,]')),
                    ],
                    onChanged: (v) => widget.onStakeChanged(
                      double.tryParse(v.replaceAll(',', '.')) ?? 0,
                    ),
                    decoration: InputDecoration(
                      hintText: '0.00',
                      suffixText: widget.form.currency,
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

              if (isCompactEntry && widget.form.committedLines.isNotEmpty)
                _Section(
                  label: translations.translate('pos.sale.ticket_label'),
                  child: _LastTicketLine(
                    line: widget.form.committedLines.last,
                    currency: widget.form.currency,
                  ),
                )
              else if (!isCompactEntry && widget.form.committedLines.isNotEmpty)
                _Section(
                  label: translations.translate('pos.sale.ticket_label'),
                  child: _TicketReceipt(
                    lines: widget.form.committedLines,
                    currency: widget.form.currency,
                    enabled: !_isLoading,
                    onRemoveLine: widget.onRemoveLine,
                  ),
                ),

              if (!isCompactEntry && widget.previewResult != null) ...[
                _PreviewCard(result: widget.previewResult!),
              ],

              if (widget.error != null) ...[
                const SizedBox(height: TchSpacing.s8),
                Container(
                  padding: const EdgeInsets.all(TchSpacing.s12),
                  decoration: BoxDecoration(
                    color: scheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(TchRadius.md),
                    border: Border.all(color: scheme.outlineVariant),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.error_outline_rounded,
                            size: 18,
                            color: scheme.error,
                          ),
                          const SizedBox(width: TchSpacing.s8),
                          Expanded(
                            child: Text(
                              widget.error!,
                              style: textTheme.bodySmall?.copyWith(
                                color: scheme.onSurface,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (widget.diagnostic != null &&
                          widget.diagnostic!.hasAny) ...[
                        const SizedBox(height: TchSpacing.s8),
                        _CopyDiagnosticButton(diagnostic: widget.diagnostic!),
                      ],
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),

        _BottomActions(
          form: widget.form,
          previewResult: widget.previewResult,
          isPreviewing: widget.isPreviewing,
          isConfirming: widget.isConfirming,
          total: widget.previewResult?.totalAmount ?? committedTotal,
          translations: translations,
          canAddLine: widget.form.canAddLine,
          compactEntryMode: isCompactEntry,
          hasEntryInProgress: _hasEntryInProgress,
          onAddLine: _addLine,
          onCancelEntry: _cancelEntry,
          onEditPreparedTicket: widget.onEditPreparedTicket,
          onPreview: widget.onPreview,
          onConfirm: widget.onConfirm,
        ),
      ],
    );
  }
}

// ─── Ticket receipt ───────────────────────────────────────────────────────────

class _TicketReceipt extends ConsumerWidget {
  const _TicketReceipt({
    required this.lines,
    required this.currency,
    required this.enabled,
    required this.onRemoveLine,
  });

  final List<SellLine> lines;
  final String currency;
  final bool enabled;
  final ValueChanged<int> onRemoveLine;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final translations = ref.watch(i18nBundleProvider);
    final total = lines.fold<double>(0, (sum, line) => sum + line.stake);
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Column(
        children: [
          for (var index = 0; index < lines.length; index++) ...[
            if (index > 0) const Divider(height: TchSpacing.s20),
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '#${index + 1} ${lines[index].gameLabel}',
                        style: Theme.of(context).textTheme.labelMedium
                            ?.copyWith(
                              color: scheme.onSurfaceVariant,
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                      const SizedBox(height: TchSpacing.s4),
                      Text(
                        lines[index].selection,
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(fontWeight: FontWeight.w800),
                      ),
                    ],
                  ),
                ),
                Text(
                  '${lines[index].stake.toStringAsFixed(2)} $currency',
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
                if (enabled) ...[
                  const SizedBox(width: TchSpacing.s4),
                  IconButton(
                    onPressed: () => onRemoveLine(index),
                    icon: const Icon(Icons.delete_outline_rounded),
                    tooltip: translations.translate('pos.sale.remove_line'),
                  ),
                ],
              ],
            ),
          ],
          const Padding(
            padding: EdgeInsets.symmetric(vertical: TchSpacing.s12),
            child: Divider(height: 1),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                translations.translate('pos.sale.ticket_total'),
                style: Theme.of(
                  context,
                ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800),
              ),
              Text(
                '${total.toStringAsFixed(2)} $currency',
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _LastTicketLine extends StatelessWidget {
  const _LastTicketLine({required this.line, required this.currency});

  final SellLine line;
  final String currency;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      constraints: const BoxConstraints(minHeight: 56),
      padding: const EdgeInsets.all(TchSpacing.s12),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Row(
        children: [
          const Icon(Icons.receipt_long_outlined, size: 20),
          const SizedBox(width: TchSpacing.s8),
          Expanded(
            child: Text(
              line.displayLabel,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(width: TchSpacing.s8),
          Text(
            currency,
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: scheme.onSurfaceVariant,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Preparation feedback ────────────────────────────────────────────────────

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
                accepted
                    ? translations.translate('pos.sale.sale_checked')
                    : translations.translate('pos.sale.sale_rejected'),
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
    required this.total,
    required this.translations,
    required this.canAddLine,
    required this.isPreviewing,
    required this.isConfirming,
    required this.compactEntryMode,
    required this.hasEntryInProgress,
    required this.onAddLine,
    required this.onCancelEntry,
    required this.onEditPreparedTicket,
    required this.onPreview,
    required this.onConfirm,
    this.previewResult,
  });

  final SellFormData form;
  final double total;
  final I18nBundle translations;
  final bool canAddLine;
  final CashierTicketPreviewResponse? previewResult;
  final bool isPreviewing;
  final bool isConfirming;
  final bool compactEntryMode;
  final bool hasEntryInProgress;
  final VoidCallback onAddLine;
  final VoidCallback onCancelEntry;
  final VoidCallback onEditPreparedTicket;
  final VoidCallback onPreview;
  final VoidCallback onConfirm;

  @override
  Widget build(BuildContext context) {
    final canPrepare =
        form.committedLines.isNotEmpty &&
        !hasEntryInProgress &&
        !isPreviewing &&
        !isConfirming;
    final canConfirm = previewResult?.isAccepted == true && !isConfirming;
    final showConfirm = previewResult?.isAccepted == true;
    final verticalPadding = compactEntryMode ? TchSpacing.s8 : TchSpacing.s16;

    return Container(
      padding: EdgeInsets.fromLTRB(
        TchSpacing.s16,
        TchSpacing.s8,
        TchSpacing.s16,
        verticalPadding,
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
          if (!compactEntryMode) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // Both texts were unflexed, so "Total pou peye" + the amount
                // overflowed by 23 px at 360 dp. The label yields; the amount
                // must never be truncated.
                Flexible(
                  child: Text(
                    translations.translate(CashierPreparationCopy.totalKey),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Text(
                  '${total.toStringAsFixed(2)} ${form.currency}',
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            const SizedBox(height: TchSpacing.s8),
          ] else if (form.committedLines.isNotEmpty) ...[
            _LastTicketLine(
              line: form.committedLines.last,
              currency: form.currency,
            ),
            const SizedBox(height: TchSpacing.s8),
          ],
          if (compactEntryMode)
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: context.minTouchTarget,
                    child: FilledButton.tonalIcon(
                      onPressed: canAddLine && !isPreviewing && !isConfirming
                          ? onAddLine
                          : null,
                      icon: const Icon(Icons.add_rounded),
                      label: Text(translations.translate('pos.sale.add_line')),
                    ),
                  ),
                ),
                const SizedBox(width: TchSpacing.s8),
                Expanded(
                  child: SizedBox(
                    height: context.minTouchTarget,
                    child: OutlinedButton.icon(
                      onPressed: isPreviewing || isConfirming
                          ? null
                          : onCancelEntry,
                      icon: const Icon(Icons.close_rounded),
                      label: Text(translations.translate('pos.sale.cancel')),
                    ),
                  ),
                ),
              ],
            )
          else if (showConfirm)
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: context.minTouchTarget,
                    child: OutlinedButton(
                      onPressed: isConfirming ? null : onEditPreparedTicket,
                      child: Text(
                        translations.translate('pos.sale.back_to_ticket'),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: TchSpacing.s8),
                Expanded(
                  flex: 2,
                  child: SizedBox(
                    height: context.minTouchTarget,
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
                        translations.translate('pos.sale.confirm_sale'),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                ),
              ],
            )
          else
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: context.minTouchTarget,
                    child: FilledButton.tonalIcon(
                      onPressed: canAddLine && !isPreviewing && !isConfirming
                          ? onAddLine
                          : null,
                      icon: const Icon(Icons.add_rounded),
                      label: Text(translations.translate('pos.sale.add_line')),
                    ),
                  ),
                ),
                const SizedBox(width: TchSpacing.s8),
                Expanded(
                  flex: 2,
                  child: SizedBox(
                    height: context.minTouchTarget,
                    child: FilledButton.icon(
                      onPressed: canPrepare ? onPreview : null,
                      icon: isPreviewing
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: TchColors.onPrimary,
                              ),
                            )
                          : const Icon(Icons.fact_check_outlined),
                      label: Text(
                        isPreviewing
                            ? translations.translate('pos.sale.checking_sale')
                            : translations.translate('pos.sale.verify_sale'),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

// ─── Section ──────────────────────────────────────────────────────────────────

class _Section extends StatelessWidget {
  const _Section({super.key, required this.label, required this.child});

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

// ─── Selected draw ───────────────────────────────────────────────────────────

class _SelectedDraw extends ConsumerWidget {
  const _SelectedDraw({
    required this.draws,
    required this.selected,
    required this.enabled,
    required this.compact,
    required this.onSelect,
  });

  final List<CashierAvailableDrawView> draws;
  final String? selected;
  final bool enabled;
  final bool compact;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final selectedDraws = draws.where((item) => item.drawId == selected);
    final draw = selectedDraws.isEmpty ? draws.first : selectedDraws.first;
    final scheme = Theme.of(context).colorScheme;
    return Container(
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
          TchProviderLogo(providerCode: draw.providerCode),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  localizedCashierDrawLabel(draw, translations),
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
                if (!compact) ...[
                  const SizedBox(height: TchSpacing.s4),
                  Text(
                    _cutoffLabel(draw, translations),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ],
            ),
          ),
          TextButton(
            onPressed: enabled
                ? () => _showDrawPicker(
                    context,
                    draws,
                    draw.drawId,
                    translations,
                    onSelect,
                  )
                : null,
            child: Text(translations.translate('pos.sale.change_draw')),
          ),
        ],
      ),
    );
  }
}

Future<void> _showDrawPicker(
  BuildContext context,
  List<CashierAvailableDrawView> draws,
  String selectedId,
  I18nBundle translations,
  ValueChanged<String> onSelect,
) => showModalBottomSheet<void>(
  context: context,
  showDragHandle: true,
  builder: (sheetContext) => SafeArea(
    child: ListView.separated(
      shrinkWrap: true,
      itemCount: draws.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (_, index) {
        final draw = draws[index];
        return ListTile(
          leading: TchProviderLogo(providerCode: draw.providerCode, size: 32),
          selected: draw.drawId == selectedId,
          title: Text(localizedCashierDrawLabel(draw, translations)),
          subtitle: Text(draw.localScheduleLabel),
          trailing: draw.drawId == selectedId
              ? const Icon(Icons.check_rounded)
              : null,
          onTap: () {
            Navigator.of(sheetContext).pop();
            onSelect(draw.drawId);
          },
        );
      },
    ),
  ),
);

String _cutoffLabel(CashierAvailableDrawView draw, I18nBundle translations) {
  if (draw.cutoffAt == null) return '—';
  final remaining = draw.cutoffAt!.difference(DateTime.now());
  if (remaining.isNegative) {
    return translations.translate('pos.dashboard.closed');
  }
  if (remaining.inMinutes < 5) {
    return translations.translate('pos.dashboard.closes_soon');
  }
  if (remaining.inHours > 0) {
    return translations
        .translate('pos.dashboard.closes_in_hours_minutes')
        .replaceAll('{hours}', remaining.inHours.toString())
        .replaceAll('{minutes}', (remaining.inMinutes % 60).toString());
  }
  return translations
      .translate('pos.dashboard.closes_in_minutes')
      .replaceAll('{minutes}', remaining.inMinutes.toString());
}

// ─── Structured number entry ─────────────────────────────────────────────────

class _SelectionInput extends StatelessWidget {
  const _SelectionInput({
    super.key,
    required this.game,
    required this.betOption,
    required this.value,
    required this.enabled,
    required this.onFocus,
    required this.onChanged,
  });

  final CashierGameOptionResponse game;
  final int? betOption;
  final String value;
  final bool enabled;
  final VoidCallback onFocus;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final shape = game.selectionShapeFor(betOption);
    return _GroupedSelectionInput(
      shape: shape,
      value: value,
      enabled: enabled,
      onFocus: onFocus,
      onChanged: onChanged,
    );
  }
}

class _GroupedSelectionInput extends StatefulWidget {
  const _GroupedSelectionInput({
    required this.shape,
    required this.value,
    required this.enabled,
    required this.onFocus,
    required this.onChanged,
  });

  final CashierSelectionShape shape;
  final String value;
  final bool enabled;
  final VoidCallback onFocus;
  final ValueChanged<String> onChanged;

  @override
  State<_GroupedSelectionInput> createState() => _GroupedSelectionInputState();
}

class _GroupedSelectionInputState extends State<_GroupedSelectionInput> {
  late List<TextEditingController> _controllers;
  late List<FocusNode> _focusNodes;

  int get _fieldCount => widget.shape.segments;

  @override
  void initState() {
    super.initState();
    _resetControllers();
  }

  @override
  void didUpdateWidget(covariant _GroupedSelectionInput oldWidget) {
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
    for (final node in _focusNodes) {
      node.addListener(_handleFocus);
    }
    _applyValue(widget.value);
  }

  void _applyValue(String value) {
    final groups = value.split('-');
    for (var index = 0; index < _controllers.length; index++) {
      _controllers[index].text = index < groups.length
          ? groups[index].replaceAll(RegExp(r'\D'), '')
          : '';
    }
  }

  String _value() {
    final groups = <String>[];
    for (final controller in _controllers) {
      groups.add(controller.text);
    }
    if (groups.any((group) => group.length != widget.shape.digits)) return '';
    return groups.join('-');
  }

  void _changed(int index, String value) {
    if (value.length == widget.shape.digits && index < _focusNodes.length - 1) {
      _focusNodes[index + 1].requestFocus();
    }
    widget.onChanged(_value());
  }

  void _handleFocus() {
    if (_focusNodes.any((node) => node.hasFocus)) {
      widget.onFocus();
    }
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
      node.removeListener(_handleFocus);
      node.dispose();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: TchSpacing.s8,
      runSpacing: TchSpacing.s8,
      children: [
        for (var segment = 0; segment < widget.shape.segments; segment++) ...[
          if (segment > 0)
            Text(
              '|',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontWeight: FontWeight.w700,
              ),
            ),
          SizedBox(
            width: widget.shape.segments == 1 ? 152 : 112,
            child: TextField(
              controller: _controllers[segment],
              focusNode: _focusNodes[segment],
              enabled: widget.enabled,
              textAlign: TextAlign.center,
              keyboardType: TextInputType.number,
              maxLength: widget.shape.digits,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(widget.shape.digits),
              ],
              onChanged: (value) => _changed(segment, value),
              decoration: InputDecoration(
                counterText: '',
                contentPadding: const EdgeInsets.symmetric(
                  vertical: TchSpacing.s16,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(TchRadius.md),
                ),
              ),
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w700,
                letterSpacing: 1,
              ),
            ),
          ),
        ],
      ],
    );
  }
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
        constraints: const BoxConstraints(minHeight: 48),
        // No `alignment` here on purpose: a Container with an alignment and no
        // width expands to the incoming max constraint, so inside the Wrap each
        // chip took the full row. Five games cost 316 dp instead of ~120.
        padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s16,
          vertical: TchSpacing.s12,
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

class _ErrorBody extends ConsumerWidget {
  const _ErrorBody({
    required this.message,
    required this.onRetry,
    this.diagnostic,
  });

  final String message;
  final VoidCallback onRetry;
  final DiagnosticInfo? diagnostic;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final translations = ref.watch(i18nBundleProvider);
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
              child: Text(translations.translate('common.retry')),
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

class _CopyDiagnosticButton extends ConsumerStatefulWidget {
  const _CopyDiagnosticButton({required this.diagnostic});

  final DiagnosticInfo diagnostic;

  @override
  ConsumerState<_CopyDiagnosticButton> createState() =>
      _CopyDiagnosticButtonState();
}

class _CopyDiagnosticButtonState extends ConsumerState<_CopyDiagnosticButton> {
  bool _copied = false;

  @override
  Widget build(BuildContext context) {
    final translations = ref.watch(i18nBundleProvider);
    return TextButton.icon(
      onPressed: _copy,
      icon: Icon(
        _copied ? Icons.check_rounded : Icons.content_copy_rounded,
        size: 16,
      ),
      label: Text(
        translations.translate(
          _copied ? 'pos.sale.diagnostic_copied' : 'pos.sale.copy_diagnostic',
        ),
      ),
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
