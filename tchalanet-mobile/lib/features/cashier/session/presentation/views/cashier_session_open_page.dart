import 'package:flutter/material.dart';
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
  final _floatController = TextEditingController(text: '0.00');

  @override
  void dispose() {
    _floatController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final sessionState = ref.watch(cashierSessionControllerProvider);
    final homeAsync = ref.watch(cashierHomeProvider);

    ref.listen<SessionOpenState>(cashierSessionControllerProvider, (_, next) {
      if (next is SessionOpenSuccess) context.pop();
    });

    final opCtx = homeAsync.when(
      data: (h) => h.operationalContext,
      loading: () => null,
      error: (_, _) => null,
    );
    final isLoading = sessionState is SessionOpenInProgress;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Ouvrir la session'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: isLoading ? null : () => context.pop(),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Context summary
              if (opCtx != null) _ContextSummaryCard(opCtx: opCtx),
              const SizedBox(height: TchSpacing.s24),

              // Opening float
              Text(
                'Fonds de caisse',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
              ),
              const SizedBox(height: TchSpacing.s8),
              TextFormField(
                controller: _floatController,
                enabled: !isLoading,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  hintText: '0.00',
                  suffixText: 'HTG',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                ),
              ),
              const SizedBox(height: TchSpacing.s8),
              Text(
                'Montant en caisse au début de la session. Saisir 0 si aucun fonds initial.',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
              ),

              // Error
              if (sessionState is SessionOpenFailure) ...[
                const SizedBox(height: TchSpacing.s16),
                _ErrorBanner(message: sessionState.message),
              ],

              const Spacer(),

              // Confirm button
              FilledButton.icon(
                onPressed: isLoading || opCtx == null
                    ? null
                    : () => _submit(context, ref, opCtx),
                icon: isLoading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.play_circle_outline_rounded),
                label: Text(isLoading ? 'Ouverture…' : 'Ouvrir la session'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _submit(
      BuildContext context, WidgetRef ref, CashierHomeOpCtx opCtx) {
    final float = double.tryParse(
            _floatController.text.replaceAll(',', '.')) ??
        0.0;
    ref.read(cashierSessionControllerProvider.notifier).openSession(
          outletId: opCtx.outletId!,
          terminalId: opCtx.terminalId!,
          openingFloat: float,
        );
  }
}

// ─── Context summary ──────────────────────────────────────────────────────────

class _ContextSummaryCard extends StatelessWidget {
  const _ContextSummaryCard({required this.opCtx});

  final CashierHomeOpCtx opCtx;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Poste de vente',
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                  letterSpacing: 0.5,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: TchSpacing.s8),
          if (opCtx.outletName != null)
            _Row(
              icon: Icons.store_rounded,
              label: opCtx.outletName!,
            ),
          if (opCtx.terminalLabel != null) ...[
            const SizedBox(height: TchSpacing.s4),
            _Row(
              icon: Icons.point_of_sale_rounded,
              label: opCtx.terminalLabel!,
            ),
          ],
        ],
      ),
    );
  }
}

class _Row extends StatelessWidget {
  const _Row({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(icon, size: 16, color: scheme.onSurfaceVariant),
        const SizedBox(width: TchSpacing.s8),
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
      ],
    );
  }
}

// ─── Error banner ─────────────────────────────────────────────────────────────

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
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
              message,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: scheme.onErrorContainer,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}
