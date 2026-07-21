import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';

void main() {
  test('print sends a non-empty reprint reason to the server', () async {
    final adapter = _CapturePrintAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    final bytes = await service.print(
      'ticket-1',
      reprintReason: ' SELLER_REQUESTED_REPRINT ',
    );

    expect(bytes, Uint8List.fromList([1, 2, 3]));
    expect(adapter.capturedPath, '/tenant/cashier/tickets/ticket-1/print');
    expect(adapter.capturedData?['recordPrint'], isTrue);
    expect(adapter.capturedData?['reprintReason'], 'SELLER_REQUESTED_REPRINT');
    expect(adapter.capturedData?['deliveryOptions'], ['RETURN_FILE']);
  });

  test('print omits a blank reprint reason', () async {
    final adapter = _CapturePrintAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    await service.print('ticket-1', reprintReason: '   ');

    expect(adapter.capturedData?.containsKey('reprintReason'), isFalse);
  });
}

class _CapturePrintAdapter implements HttpClientAdapter {
  String? capturedPath;
  Map<String, dynamic>? capturedData;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedPath = options.path;
    final requestBytes = <int>[];
    if (requestStream != null) {
      await for (final chunk in requestStream) {
        requestBytes.addAll(chunk);
      }
    }
    capturedData =
        jsonDecode(utf8.decode(requestBytes)) as Map<String, dynamic>;
    return ResponseBody.fromBytes(
      [1, 2, 3],
      200,
      headers: {
        Headers.contentTypeHeader: ['application/pdf'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
