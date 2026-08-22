import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'printer_contracts.dart';

class RawBtPrinterAdapter implements PrinterAdapter {
  const RawBtPrinterAdapter();

  static const _channel = MethodChannel('com.tchalanet.mobile/rawbt_printer');

  @override
  String get id => 'rawbt';

  @override
  PrinterCapability capability() => PrinterCapability(
    adapterId: id,
    available: defaultTargetPlatform == TargetPlatform.android,
    paperSizes: const {
      ReceiptPaperSize.receipt58mm,
      ReceiptPaperSize.receipt80mm,
    },
  );

  Future<bool> isAvailable() async {
    if (defaultTargetPlatform != TargetPlatform.android) return false;
    return await _channel.invokeMethod<bool>('isAvailable') ?? false;
  }

  @override
  Future<void> print(Uint8List bytes) async {
    if (defaultTargetPlatform != TargetPlatform.android) {
      throw StateError('rawbt_platform_unsupported');
    }
    await _channel.invokeMethod<void>('printRaw', {'bytes': bytes});
  }
}
