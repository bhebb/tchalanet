import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../data/services/cashier_ticket_service.dart';
import 'bluetooth_esc_pos_printer.dart';

enum PrinterMode { auto, posDirect, systemPdf }

enum ReceiptPaperSize { receipt58mm, receipt80mm, a4 }

enum PrintOutcome { printed, unavailable, failed }

class PrinterCapability {
  const PrinterCapability({
    required this.adapterId,
    required this.available,
    this.paperSizes = const {},
    this.reason,
  });

  final String adapterId;
  final bool available;
  final Set<ReceiptPaperSize> paperSizes;
  final String? reason;
}

class PrintResult {
  const PrintResult({required this.outcome, this.adapterId, this.error});

  final PrintOutcome outcome;
  final String? adapterId;
  final Object? error;

  bool get isPrinted => outcome == PrintOutcome.printed;
}

class PrinterDiagnostic {
  const PrinterDiagnostic({
    required this.adapterId,
    required this.outcome,
    this.reason,
  });

  final String adapterId;
  final PrintOutcome outcome;
  final String? reason;
}

abstract interface class PrinterAdapter {
  String get id;

  PrinterCapability capability();

  Future<void> print(Uint8List bytes);
}

/// The system PDF surface is deliberately opt-in. It is not a valid automatic
/// POS adapter because it replaces the Tchalanet screen on Android.
class SystemPdfPrintAdapter implements PrinterAdapter {
  const SystemPdfPrintAdapter();

  @override
  String get id => 'system_pdf';

  @override
  PrinterCapability capability() => const PrinterCapability(
    adapterId: 'system_pdf',
    available: true,
    paperSizes: {
      ReceiptPaperSize.receipt58mm,
      ReceiptPaperSize.receipt80mm,
      ReceiptPaperSize.a4,
    },
  );

  @override
  Future<void> print(Uint8List bytes) =>
      Printing.layoutPdf(onLayout: (_) async => bytes);
}

class PrinterService {
  const PrinterService(
    this._tickets, {
    this.directAdapters = const [],
    this.pdfAdapter = const SystemPdfPrintAdapter(),
  });

  final CashierTicketService _tickets;
  final List<PrinterAdapter> directAdapters;
  final PrinterAdapter pdfAdapter;

  Future<PrintResult> printTicket(
    String ticketId, {
    PrinterMode mode = PrinterMode.auto,
    ReceiptPaperSize paperSize = ReceiptPaperSize.receipt80mm,
    bool allowManualPdfFallback = false,
    bool recordPrint = true,
    String? reprintReason,
  }) async {
    final direct = _selectDirectAdapter(paperSize);
    if (mode != PrinterMode.systemPdf && direct != null) {
      try {
        final bytes = await _tickets.print(
          ticketId,
          recordPrint: recordPrint,
          reprintReason: reprintReason,
          outputFormat: 'ESC_POS',
          paperSize: _paperSizeCode(paperSize),
        );
        if (bytes.isEmpty) {
          return const PrintResult(
            outcome: PrintOutcome.failed,
            error: 'empty_print_document',
          );
        }
        await direct.print(bytes);
        return PrintResult(outcome: PrintOutcome.printed, adapterId: direct.id);
      } catch (error) {
        if (!allowManualPdfFallback) {
          return PrintResult(
            outcome: PrintOutcome.failed,
            adapterId: direct.id,
            error: error,
          );
        }
      }
    }

    if (mode == PrinterMode.systemPdf || allowManualPdfFallback) {
      try {
        final bytes = await _tickets.print(
          ticketId,
          recordPrint: recordPrint,
          reprintReason: reprintReason,
          outputFormat: 'PDF',
          paperSize: paperSize == ReceiptPaperSize.a4
              ? 'A4'
              : _paperSizeCode(paperSize),
        );
        if (bytes.isEmpty) {
          return const PrintResult(
            outcome: PrintOutcome.failed,
            adapterId: 'system_pdf',
            error: 'empty_print_document',
          );
        }
        await pdfAdapter.print(bytes);
        return const PrintResult(
          outcome: PrintOutcome.printed,
          adapterId: 'system_pdf',
        );
      } catch (error) {
        return PrintResult(
          outcome: PrintOutcome.failed,
          adapterId: 'system_pdf',
          error: error,
        );
      }
    }

    return const PrintResult(outcome: PrintOutcome.unavailable);
  }

  PrinterAdapter? _selectDirectAdapter(ReceiptPaperSize paperSize) {
    for (final adapter in directAdapters) {
      final capability = adapter.capability();
      if (capability.available && capability.paperSizes.contains(paperSize)) {
        return adapter;
      }
    }
    return null;
  }

  static String _paperSizeCode(ReceiptPaperSize value) => switch (value) {
    ReceiptPaperSize.receipt58mm => 'RECEIPT_58MM',
    ReceiptPaperSize.receipt80mm => 'RECEIPT_80MM',
    ReceiptPaperSize.a4 => 'A4',
  };
}

final printerServiceProvider = Provider<PrinterService>(
  (ref) => PrinterService(
    ref.watch(cashierTicketServiceProvider),
    directAdapters: [
      BluetoothEscPosPrinterAdapter(
        ref.watch(bluetoothPrinterRepositoryProvider),
      ),
    ],
  ),
);

final bluetoothPrinterRepositoryProvider =
    Provider<BluetoothEscPosPrinterRepository>(
      (ref) => BluetoothEscPosPrinterRepository(),
    );
