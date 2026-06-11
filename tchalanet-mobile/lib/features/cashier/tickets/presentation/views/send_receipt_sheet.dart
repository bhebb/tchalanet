import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/storage/op_context_storage.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/services/cashier_ticket_service.dart';

enum _DeliveryMode { sms, whatsapp, email }

/// Bottom sheet for sending a ticket receipt via SMS, WhatsApp, email,
/// or Slack (dev-only, for testing without real carrier infrastructure).
///
/// Usage:
///   await SendReceiptSheet.show(context, ref, ticketId: '...');
class SendReceiptSheet extends ConsumerStatefulWidget {
  const SendReceiptSheet({super.key, required this.ticketId});

  final String ticketId;

  static Future<void> show(
    BuildContext context, {
    required String ticketId,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(TchRadius.xl)),
      ),
      builder: (_) => SendReceiptSheet(ticketId: ticketId),
    );
  }

  @override
  ConsumerState<SendReceiptSheet> createState() => _SendReceiptSheetState();
}

class _SendReceiptSheetState extends ConsumerState<SendReceiptSheet> {
  _DeliveryMode _mode = _DeliveryMode.sms;
  final _inputController = TextEditingController();
  bool _sending = false;
  String? _error;
  bool _sent = false;

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        TchSpacing.s24, TchSpacing.s16, TchSpacing.s24,
        TchSpacing.s24 + bottomInset,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Handle
          Center(
            child: Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: TchSpacing.s16),
              decoration: BoxDecoration(
                color: scheme.outlineVariant,
                borderRadius: BorderRadius.circular(TchRadius.pill),
              ),
            ),
          ),

          Text(
            'Envoyer le reçu',
            style: textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: TchSpacing.s20),

          // Mode selector
          Row(
            children: [
              for (final mode in _DeliveryMode.values) ...[
                if (mode.index > 0) const SizedBox(width: TchSpacing.s8),
                Expanded(child: _ModeChip(
                  mode: mode,
                  selected: _mode == mode,
                  onTap: () => setState(() {
                    _mode = mode;
                    _inputController.clear();
                    _error = null;
                  }),
                )),
              ],
            ],
          ),
          const SizedBox(height: TchSpacing.s20),

          // Input
          TextField(
            controller: _inputController,
            keyboardType: _mode == _DeliveryMode.email
                ? TextInputType.emailAddress
                : TextInputType.phone,
            decoration: InputDecoration(
              labelText: _inputLabel,
              hintText: _inputHint,
              prefixIcon: Icon(_inputIcon),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
            ),
            onChanged: (_) => setState(() => _error = null),
          ),
          const SizedBox(height: TchSpacing.s16),

          // Error
          if (_error != null) ...[
            Container(
              padding: const EdgeInsets.all(TchSpacing.s12),
              decoration: BoxDecoration(
                color: scheme.errorContainer,
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
              child: Text(
                _error!,
                style: textTheme.bodySmall
                    ?.copyWith(color: scheme.onErrorContainer),
              ),
            ),
            const SizedBox(height: TchSpacing.s12),
          ],

          // Success
          if (_sent) ...[
            Container(
              padding: const EdgeInsets.all(TchSpacing.s12),
              decoration: BoxDecoration(
                color: TchColors.successContainer,
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
              child: Row(
                children: [
                  const Icon(Icons.check_circle_outline_rounded,
                      color: TchColors.success, size: 18),
                  const SizedBox(width: TchSpacing.s8),
                  Text(
                    'Reçu envoyé avec succès.',
                    style: textTheme.bodySmall
                        ?.copyWith(color: TchColors.success),
                  ),
                ],
              ),
            ),
            const SizedBox(height: TchSpacing.s12),
          ],

          // Send button
          SizedBox(
            height: 52,
            child: FilledButton.icon(
              onPressed: _sending || _sent ? null : _send,
              icon: _sending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: TchColors.onPrimary),
                    )
                  : Icon(_modeIcon),
              label: Text(
                _sending ? 'ENVOI…' : 'ENVOYER',
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
    );
  }

  String? _validate(String value) {
    if (value.isEmpty) return 'Veuillez saisir ${_inputLabel.toLowerCase()}.';
    if (_mode == _DeliveryMode.email) {
      if (!value.contains('@') || !value.contains('.')) {
        return 'Adresse e-mail invalide.';
      }
    } else {
      if (value.length < 8) return 'Numéro de téléphone invalide.';
    }
    return null;
  }

  Future<void> _send() async {
    final value = _inputController.text.trim();
    final validationError = _validate(value);
    if (validationError != null) {
      setState(() => _error = validationError);
      return;
    }

    setState(() {
      _sending = true;
      _error = null;
    });

    try {
      final terminalId =
          await ref.read(opContextStorageProvider).readTerminalId();
      if (terminalId == null || terminalId.isEmpty) {
        setState(() => _error = 'Aucun terminal actif. Rouvrez une session.');
        return;
      }
      await ref.read(cashierTicketServiceProvider).sendReceipt(
            widget.ticketId,
            terminalId: terminalId,
            channel: _mode.serverKey,
            to: value,
          );
      setState(() => _sent = true);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _sending = false);
    }
  }

  String get _inputLabel => switch (_mode) {
        _DeliveryMode.sms => 'Numéro de téléphone',
        _DeliveryMode.whatsapp => 'Numéro WhatsApp',
        _DeliveryMode.email => 'Adresse e-mail',
      };

  String get _inputHint => switch (_mode) {
        _DeliveryMode.sms => '+509 XXXX XXXX',
        _DeliveryMode.whatsapp => '+509 XXXX XXXX',
        _DeliveryMode.email => 'client@example.com',
      };

  IconData get _inputIcon => switch (_mode) {
        _DeliveryMode.sms => Icons.sms_rounded,
        _DeliveryMode.whatsapp => Icons.chat_rounded,
        _DeliveryMode.email => Icons.email_outlined,
      };

  IconData get _modeIcon => switch (_mode) {
        _DeliveryMode.sms => Icons.sms_rounded,
        _DeliveryMode.whatsapp => Icons.chat_rounded,
        _DeliveryMode.email => Icons.email_rounded,
      };
}

extension on _DeliveryMode {
  String get serverKey => switch (this) {
        _DeliveryMode.sms => 'SMS',
        _DeliveryMode.whatsapp => 'WHATSAPP',
        _DeliveryMode.email => 'EMAIL',
      };
}

// ─── Mode chip ────────────────────────────────────────────────────────────────

class _ModeChip extends StatelessWidget {
  const _ModeChip({
    required this.mode,
    required this.selected,
    required this.onTap,
  });

  final _DeliveryMode mode;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (icon, label) = switch (mode) {
      _DeliveryMode.sms => (Icons.sms_rounded, 'SMS'),
      _DeliveryMode.whatsapp => (Icons.chat_rounded, 'WhatsApp'),
      _DeliveryMode.email => (Icons.email_outlined, 'Email'),
    };

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(
          vertical: TchSpacing.s8,
          horizontal: TchSpacing.s4,
        ),
        decoration: BoxDecoration(
          color: selected ? scheme.primaryContainer : scheme.surfaceContainerLow,
          borderRadius: BorderRadius.circular(TchRadius.md),
          border: Border.all(
            color: selected ? scheme.primary : scheme.outlineVariant,
          ),
        ),
        child: Column(
          children: [
            Icon(icon,
                size: 20,
                color: selected ? scheme.primary : scheme.onSurfaceVariant),
            const SizedBox(height: 4),
            Text(
              label,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: selected ? scheme.primary : scheme.onSurfaceVariant,
                    fontWeight:
                        selected ? FontWeight.w700 : FontWeight.w400,
                  ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
