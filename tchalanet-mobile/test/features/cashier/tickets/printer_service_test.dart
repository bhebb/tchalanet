import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/printing/printer_contracts.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/printing/printer_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/printing/rawbt_printer.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/printing/sunmi_internal_printer.dart';

void main() {
  test('Sunmi declares a fixed 58 mm paper size', () {
    final capability = const SunmiInternalPrinterAdapter().capability();

    expect(capability.adapterId, 'sunmi_internal');
    expect(capability.paperSizes, {ReceiptPaperSize.receipt58mm});
    expect(capability.forcedPaperSize, ReceiptPaperSize.receipt58mm);
  });

  test('RawBT remains a generic Android ESC/POS adapter', () {
    final capability = const RawBtPrinterAdapter().capability();

    expect(capability.adapterId, 'rawbt');
    expect(capability.paperSizes, {
      ReceiptPaperSize.receipt58mm,
      ReceiptPaperSize.receipt80mm,
    });
    expect(capability.forcedPaperSize, isNull);
  });

  test(
    'generic adapters use the paper size requested by terminal params',
    () async {
      final tickets = _FakeCashierTicketService();
      final rawBtLike = _FakePrinterAdapter(
        id: 'rawbt_like',
        paperSizes: const {
          ReceiptPaperSize.receipt58mm,
          ReceiptPaperSize.receipt80mm,
        },
      );
      final service = PrinterService(tickets, directAdapters: [rawBtLike]);

      final result = await service.printTicket(
        'ticket-1',
        paperSize: ReceiptPaperSize.receipt80mm,
      );

      expect(result.outcome, PrintOutcome.printed);
      expect(rawBtLike.printedPayloads, [
        Uint8List.fromList([80]),
      ]);
      expect(tickets.calls, [
        const _PrintCall(recordPrint: false, paperSize: 'RECEIPT_80MM'),
        const _PrintCall(recordPrint: true, paperSize: 'RECEIPT_80MM'),
      ]);
    },
  );

  test(
    'falls back to the next adapter without reusing the wrong paper size',
    () async {
      final tickets = _FakeCashierTicketService();
      final sunmiLike = _FakePrinterAdapter(
        id: 'sunmi_like',
        paperSizes: const {ReceiptPaperSize.receipt58mm},
        forcedPaperSize: ReceiptPaperSize.receipt58mm,
        error: StateError('sunmi_failed'),
      );
      final rawBtLike = _FakePrinterAdapter(
        id: 'rawbt_like',
        paperSizes: const {
          ReceiptPaperSize.receipt58mm,
          ReceiptPaperSize.receipt80mm,
        },
      );
      final service = PrinterService(
        tickets,
        directAdapters: [sunmiLike, rawBtLike],
      );

      final result = await service.printTicket(
        'ticket-1',
        paperSize: ReceiptPaperSize.receipt80mm,
      );

      expect(result.outcome, PrintOutcome.printed);
      expect(result.adapterId, 'rawbt_like');
      expect(sunmiLike.printedPayloads, [
        Uint8List.fromList([58]),
      ]);
      expect(rawBtLike.printedPayloads, [
        Uint8List.fromList([80]),
      ]);
      expect(tickets.calls, [
        const _PrintCall(recordPrint: false, paperSize: 'RECEIPT_58MM'),
        const _PrintCall(recordPrint: false, paperSize: 'RECEIPT_80MM'),
        const _PrintCall(recordPrint: true, paperSize: 'RECEIPT_80MM'),
      ]);
    },
  );

  test('does not retry physical printing when audit recording fails', () async {
    final tickets = _FakeCashierTicketService(failRecordPrint: true);
    final adapter = _FakePrinterAdapter(
      id: 'rawbt_like',
      paperSizes: const {ReceiptPaperSize.receipt80mm},
    );
    final service = PrinterService(tickets, directAdapters: [adapter]);

    final result = await service.printTicket(
      'ticket-1',
      paperSize: ReceiptPaperSize.receipt80mm,
    );

    expect(result.outcome, PrintOutcome.printed);
    expect(adapter.printedPayloads.length, 1);
    expect(tickets.calls, [
      const _PrintCall(recordPrint: false, paperSize: 'RECEIPT_80MM'),
      const _PrintCall(recordPrint: true, paperSize: 'RECEIPT_80MM'),
    ]);
  });
}

class _FakeCashierTicketService extends CashierTicketService {
  _FakeCashierTicketService({this.failRecordPrint = false}) : super(Dio());

  final bool failRecordPrint;
  final calls = <_PrintCall>[];

  @override
  Future<Uint8List> print(
    String ticketId, {
    bool recordPrint = true,
    String? reprintReason,
    String outputFormat = 'PDF',
    String paperSize = 'A4',
    String? buyerLocale,
  }) async {
    calls.add(_PrintCall(recordPrint: recordPrint, paperSize: paperSize));
    if (recordPrint && failRecordPrint) {
      throw StateError('record_print_failed');
    }
    return switch (paperSize) {
      'RECEIPT_58MM' => Uint8List.fromList([58]),
      'RECEIPT_80MM' => Uint8List.fromList([80]),
      _ => Uint8List.fromList([4]),
    };
  }
}

class _FakePrinterAdapter implements PrinterAdapter {
  _FakePrinterAdapter({
    required this.id,
    required this.paperSizes,
    this.forcedPaperSize,
    this.error,
  });

  @override
  final String id;
  final Set<ReceiptPaperSize> paperSizes;
  final ReceiptPaperSize? forcedPaperSize;
  final Object? error;
  final printedPayloads = <Uint8List>[];

  @override
  PrinterCapability capability() => PrinterCapability(
    adapterId: id,
    available: true,
    paperSizes: paperSizes,
    forcedPaperSize: forcedPaperSize,
  );

  @override
  Future<void> print(Uint8List bytes) async {
    printedPayloads.add(bytes);
    final failure = error;
    if (failure != null) throw failure;
  }
}

class _PrintCall {
  const _PrintCall({required this.recordPrint, required this.paperSize});

  final bool recordPrint;
  final String paperSize;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is _PrintCall &&
          other.recordPrint == recordPrint &&
          other.paperSize == paperSize;

  @override
  int get hashCode => Object.hash(recordPrint, paperSize);

  @override
  String toString() =>
      '_PrintCall(recordPrint: $recordPrint, paperSize: $paperSize)';
}
