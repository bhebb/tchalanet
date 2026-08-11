import 'dart:typed_data';

import 'package:flutter_blue_plus/flutter_blue_plus.dart' as ble;
import 'package:flutter_bluetooth_serial_plus/flutter_bluetooth_serial_plus.dart'
    as classic;
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'printer_contracts.dart';

class BluetoothPrinterDevice {
  const BluetoothPrinterDevice({
    required this.id,
    required this.name,
    required this.transport,
    this.bleDevice,
  });

  final String id;
  final String name;
  final String transport;
  final ble.BluetoothDevice? bleDevice;
}

/// Local Bluetooth state. The server stores the terminal policy only; pairing
/// stays on this app installation.
class BluetoothEscPosPrinterRepository {
  static const _deviceIdKey = 'pos.receipt.bluetooth.device_id';
  static const _deviceNameKey = 'pos.receipt.bluetooth.device_name';
  static const _deviceTransportKey = 'pos.receipt.bluetooth.transport';

  ble.BluetoothDevice? _selectedBle;
  ble.BluetoothCharacteristic? _writeCharacteristic;
  classic.BluetoothConnection? _selectedClassic;

  Future<List<BluetoothPrinterDevice>> scan({
    Duration timeout = const Duration(seconds: 6),
  }) async {
    await _ensurePermissions();
    final found = <String, BluetoothPrinterDevice>{};

    if (await ble.FlutterBluePlus.adapterState.first ==
        ble.BluetoothAdapterState.on) {
      final subscription = ble.FlutterBluePlus.scanResults.listen((results) {
        for (final result in results) {
          final name = result.device.platformName.trim();
          final advertisedName = result.advertisementData.advName.trim();
          if (name.isEmpty && advertisedName.isEmpty) continue;
          found['ble:${result.device.remoteId.str}'] = BluetoothPrinterDevice(
            id: result.device.remoteId.str,
            name: name.isEmpty ? advertisedName : name,
            transport: 'BLE',
            bleDevice: result.device,
          );
        }
      });
      try {
        await ble.FlutterBluePlus.startScan(timeout: timeout);
        await Future<void>.delayed(timeout);
      } finally {
        await ble.FlutterBluePlus.stopScan();
        await subscription.cancel();
      }
    }

    // Classic SPP printers must be paired in Android system settings first.
    try {
      final bonded = await classic.FlutterBluetoothSerial.instance
          .getBondedDevices();
      for (final device in bonded) {
        final address = device.address;
        if (address.isEmpty) continue;
        found['classic:$address'] = BluetoothPrinterDevice(
          id: address,
          name: device.name?.trim().isNotEmpty == true
              ? device.name!.trim()
              : address,
          transport: 'CLASSIC',
        );
      }
    } catch (_) {
      // BLE discovery remains available on platforms without classic SPP.
    }

    return found.values.toList()
      ..sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
  }

  Future<BluetoothPrinterDevice?> selected() async {
    final prefs = await SharedPreferences.getInstance();
    final id = prefs.getString(_deviceIdKey);
    if (id == null || id.isEmpty) return null;
    final transport = prefs.getString(_deviceTransportKey) ?? 'BLE';
    return BluetoothPrinterDevice(
      id: id,
      name: prefs.getString(_deviceNameKey) ?? id,
      transport: transport,
      bleDevice: transport == 'BLE' ? ble.BluetoothDevice.fromId(id) : null,
    );
  }

  Future<void> select(BluetoothPrinterDevice printer) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_deviceIdKey, printer.id);
    await prefs.setString(_deviceNameKey, printer.name);
    await prefs.setString(_deviceTransportKey, printer.transport);
    _selectedBle = printer.bleDevice;
    _writeCharacteristic = null;
  }

  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_deviceIdKey);
    await prefs.remove(_deviceNameKey);
    await prefs.remove(_deviceTransportKey);
    await disconnect();
  }

  Future<void> disconnect() async {
    _writeCharacteristic = null;
    final bleDevice = _selectedBle;
    _selectedBle = null;
    if (bleDevice != null) await bleDevice.disconnect();
    final classicConnection = _selectedClassic;
    _selectedClassic = null;
    await classicConnection?.finish();
  }

  Future<void> write(Uint8List bytes) async {
    final printer = await selected();
    if (printer == null) throw StateError('printer_not_configured');
    if (printer.transport == 'CLASSIC') {
      final connection =
          _selectedClassic ??
          await classic.BluetoothConnection.toAddress(printer.id);
      _selectedClassic = connection;
      const chunkSize = 180;
      for (var offset = 0; offset < bytes.length; offset += chunkSize) {
        final end = (offset + chunkSize).clamp(0, bytes.length);
        connection.output.add(bytes.sublist(offset, end));
        await connection.output.allSent;
      }
      return;
    }

    final device = printer.bleDevice;
    if (device == null) throw StateError('printer_device_unavailable');
    final characteristic = await _characteristicFor(device);
    const chunkSize = 180;
    for (var offset = 0; offset < bytes.length; offset += chunkSize) {
      final end = (offset + chunkSize).clamp(0, bytes.length);
      await characteristic.write(
        bytes.sublist(offset, end),
        withoutResponse: characteristic.properties.writeWithoutResponse,
      );
    }
  }

  Future<ble.BluetoothCharacteristic> _characteristicFor(
    ble.BluetoothDevice device,
  ) async {
    if (_writeCharacteristic != null &&
        _selectedBle?.remoteId == device.remoteId) {
      return _writeCharacteristic!;
    }
    await device.connect(timeout: const Duration(seconds: 8));
    final services = await device.discoverServices();
    for (final service in services) {
      for (final characteristic in service.characteristics) {
        if (characteristic.properties.write ||
            characteristic.properties.writeWithoutResponse) {
          _selectedBle = device;
          _writeCharacteristic = characteristic;
          return characteristic;
        }
      }
    }
    throw StateError('printer_write_characteristic_not_found');
  }

  Future<void> _ensurePermissions() async {
    final statuses = await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.locationWhenInUse,
    ].request();
    if (statuses.values.any(
      (status) => status.isDenied || status.isPermanentlyDenied,
    )) {
      throw StateError('bluetooth_permission_denied');
    }
  }
}

class BluetoothEscPosPrinterAdapter implements PrinterAdapter {
  const BluetoothEscPosPrinterAdapter(this.repository);

  final BluetoothEscPosPrinterRepository repository;

  @override
  String get id => 'bluetooth_esc_pos';

  @override
  PrinterCapability capability() => const PrinterCapability(
    adapterId: 'bluetooth_esc_pos',
    available: true,
    paperSizes: {ReceiptPaperSize.receipt58mm, ReceiptPaperSize.receipt80mm},
  );

  @override
  Future<void> print(Uint8List bytes) => repository.write(bytes);
}
