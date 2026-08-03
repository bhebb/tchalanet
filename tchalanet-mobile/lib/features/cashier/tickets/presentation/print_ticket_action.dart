import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../../../../core/i18n/i18n_repository.dart';
import '../../../../core/network/api_exception.dart';
import '../data/services/cashier_ticket_service.dart';

const _sellerRequestedReprintReason = 'SELLER_REQUESTED_REPRINT';
const _reprintReasonPresets = [
  _sellerRequestedReprintReason,
  'PRINTER_FAILURE',
  'PRINT_ILLEGIBLE',
  'CUSTOMER_REQUEST',
];
const _reprintReasonRequiredCode = 'ticket.reprint.reason_required';

/// Opens the native PDF print preview for an already rendered ticket receipt.
/// The initial print is reason-free. Seller-initiated reprints include the
/// stable default reason. The seller can choose another preset or edit it.
Future<void> printTicket(
  BuildContext context,
  WidgetRef ref,
  String ticketId, {
  String? reprintReason,
}) async {
  final messenger = ScaffoldMessenger.of(context);
  final translations = ref.read(i18nBundleProvider);
  try {
    final service = ref.read(cashierTicketServiceProvider);
    Uint8List bytes;
    try {
      bytes = await service.print(ticketId, reprintReason: reprintReason);
    } on ApiException catch (error) {
      if (error.code != _reprintReasonRequiredCode ||
          reprintReason?.trim().isNotEmpty == true) {
        rethrow;
      }
      bytes = await service.print(
        ticketId,
        reprintReason: _sellerRequestedReprintReason,
      );
    }

    if (bytes.isEmpty) {
      messenger.showSnackBar(
        SnackBar(
          content: Text(translations.translate('pos.tickets.receipt_empty')),
        ),
      );
      return;
    }

    await Printing.layoutPdf(onLayout: (_) async => bytes);
  } on ApiException catch (error) {
    messenger.showSnackBar(
      SnackBar(content: Text(localizedUserError(translations, error))),
    );
  } catch (_) {
    messenger.showSnackBar(
      SnackBar(content: Text(translations.translate('common.error.unknown'))),
    );
  }
}

// Guards against the reprint dialog stacking multiple times when the
// triggering IconButton is tapped repeatedly before the first dialog opens —
// there's no per-button loading state at the call sites, so this is a
// per-ticket re-entrancy guard instead.
final _reprintInFlight = <String>{};

Future<void> requestTicketReprint(
  BuildContext context,
  WidgetRef ref,
  String ticketId,
) async {
  if (!_reprintInFlight.add(ticketId)) return;
  final translations = ref.read(i18nBundleProvider);
  final controller = TextEditingController(text: _sellerRequestedReprintReason);
  try {
    final reason = await showDialog<String>(
      context: context,
      builder: (dialogContext) {
        var selectedPreset = _sellerRequestedReprintReason;
        var showValidation = false;
        return StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            title: Text(translations.translate('pos.tickets.reprint_title')),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(translations.translate('pos.tickets.reprint_message')),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  initialValue: selectedPreset,
                  decoration: InputDecoration(
                    labelText: translations.translate(
                      'pos.tickets.reprint_preset',
                    ),
                  ),
                  items: _reprintReasonPresets
                      .map(
                        (preset) => DropdownMenuItem(
                          value: preset,
                          child: Text(
                            translations.translate(_presetLabelKey(preset)),
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: (preset) {
                    if (preset == null) return;
                    setDialogState(() {
                      selectedPreset = preset;
                      controller.text = preset;
                      showValidation = false;
                    });
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: controller,
                  autofocus: true,
                  minLines: 2,
                  maxLines: 4,
                  onChanged: (_) {
                    if (showValidation) {
                      setDialogState(() => showValidation = false);
                    }
                  },
                  decoration: InputDecoration(
                    labelText: translations.translate(
                      'pos.tickets.reprint_reason',
                    ),
                    errorText: showValidation
                        ? translations.translate(
                            'pos.tickets.reprint_reason_invalid',
                          )
                        : null,
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: Text(translations.translate('common.cancel')),
              ),
              FilledButton.icon(
                onPressed: () {
                  final reason = controller.text.trim();
                  if (reason.length < 10) {
                    setDialogState(() => showValidation = true);
                    return;
                  }
                  Navigator.of(dialogContext).pop(reason);
                },
                icon: const Icon(Icons.print_rounded),
                label: Text(
                  translations.translate('pos.tickets.reprint_confirm'),
                ),
              ),
            ],
          ),
        );
      },
    );
    if (reason != null && context.mounted) {
      await printTicket(context, ref, ticketId, reprintReason: reason);
    }
  } finally {
    controller.dispose();
    _reprintInFlight.remove(ticketId);
  }
}

String _presetLabelKey(String preset) => switch (preset) {
  _sellerRequestedReprintReason => 'pos.tickets.reprint_default',
  'PRINTER_FAILURE' => 'pos.tickets.reprint_printer_failure',
  'PRINT_ILLEGIBLE' => 'pos.tickets.reprint_illegible',
  'CUSTOMER_REQUEST' => 'pos.tickets.reprint_customer_request',
  _ => 'pos.tickets.reprint_default',
};
