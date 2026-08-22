import 'dart:typed_data';

enum PrinterMode { auto, posDirect, systemPdf }

enum ReceiptPaperSize { receipt58mm, receipt80mm, a4 }

enum PrintOutcome { printed, unavailable, failed }

class PrinterCapability {
  const PrinterCapability({
    required this.adapterId,
    required this.available,
    this.paperSizes = const {},
    this.forcedPaperSize,
    this.reason,
  });

  final String adapterId;
  final bool available;
  final Set<ReceiptPaperSize> paperSizes;

  /// Use when the hardware has a fixed roll width, for example Sunmi internal
  /// printers that should always receive 58 mm receipts.
  final ReceiptPaperSize? forcedPaperSize;
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
