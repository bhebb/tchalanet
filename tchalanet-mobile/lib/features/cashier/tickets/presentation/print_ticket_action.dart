import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../../../../core/network/api_exception.dart';
import '../../operationalcontext/data/storage/op_context_storage.dart';
import '../data/services/cashier_ticket_service.dart';

/// Fetches the ticket receipt from the backend (`POST /tickets/{id}/print`,
/// returns the rendered bytes) and opens the native print preview.
///
/// The backend is expected to return a PDF for `deliveryOptions: ['RETURN_FILE']`.
/// Shows a SnackBar on failure. Error text is raw for now (i18n keyed-error
/// contract is deferred until the backend emits message keys).
Future<void> printTicket(
  BuildContext context,
  WidgetRef ref,
  String ticketId,
) async {
  final messenger = ScaffoldMessenger.of(context);
  try {
    final terminalId = await ref.read(opContextStorageProvider).readTerminalId();
    final bytes = await ref
        .read(cashierTicketServiceProvider)
        .print(ticketId, terminalId: terminalId);

    if (bytes.isEmpty) {
      messenger.showSnackBar(
        const SnackBar(content: Text('Reçu vide — rien à imprimer')),
      );
      return;
    }

    await Printing.layoutPdf(onLayout: (_) async => bytes);
  } on ApiException catch (e) {
    messenger.showSnackBar(
      SnackBar(content: Text(e.message)),
    );
  }
}
