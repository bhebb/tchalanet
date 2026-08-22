import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';

void main() {
  test('sendReceipt supports SMS delivery requests', () async {
    final adapter = _CaptureSendReceiptAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    await service.sendReceipt('ticket-1', channel: 'SMS', to: '+15145550123');

    expect(adapter.capturedPath, '/tenant/cashier/tickets/ticket-1/send');
    expect(adapter.capturedData, {'channel': 'SMS', 'to': '+15145550123'});
  });

  test('sendReceipt supports email delivery requests', () async {
    final adapter = _CaptureSendReceiptAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    await service.sendReceipt(
      'ticket-1',
      channel: 'EMAIL',
      to: 'client@example.com',
    );

    expect(adapter.capturedPath, '/tenant/cashier/tickets/ticket-1/send');
    expect(adapter.capturedData, {
      'channel': 'EMAIL',
      'to': 'client@example.com',
    });
  });
}

class _CaptureSendReceiptAdapter implements HttpClientAdapter {
  String? capturedPath;
  Map<String, dynamic>? capturedData;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedPath = options.path;
    capturedData = options.data as Map<String, dynamic>;
    return ResponseBody.fromString('', 204);
  }

  @override
  void close({bool force = false}) {}
}
