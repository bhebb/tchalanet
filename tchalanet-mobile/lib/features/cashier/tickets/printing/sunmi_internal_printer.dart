import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'printer_contracts.dart';

class SunmiInternalPrinterAdapter implements PrinterAdapter {
  const SunmiInternalPrinterAdapter();

  /// Supports Sunmi generations that expose the Woyou printer service, instead
  /// of coupling the app to one hardware model such as V2 Pro.
  static const _channel = MethodChannel('com.tchalanet.mobile/sunmi_printer');

  @override
  String get id => 'sunmi_internal';

  @override
  PrinterCapability capability() => PrinterCapability(
    adapterId: id,
    available: defaultTargetPlatform == TargetPlatform.android,
    paperSizes: const {ReceiptPaperSize.receipt58mm},
    forcedPaperSize: ReceiptPaperSize.receipt58mm,
  );

  Future<bool> isAvailable() async {
    if (defaultTargetPlatform != TargetPlatform.android) return false;
    return await _channel.invokeMethod<bool>('isAvailable') ?? false;
  }

  @override
  Future<void> print(Uint8List bytes) async {
    if (defaultTargetPlatform != TargetPlatform.android) {
      throw StateError('sunmi_printer_platform_unsupported');
    }
    await _channel.invokeMethod<void>('printRaw', {'bytes': bytes});
  }
}

final sunmiInternalPrinterAvailableProvider = FutureProvider.autoDispose<bool>(
  (ref) => const SunmiInternalPrinterAdapter().isAvailable(),
);
