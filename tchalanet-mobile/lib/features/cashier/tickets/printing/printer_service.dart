import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../data/services/cashier_ticket_service.dart';
import 'bluetooth_esc_pos_printer.dart';
import 'printer_contracts.dart';
import 'rawbt_printer.dart';
import 'sunmi_internal_printer.dart';

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
    String? buyerLocale,
  }) async {
    final directAdapters = _selectDirectAdapters(paperSize);
    if (mode != PrinterMode.systemPdf && directAdapters.isNotEmpty) {
      final renderedByPaperSize = <ReceiptPaperSize, Uint8List>{};
      PrintResult? lastFailure;

      for (final direct in directAdapters) {
        final adapterPaperSize = _paperSizeForAdapter(direct, paperSize);
        try {
          final bytes = await _renderEscPosTicket(
            renderedByPaperSize,
            ticketId,
            paperSize: adapterPaperSize,
            reprintReason: reprintReason,
            buyerLocale: buyerLocale,
          );
          await direct.print(bytes);
          if (recordPrint) {
            try {
              await _tickets.print(
                ticketId,
                recordPrint: true,
                reprintReason: reprintReason,
                buyerLocale: buyerLocale,
                outputFormat: 'ESC_POS',
                paperSize: _paperSizeCode(adapterPaperSize),
              );
            } catch (_) {
              // The receipt already reached the local device. Do not retry
              // physical printing or surface a failure only because audit
              // recording needs a reprint reason or the server is transiently
              // unavailable.
            }
          }
          return PrintResult(
            outcome: PrintOutcome.printed,
            adapterId: direct.id,
          );
        } catch (error) {
          lastFailure = PrintResult(
            outcome: PrintOutcome.failed,
            adapterId: direct.id,
            error: error,
          );
        }
      }

      if (lastFailure != null && !allowManualPdfFallback) {
        return lastFailure;
      }
    }

    if (mode == PrinterMode.systemPdf || allowManualPdfFallback) {
      try {
        final bytes = await _tickets.print(
          ticketId,
          recordPrint: recordPrint,
          reprintReason: reprintReason,
          buyerLocale: buyerLocale,
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

  List<PrinterAdapter> _selectDirectAdapters(ReceiptPaperSize paperSize) {
    final adapters = <PrinterAdapter>[];
    for (final adapter in directAdapters) {
      final capability = adapter.capability();
      final adapterPaperSize = capability.forcedPaperSize ?? paperSize;
      if (capability.available &&
          capability.paperSizes.contains(adapterPaperSize)) {
        adapters.add(adapter);
      }
    }
    return adapters;
  }

  ReceiptPaperSize _paperSizeForAdapter(
    PrinterAdapter adapter,
    ReceiptPaperSize requestedPaperSize,
  ) {
    final capability = adapter.capability();
    return capability.forcedPaperSize ?? requestedPaperSize;
  }

  Future<Uint8List> _renderEscPosTicket(
    Map<ReceiptPaperSize, Uint8List> cache,
    String ticketId, {
    required ReceiptPaperSize paperSize,
    String? reprintReason,
    String? buyerLocale,
  }) async {
    final cached = cache[paperSize];
    if (cached != null) return cached;
    final bytes = await _tickets.print(
      ticketId,
      recordPrint: false,
      reprintReason: reprintReason,
      buyerLocale: buyerLocale,
      outputFormat: 'ESC_POS',
      paperSize: _paperSizeCode(paperSize),
    );
    if (bytes.isEmpty) {
      throw StateError('empty_print_document');
    }
    cache[paperSize] = bytes;
    return bytes;
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
      const SunmiInternalPrinterAdapter(),
      const RawBtPrinterAdapter(),
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
