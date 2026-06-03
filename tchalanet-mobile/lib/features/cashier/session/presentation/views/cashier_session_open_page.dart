import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../home/data/models/cashier_home_models.dart';
import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../view_models/cashier_session_controller.dart';

class CashierSessionOpenPage extends ConsumerStatefulWidget {
  const CashierSessionOpenPage({super.key});

  @override
  ConsumerState<CashierSessionOpenPage> createState() =>
      _CashierSessionOpenPageState();
}

class _CashierSessionOpenPageState
    extends ConsumerState<CashierSessionOpenPage> {
  final _controller = TextEditingController(text: '0.00');

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _selectAll();
    });
  }

  void _selectAll() {
    _controller.selection =
        TextSelection(baseOffset: 0, extentOffset: _controller.text.length);
  }

  void _addAmount(int amount) {
    final current = double.tryParse(_controller.text.replaceAll(',', '.')) ?? 0;
    setState(() {
      _controller.text = (current + amount).toStringAsFixed(2);
      _selectAll();
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final sessionState = ref.watch(cashierSessionControllerProvider);
    final homeAsync = ref.watch(cashierHomeProvider);
    final isLoading = sessionState is SessionOpenInProgress;

    ref.listen<SessionOpenState>(cashierSessionControllerProvider, (_, next) {
      if (next is SessionOpenSuccess) context.pop();
    });

    final opCtx = homeAsync.when(
      data: (h) => h.operationalContext,
      loading: () => null,
      error: (_, _) => null,
    );

    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: isLoading ? null : () => context.pop(),
        ),
        title: Row(
          children: [
            const SizedBox(width: 4),
            if (opCtx?.terminalLabel != null) ...[
              Text(
                opCtx!.terminalLabel!,
                style: textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(width: TchSpacing.s8),
              _OnlineDot(),
            ],
          ],
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: TchSpacing.s12),
            child: CircleAvatar(
              radius: 18,
              backgroundColor: scheme.primaryContainer,
              child: const Icon(Icons.person_rounded, size: 18),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s32),
          child: Column(
            children: [
              const Spacer(flex: 2),

              // Icon
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: scheme.primaryContainer,
                  borderRadius: BorderRadius.circular(TchRadius.xl),
                  border: Border.all(color: scheme.primary.withValues(alpha: 0.2)),
                ),
                child: Icon(
                  Icons.lock_open_rounded,
                  size: 40,
                  color: scheme.primary,
                ),
              ),
              const SizedBox(height: TchSpacing.s24),

              // Title
              Text(
                'Ouvrir la session',
                style: textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s8),
              Text(
                'Comptez votre fond de caisse initial\npour commencer les ventes.',
                style: textTheme.bodyMedium?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),

              const Spacer(),

              // Large HTG input
              _HtgAmountField(controller: _controller, enabled: !isLoading),
              const SizedBox(height: TchSpacing.s16),

              // Quick chips
              Row(
                children: [
                  for (final amount in [500, 1000, 2000]) ...[
                    if (amount > 500) const SizedBox(width: TchSpacing.s8),
                    Expanded(
                      child: _QuickChip(
                        label: amount == 1000 ? '+1 000' : '+$amount',
                        enabled: !isLoading,
                        onTap: () => _addAmount(amount),
                      ),
                    ),
                  ],
                ],
              ),

              // Error
              if (sessionState is SessionOpenFailure) ...[
                const SizedBox(height: TchSpacing.s16),
                Container(
                  padding: const EdgeInsets.all(TchSpacing.s12),
                  decoration: BoxDecoration(
                    color: scheme.errorContainer,
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline_rounded,
                          size: 18, color: scheme.onErrorContainer),
                      const SizedBox(width: TchSpacing.s8),
                      Expanded(
                        child: Text(
                          sessionState.message,
                          style: textTheme.bodySmall
                              ?.copyWith(color: scheme.onErrorContainer),
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const Spacer(flex: 2),

              // CTA
              SizedBox(
                width: double.infinity,
                height: 60,
                child: FilledButton.icon(
                  onPressed: isLoading || opCtx == null
                      ? null
                      : () => _submit(ref, opCtx),
                  icon: isLoading
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.play_arrow_rounded),
                  label: Text(
                    isLoading ? 'OUVERTURE…' : 'DÉMARRER LA SESSION',
                    style: const TextStyle(
                        fontWeight: FontWeight.w700, letterSpacing: 1),
                  ),
                  style: FilledButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(TchRadius.lg),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: TchSpacing.s32),
            ],
          ),
        ),
      ),
    );
  }

  void _submit(WidgetRef ref, CashierHomeOpCtx opCtx) {
    final float =
        double.tryParse(_controller.text.replaceAll(',', '.')) ?? 0.0;
    ref.read(cashierSessionControllerProvider.notifier).openSession(
          outletId: opCtx.outletId!,
          terminalId: opCtx.terminalId!,
          openingFloat: float,
        );
  }
}

// ─── HTG amount field ─────────────────────────────────────────────────────────

class _HtgAmountField extends StatelessWidget {
  const _HtgAmountField({required this.controller, required this.enabled});

  final TextEditingController controller;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Stack(
      alignment: Alignment.centerLeft,
      children: [
        TextField(
          controller: controller,
          enabled: enabled,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          inputFormatters: [
            FilteringTextInputFormatter.allow(RegExp(r'[\d.,]')),
          ],
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.displaySmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: scheme.onSurface,
              ),
          decoration: InputDecoration(
            contentPadding: const EdgeInsets.fromLTRB(
                TchSpacing.s64, TchSpacing.s20, TchSpacing.s16, TchSpacing.s20),
            filled: true,
            fillColor: scheme.surfaceContainerLowest,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(TchRadius.lg),
              borderSide: BorderSide(color: scheme.outlineVariant, width: 2),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(TchRadius.lg),
              borderSide: BorderSide(color: scheme.outlineVariant, width: 2),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(TchRadius.lg),
              borderSide: BorderSide(color: scheme.primary, width: 2),
            ),
          ),
        ),
        Positioned(
          left: TchSpacing.s20,
          child: Text(
            'HTG',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: scheme.outline,
                  fontWeight: FontWeight.w500,
                ),
          ),
        ),
      ],
    );
  }
}

// ─── Quick chip ───────────────────────────────────────────────────────────────

class _QuickChip extends StatelessWidget {
  const _QuickChip(
      {required this.label, required this.enabled, required this.onTap});

  final String label;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surfaceContainerLow,
      borderRadius: BorderRadius.circular(TchRadius.md),
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(TchRadius.md),
        child: Container(
          height: 52,
          decoration: BoxDecoration(
            border: Border.all(color: scheme.outlineVariant),
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: scheme.primary,
                  fontWeight: FontWeight.w700,
                ),
          ),
        ),
      ),
    );
  }
}

// ─── Online dot ───────────────────────────────────────────────────────────────

class _OnlineDot extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: const BoxDecoration(
            color: Color(0xFF22C55E),
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 4),
        Text(
          'READY',
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
                fontSize: 10,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
              ),
        ),
      ],
    );
  }
}
