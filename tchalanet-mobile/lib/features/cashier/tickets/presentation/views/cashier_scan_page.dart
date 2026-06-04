import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../../design_system/tokens/tch_colors.dart';

import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_ticket_service.dart';

// ─── State ────────────────────────────────────────────────────────────────────

sealed class VerifyState {
  const VerifyState();
}

final class VerifyIdle extends VerifyState {
  const VerifyIdle();
}

final class VerifyInProgress extends VerifyState {
  const VerifyInProgress();
}

final class VerifyResult extends VerifyState {
  const VerifyResult(this.response, this.scannedValue);
  final CashierTicketVerificationResponse response;
  final String scannedValue;
}

final class VerifyError extends VerifyState {
  const VerifyError(this.message);
  final String message;
}

class VerifyController extends Notifier<VerifyState> {
  @override
  VerifyState build() => const VerifyIdle();

  Future<void> verify(String scannedValue) async {
    if (scannedValue.trim().isEmpty) return;
    state = const VerifyInProgress();
    try {
      final result = await ref
          .read(cashierTicketServiceProvider)
          .verify(CashierVerifyTicketRequest(scannedValue: scannedValue.trim()));
      state = VerifyResult(result, scannedValue.trim());
    } catch (e) {
      state = VerifyError(e.toString());
    }
  }

  void reset() => state = const VerifyIdle();
}

final verifyControllerProvider =
    NotifierProvider<VerifyController, VerifyState>(VerifyController.new);

// ─── Page ─────────────────────────────────────────────────────────────────────

class CashierScanPage extends ConsumerStatefulWidget {
  const CashierScanPage({super.key});

  @override
  ConsumerState<CashierScanPage> createState() => _CashierScanPageState();
}

class _CashierScanPageState extends ConsumerState<CashierScanPage> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(verifyControllerProvider);
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final isLoading = state is VerifyInProgress;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Scanner / Vérifier'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          tooltip: 'Accueil',
          onPressed: () => context.go('/pos'),
        ),
        actions: [
          if (state is VerifyResult || state is VerifyError)
            TextButton(
              onPressed: () {
                _controller.clear();
                ref.read(verifyControllerProvider.notifier).reset();
              },
              child: const Text('Nouveau'),
            ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(TchSpacing.s24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // QR scan placeholder
                    GestureDetector(
                      onTap: () {
                        // Future: launch camera QR scanner
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Scan QR — bientôt disponible'),
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      },
                      child: Container(
                        height: 160,
                        decoration: BoxDecoration(
                          color: scheme.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(TchRadius.lg),
                          border: Border.all(
                            color: scheme.outlineVariant,
                            style: BorderStyle.solid,
                          ),
                        ),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.qr_code_scanner_rounded,
                              size: 56,
                              color: scheme.onSurface.withValues(alpha: 0.3),
                            ),
                            const SizedBox(height: TchSpacing.s8),
                            Text(
                              'Appuyez pour scanner',
                              style: textTheme.bodySmall?.copyWith(
                                color: scheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                    const SizedBox(height: TchSpacing.s20),

                    // Divider
                    Row(
                      children: [
                        Expanded(child: Divider(color: scheme.outlineVariant)),
                        Padding(
                          padding: const EdgeInsets.symmetric(
                              horizontal: TchSpacing.s12),
                          child: Text(
                            'ou saisir manuellement',
                            style: textTheme.labelSmall?.copyWith(
                              color: scheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                        Expanded(child: Divider(color: scheme.outlineVariant)),
                      ],
                    ),

                    const SizedBox(height: TchSpacing.s20),

                    // Manual input
                    TextField(
                      controller: _controller,
                      enabled: !isLoading,
                      textCapitalization: TextCapitalization.characters,
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(
                            RegExp(r'[A-Za-z0-9\-]')),
                        UpperCaseTextFormatter(),
                      ],
                      onSubmitted: (_) => _verify(),
                      decoration: InputDecoration(
                        labelText: 'Code ou URL du ticket',
                        hintText: 'Ex: 40CP-JBMR',
                        prefixIcon: const Icon(Icons.confirmation_number_outlined),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(TchRadius.md),
                        ),
                        suffixIcon: _controller.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear_rounded),
                                onPressed: () {
                                  _controller.clear();
                                  setState(() {});
                                },
                              )
                            : null,
                      ),
                      onChanged: (_) => setState(() {}),
                    ),

                    const SizedBox(height: TchSpacing.s24),

                    // Result
                    if (state is VerifyResult)
                      _VerifyResultCard(result: state.response)
                    else if (state is VerifyError)
                      _ErrorCard(message: state.message),
                  ],
                ),
              ),
            ),

            // CTA
            Padding(
              padding: const EdgeInsets.fromLTRB(
                TchSpacing.s24, TchSpacing.s8, TchSpacing.s24, TchSpacing.s24,
              ),
              child: Column(
                children: [
                  // Payout action (when PAYABLE)
                  if (state is VerifyResult &&
                      state.response.isPayable) ...[
                    SizedBox(
                      width: double.infinity,
                      height: 56,
                      child: FilledButton.icon(
                        onPressed: () => _showPayoutConfirm(context, state),
                        icon: const Icon(Icons.payments_rounded),
                        label: const Text(
                          'PAYER LE GAGNANT',
                          style: TextStyle(
                              fontWeight: FontWeight.w700, letterSpacing: 0.5),
                        ),
                        style: FilledButton.styleFrom(
                          backgroundColor: TchColors.success,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(TchRadius.md),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s8),
                  ],

                  // View detail action — only enabled when ticketId is available
                  if (state is VerifyResult) ...[
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: OutlinedButton.icon(
                        onPressed: state.response.ticketId != null
                            ? () => context.push(
                                '/pos/tickets/${state.response.ticketId}')
                            : null,
                        icon: const Icon(Icons.receipt_long_rounded),
                        label: const Text('VOIR LES DÉTAILS'),
                        style: OutlinedButton.styleFrom(
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(TchRadius.md),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s8),
                  ],

                  // Verify button
                  SizedBox(
                    width: double.infinity,
                    height: state is VerifyResult ? 48 : 56,
                    child: FilledButton.icon(
                      onPressed: isLoading ? null : _verify,
                      icon: isLoading
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: TchColors.onPrimary),
                            )
                          : const Icon(Icons.search_rounded),
                      label: Text(
                        isLoading ? 'VÉRIFICATION…' : 'VÉRIFIER',
                        style: const TextStyle(
                            fontWeight: FontWeight.w700, letterSpacing: 0.5),
                      ),
                      style: FilledButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(TchRadius.md),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _verify() {
    final value = _controller.text.trim();
    if (value.isEmpty) return;
    ref.read(verifyControllerProvider.notifier).verify(value);
  }

  void _showPayoutConfirm(BuildContext context, VerifyResult state) {
    showDialog<void>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Confirmer le paiement'),
        content: Text(
          'Voulez-vous payer le gagnant pour le ticket ${state.scannedValue} ?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuler'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(context);
              // TODO: POST /payout — next cycle
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Paiement — bientôt disponible'),
                  behavior: SnackBarBehavior.floating,
                ),
              );
            },
            child: const Text('Confirmer'),
          ),
        ],
      ),
    );
  }
}

// ─── Verification result card ─────────────────────────────────────────────────

class _VerifyResultCard extends StatelessWidget {
  const _VerifyResultCard({required this.result});

  final CashierTicketVerificationResponse result;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    final (bgColor, fgColor, borderColor, icon) = switch (result.severity) {
      'SUCCESS' => (
          TchColors.successContainer,
          TchColors.success,
          TchColors.successContainer,
          Icons.check_circle_outline_rounded,
        ),
      'WARNING' => (
          TchColors.warningContainer,
          TchColors.warning,
          TchColors.warning,
          Icons.warning_amber_rounded,
        ),
      'ERROR' => (
          scheme.errorContainer,
          scheme.onErrorContainer,
          scheme.error.withValues(alpha: 0.3),
          Icons.cancel_outlined,
        ),
      _ => (
          scheme.surfaceContainerLow,
          scheme.onSurface,
          scheme.outlineVariant,
          Icons.info_outline_rounded,
        ),
    };

    final statusLabel = switch (result.status) {
      'PAYABLE' => 'Ticket gagnant — prêt à payer',
      'ALREADY_PAID' => 'Ticket déjà payé',
      'NOT_PAYABLE_LOST' => 'Ticket perdant',
      'NOT_PAYABLE_PENDING_DRAW' => 'Tirage non encore effectué',
      'NOT_PAYABLE_RESULT_PENDING' => 'Résultats en attente',
      'CANCELLED' => 'Ticket annulé',
      'VOIDED' => 'Ticket invalidé',
      'NOT_FOUND' => 'Ticket introuvable',
      'BLOCKED' => 'Ticket bloqué — contacter l\'admin',
      _ => result.status,
    };

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: fgColor, size: 22),
              const SizedBox(width: TchSpacing.s8),
              Expanded(
                child: Text(
                  statusLabel,
                  style: textTheme.titleSmall?.copyWith(
                    color: fgColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          if (result.availableActions.isNotEmpty) ...[
            const SizedBox(height: TchSpacing.s12),
            Wrap(
              spacing: TchSpacing.s8,
              children: result.availableActions
                  .where((a) => a.enabled && a.type != 'NONE')
                  .map((a) => _ActionBadge(action: a, fgColor: fgColor))
                  .toList(),
            ),
          ],
        ],
      ),
    );
  }
}

class _ActionBadge extends StatelessWidget {
  const _ActionBadge({required this.action, required this.fgColor});

  final CashierAction action;
  final Color fgColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s8,
        vertical: TchSpacing.s4,
      ),
      decoration: BoxDecoration(
        color: fgColor.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(TchRadius.pill),
      ),
      child: Text(
        action.type.replaceAll('_', ' '),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: fgColor,
              fontWeight: FontWeight.w600,
            ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
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
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: scheme.onErrorContainer),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Formatter ────────────────────────────────────────────────────────────────

class UpperCaseTextFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
      TextEditingValue old, TextEditingValue updated) {
    return updated.copyWith(text: updated.text.toUpperCase());
  }
}
