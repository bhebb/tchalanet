import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/client_diagnostics/client_diagnostics_models.dart';
import '../../../../core/client_diagnostics/client_diagnostics_reporter.dart';
import '../../../../core/i18n/i18n_repository.dart';
import '../../../../core/network/api_exception.dart';
import '../../home/presentation/view_models/cashier_home_providers.dart';
import '../printing/printer_contracts.dart';
import '../printing/printer_service.dart';

const _sellerRequestedReprintReason = 'SELLER_REQUESTED_REPRINT';
const _reprintReasonPresets = [
  _sellerRequestedReprintReason,
  'PRINTER_FAILURE',
  'PRINT_ILLEGIBLE',
  'CUSTOMER_REQUEST',
];
const _reprintReasonRequiredCode = 'ticket.reprint.reason_required';

/// Prints an already rendered ticket through the configured printer service.
/// The system PDF surface is available only when the caller explicitly opts
/// into the manual fallback.
Future<void> printTicket(
  BuildContext context,
  WidgetRef ref,
  String ticketId, {
  String? reprintReason,
  bool manualPdfFallback = true,
  bool automatic = false,
}) async {
  final messenger = ScaffoldMessenger.of(context);
  final translations = ref.read(i18nBundleProvider);
  try {
    try {
      final result = await ref
          .read(printerServiceProvider)
          .printTicket(
            ticketId,
            paperSize: _configuredPaperSize(ref),
            reprintReason: reprintReason,
            buyerLocale: translations.locale,
            allowManualPdfFallback: manualPdfFallback && !automatic,
          );
      if (result.outcome == PrintOutcome.unavailable) {
        _recordPrintDiagnostic(
          ref,
          ticketId,
          automatic,
          errorCode: 'printer_unavailable',
          printerProvider: result.adapterId,
          printerState: result.outcome.name,
        );
        messenger.showSnackBar(
          SnackBar(
            content: Text(
              translations.translate('pos.settings.printer_unavailable'),
            ),
          ),
        );
        return;
      }
      if (!result.isPrinted) {
        if (result.error case final ApiException error) {
          _recordPrintDiagnostic(
            ref,
            ticketId,
            automatic,
            errorCode: error.code,
            message: error.message,
            requestId: error.requestId,
            httpStatus: error.statusCode,
            printerProvider: result.adapterId,
            printerState: result.outcome.name,
          );
          throw error;
        }
        _recordPrintDiagnostic(
          ref,
          ticketId,
          automatic,
          errorCode: 'print_failed',
          message: result.error?.toString(),
          printerProvider: result.adapterId,
          printerState: result.outcome.name,
        );
        messenger.showSnackBar(
          SnackBar(
            content: Text(translations.translate('common.error.unknown')),
          ),
        );
        return;
      }
    } on ApiException catch (error) {
      if (error.code != _reprintReasonRequiredCode ||
          reprintReason?.trim().isNotEmpty == true) {
        rethrow;
      }
      final result = await ref
          .read(printerServiceProvider)
          .printTicket(
            ticketId,
            paperSize: _configuredPaperSize(ref),
            reprintReason: _sellerRequestedReprintReason,
            buyerLocale: translations.locale,
            allowManualPdfFallback: manualPdfFallback && !automatic,
          );
      if (!result.isPrinted) {
        if (result.error case final ApiException error) {
          _recordPrintDiagnostic(
            ref,
            ticketId,
            automatic,
            errorCode: error.code,
            message: error.message,
            requestId: error.requestId,
            httpStatus: error.statusCode,
            printerProvider: result.adapterId,
            printerState: result.outcome.name,
          );
          throw error;
        }
        _recordPrintDiagnostic(
          ref,
          ticketId,
          automatic,
          errorCode: 'reprint_fallback_failed',
          message: result.error?.toString(),
          printerProvider: result.adapterId,
          printerState: result.outcome.name,
        );
        return;
      }
    }
  } on ApiException catch (error) {
    _recordPrintDiagnostic(
      ref,
      ticketId,
      automatic,
      errorCode: error.code,
      message: error.message,
      requestId: error.requestId,
      httpStatus: error.statusCode,
      printerState: 'api_error',
    );
    messenger.showSnackBar(
      SnackBar(content: Text(localizedUserError(translations, error))),
    );
  } catch (error) {
    _recordPrintDiagnostic(
      ref,
      ticketId,
      automatic,
      errorCode: 'print_unexpected',
      message: error.toString(),
      exceptionType: error.runtimeType.toString(),
      printerState: 'unexpected_error',
    );
    messenger.showSnackBar(
      SnackBar(content: Text(translations.translate('common.error.unknown'))),
    );
  }
}

void _recordPrintDiagnostic(
  WidgetRef ref,
  String ticketId,
  bool automatic, {
  String? errorCode,
  String? message,
  String? exceptionType,
  String? requestId,
  int? httpStatus,
  String? printerProvider,
  String? printerState,
}) {
  ref
      .read(clientDiagnosticsReporterProvider)
      .record(
        ClientDiagnosticEvent(
          category: ClientDiagnosticCategory.print,
          occurredAtClient: DateTime.now().toUtc(),
          severity: ClientDiagnosticSeverity.error,
          operation: automatic ? 'ticket.print.auto' : 'ticket.print.manual',
          errorCode: errorCode,
          message: message,
          exceptionType: exceptionType,
          requestId: requestId,
          correlationId: ticketId,
          httpStatus: httpStatus,
          endpointKey: '/tenant/cashier/tickets/{ticketId}/print',
          printerProvider: printerProvider,
          printerState: printerState,
          platform: 'android',
        ),
      );
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
      await printTicket(
        context,
        ref,
        ticketId,
        reprintReason: reason,
        manualPdfFallback: true,
      );
    }
  } finally {
    controller.dispose();
    _reprintInFlight.remove(ticketId);
  }
}

ReceiptPaperSize _configuredPaperSize(WidgetRef ref) {
  final configured = ref
      .read(posProfileProvider)
      .asData
      ?.value
      .settings
      .receipt
      .paperSize;
  return switch (configured) {
    'RECEIPT_58MM' => ReceiptPaperSize.receipt58mm,
    'RECEIPT_80MM' => ReceiptPaperSize.receipt80mm,
    _ => ReceiptPaperSize.receipt58mm,
  };
}

String _presetLabelKey(String preset) => switch (preset) {
  _sellerRequestedReprintReason => 'pos.tickets.reprint_default',
  'PRINTER_FAILURE' => 'pos.tickets.reprint_printer_failure',
  'PRINT_ILLEGIBLE' => 'pos.tickets.reprint_illegible',
  'CUSTOMER_REQUEST' => 'pos.tickets.reprint_customer_request',
  _ => 'pos.tickets.reprint_default',
};
