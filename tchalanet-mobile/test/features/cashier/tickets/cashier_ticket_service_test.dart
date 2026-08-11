import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_exception.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';

void main() {
  test(
    'verify sends a manually entered public code to the POS endpoint',
    () async {
      final adapter = _CaptureJsonAdapter({
        'data': {
          'status': 'NOT_PAYABLE_PENDING_DRAW',
          'severity': 'INFO',
          'ticketId': 'ticket-1',
          'titleKey': 'pos.ticket.verify.not_payable_pending_draw.title',
          'messageKey': 'pos.ticket.verify.not_payable_pending_draw.message',
          'params': {'displayCode': '75NR-HPD8'},
          'availableActions': <Object>[],
        },
      });
      final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

      final response = await service.verify(
        const CashierVerifyTicketRequest(scannedValue: '75NR-HPD8'),
      );

      expect(adapter.capturedPath, '/tenant/cashier/tickets/verify');
      expect(adapter.capturedData?['scannedValue'], '75NR-HPD8');
      expect(response.status, 'NOT_PAYABLE_PENDING_DRAW');
      expect(response.params?['displayCode'], '75NR-HPD8');
    },
  );

  test('verify sends a QR URL payload unchanged to the POS endpoint', () async {
    final adapter = _CaptureJsonAdapter({
      'data': {
        'status': 'PAYABLE',
        'severity': 'SUCCESS',
        'ticketId': 'ticket-2',
        'titleKey': 'pos.ticket.verify.payable.title',
        'messageKey': 'pos.ticket.verify.payable.message',
        'params': {'displayCode': '75NR-HPD8'},
        'availableActions': <Object>[],
      },
    });
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);
    const scannedValue =
        'https://tickets.test/public/check-ticket?code=75NR-HPD8';

    final response = await service.verify(
      const CashierVerifyTicketRequest(scannedValue: scannedValue),
    );

    expect(adapter.capturedPath, '/tenant/cashier/tickets/verify');
    expect(adapter.capturedData?['scannedValue'], scannedValue);
    expect(response.status, 'PAYABLE');
    expect(response.severity, 'SUCCESS');
  });

  test('print sends a non-empty reprint reason to the server', () async {
    final adapter = _CapturePrintAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    final bytes = await service.print(
      'ticket-1',
      reprintReason: ' SELLER_REQUESTED_REPRINT ',
      buyerLocale: ' ht ',
    );

    expect(bytes, Uint8List.fromList([1, 2, 3]));
    expect(adapter.capturedPath, '/tenant/cashier/tickets/ticket-1/print');
    expect(adapter.capturedData?['recordPrint'], isTrue);
    expect(adapter.capturedData?['reprintReason'], 'SELLER_REQUESTED_REPRINT');
    expect(adapter.capturedData?['buyerLocale'], 'ht');
    expect(adapter.capturedData?['deliveryOptions'], ['RETURN_FILE']);
  });

  test('print omits a blank reprint reason', () async {
    final adapter = _CapturePrintAdapter();
    final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

    await service.print('ticket-1', reprintReason: '   ');

    expect(adapter.capturedData?.containsKey('reprintReason'), isFalse);
  });

  test(
    'prepare maps shared validation fixture to stable ApiException fields',
    () async {
      final adapter = _ProblemDetailAdapter(
        _contractFixture('problem-details/validation-failed.json'),
        statusCode: 400,
      );
      final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

      await expectLater(
        () => service.prepare(
          const CashierTicketPreviewRequest(
            drawId: 'draw-1',
            drawChannelId: 'channel-1',
            currency: 'HTG',
            lines: [
              CashierTicketLineRequest(
                lineNumber: 1,
                gameCode: 'HT_BOLET',
                betType: 'MATCH_1_2D',
                selection: '42',
                stakeAmount: 10,
              ),
            ],
          ),
        ),
        throwsA(
          isA<ApiException>()
              .having((e) => e.code, 'code', 'validation.failed')
              .having((e) => e.category, 'category', 'validation')
              .having((e) => e.retryPolicy, 'retryPolicy', 'AFTER_USER_ACTION')
              .having(
                (e) => e.requestId,
                'requestId',
                'req_contract_validation',
              )
              .having((e) => e.message, 'message', 'Erreur serveur'),
        ),
      );
      expect(adapter.capturedPath, '/tenant/sales/preparations');
    },
  );

  test(
    'confirm/sell maps shared access denied fixture to stable ApiException fields',
    () async {
      final adapter = _ProblemDetailAdapter(
        _contractFixture('problem-details/access-denied.json'),
        statusCode: 403,
      );
      final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

      await expectLater(
        () => service.confirm('prep-1', idempotencyKey: 'idem-1'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.statusCode, 'statusCode', 403)
              .having((e) => e.code, 'code', 'access.denied')
              .having((e) => e.category, 'category', 'access_denied')
              .having((e) => e.retryable, 'retryable', isFalse)
              .having((e) => e.errorId, 'errorId', 'err_contract_denied'),
        ),
      );
      expect(adapter.capturedPath, '/tenant/sales/preparations/prep-1/confirm');
      expect(adapter.capturedHeaders['Idempotency-Key'], 'idem-1');
    },
  );

  test(
    'print decodes shared ProblemDetail even when success responseType is bytes',
    () async {
      final adapter = _ProblemDetailAdapter(
        _contractFixture('problem-details/validation-failed.json'),
        statusCode: 400,
      );
      final service = CashierTicketService(Dio()..httpClientAdapter = adapter);

      await expectLater(
        () => service.print('ticket-1', reprintReason: 'SELLER_REPRINT'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.code, 'code', 'validation.failed')
              .having(
                (e) => e.requestId,
                'requestId',
                'req_contract_validation',
              )
              .having((e) => e.category, 'category', 'validation'),
        ),
      );
      expect(adapter.capturedPath, '/tenant/cashier/tickets/ticket-1/print');
      expect(adapter.capturedResponseType, ResponseType.bytes);
    },
  );
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

class _CaptureJsonAdapter implements HttpClientAdapter {
  _CaptureJsonAdapter(this.response);

  final Map<String, dynamic> response;
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
    return ResponseBody.fromString(
      jsonEncode(response),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _ProblemDetailAdapter implements HttpClientAdapter {
  _ProblemDetailAdapter(this.problem, {required this.statusCode});

  final Map<String, dynamic> problem;
  final int statusCode;
  String? capturedPath;
  ResponseType? capturedResponseType;
  Map<String, dynamic> capturedHeaders = const {};

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedPath = options.path;
    capturedResponseType = options.responseType;
    capturedHeaders = Map<String, dynamic>.from(options.headers);
    return ResponseBody.fromString(
      jsonEncode(problem),
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Map<String, dynamic> _contractFixture(String relativePath) {
  final file = File(
    '../tchalanet-server/testing/contracts/error-contract/v1/$relativePath',
  );
  return jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
}
