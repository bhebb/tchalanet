import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../print_ticket_action.dart';
import 'send_receipt_sheet.dart';

/// Shown after a successful `POST /sell`. Displays the ticket code and
/// delivery actions (copy, message, print, WhatsApp/SMS).
class CashierSellSuccessPage extends ConsumerWidget {
  const CashierSellSuccessPage({
    super.key,
    required this.ticketId,
    required this.ticketCode,
    this.publicCode,
    this.shareableText,
  });

  final String ticketId;
  final String ticketCode;
  final String? publicCode;
  final String? shareableText;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(userSessionProvider);
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    final displayCode = publicCode ?? ticketCode;

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          tooltip: 'Retour à l\'accueil',
          onPressed: () {
            ref.invalidate(cashierHomeProvider);
            context.go('/pos');
          },
        ),
        title: Row(
          children: [
            const Icon(Icons.person_outline_rounded, size: 18),
            const SizedBox(width: TchSpacing.s8),
            Text(
              session.displayName?.toUpperCase() ?? session.username?.toUpperCase() ?? '—',
              style: textTheme.labelMedium?.copyWith(
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
                color: scheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => context.push('/pos/profile'),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(
                  horizontal: TchSpacing.s24,
                  vertical: TchSpacing.s32,
                ),
                child: Column(
                  children: [
                    // Success indicator
                    Container(
                      width: 64,
                      height: 64,
                      decoration: BoxDecoration(
                        color: scheme.secondaryContainer,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.check_rounded,
                        size: 32,
                        color: scheme.onSecondaryContainer,
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s16),
                    Text(
                      'Vente acceptée',
                      style: textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s4),
                    Text(
                      'Paiement validé avec succès.',
                      style: textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),

                    const SizedBox(height: TchSpacing.s40),

                    // Ticket code — large display
                    GestureDetector(
                      onTap: () {
                        Clipboard.setData(ClipboardData(text: displayCode));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Code copié'),
                            duration: Duration(seconds: 1),
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      },
                      child: Text(
                        displayCode,
                        style: textTheme.displayMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                          letterSpacing: -1,
                          color: scheme.onSurface,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s12),
                    Text(
                      'Donnez ce code au client.',
                      style: textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w500,
                        color: scheme.onSurface,
                      ),
                      textAlign: TextAlign.center,
                    ),

                    const SizedBox(height: TchSpacing.s40),

                    // Action bento grid 2×2
                    GridView.count(
                      crossAxisCount: 2,
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      crossAxisSpacing: TchSpacing.s12,
                      mainAxisSpacing: TchSpacing.s12,
                      childAspectRatio: 1.4,
                      children: [
                        _ActionTile(
                          icon: Icons.content_copy_rounded,
                          label: 'Copier code',
                          onTap: () {
                            Clipboard.setData(
                                ClipboardData(text: displayCode));
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('Copié !'),
                                duration: Duration(seconds: 1),
                                behavior: SnackBarBehavior.floating,
                              ),
                            );
                          },
                        ),
                        _ActionTile(
                          icon: Icons.sms_rounded,
                          label: 'Message',
                          onTap: () => SendReceiptSheet.show(
                            context,
                            ticketId: ticketId,
                          ),
                        ),
                        _ActionTile(
                          icon: Icons.print_rounded,
                          label: 'Imprimer',
                          onTap: () => printTicket(context, ref, ticketId),
                        ),
                        _ActionTile(
                          icon: Icons.share_rounded,
                          label: 'WhatsApp/SMS',
                          onTap: () => SendReceiptSheet.show(
                            context,
                            ticketId: ticketId,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // Sticky bottom — NOUVEAU TICKET
            Padding(
              padding: const EdgeInsets.fromLTRB(
                TchSpacing.s24, TchSpacing.s8, TchSpacing.s24, TchSpacing.s24,
              ),
              child: SizedBox(
                width: double.infinity,
                height: 56,
                child: FilledButton.icon(
                  onPressed: () {
                    ref.invalidate(cashierHomeProvider);
                    context.go('/pos');
                  },
                  icon: const Icon(Icons.add_rounded),
                  label: const Text(
                    'NOUVEAU TICKET',
                    style: TextStyle(
                        fontWeight: FontWeight.w700, letterSpacing: 1),
                  ),
                  style: FilledButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(TchRadius.lg),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Action tile ──────────────────────────────────────────────────────────────

class _ActionTile extends StatelessWidget {
  const _ActionTile({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(TchRadius.lg),
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: scheme.outlineVariant),
            borderRadius: BorderRadius.circular(TchRadius.lg),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: scheme.onSurfaceVariant, size: 24),
              const SizedBox(height: TchSpacing.s8),
              Text(
                label.toUpperCase(),
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: scheme.onSurface,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.5,
                    ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
