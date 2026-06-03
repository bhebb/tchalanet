import 'dart:math';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/cashier_ticket_models.dart';

class CashierTicketService {
  const CashierTicketService(this._dio);

  final Dio _dio;

  /// Read-only sell acceptance check — no ticket created.
  Future<CashierTicketPreviewResponse> preview(
      CashierTicketPreviewRequest request) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tenant/cashier/tickets/preview',
        data: request.toJson(),
      );
      return CashierTicketPreviewResponse.fromJson(
          response.data!['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Idempotent sale. Pass a stable [idempotencyKey] per attempt (UUID v4).
  /// Generate once per sell flow; reuse on retry to avoid double-sell.
  Future<CashierSellTicketResponse> sell(
    CashierSellTicketRequest request, {
    required String idempotencyKey,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tenant/cashier/tickets/sell',
        data: request.toJson(),
        options: Options(headers: {'Idempotency-Key': idempotencyKey}),
      );
      return CashierSellTicketResponse.fromJson(
          response.data!['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Verify a scanned public code or URL for payout readiness.
  Future<CashierTicketVerificationResponse> verify(
      CashierVerifyTicketRequest request) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tenant/cashier/tickets/verify',
        data: request.toJson(),
      );
      return CashierTicketVerificationResponse.fromJson(
          response.data!['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Print a ticket. Returns raw bytes (PDF or ESC/POS).
  Future<Uint8List> print(
    String ticketId, {
    String? terminalId,
    bool recordPrint = true,
  }) async {
    try {
      final response = await _dio.post<List<int>>(
        '/tenant/cashier/tickets/$ticketId/print',
        data: {
          'terminalId': terminalId,
          'recordPrint': recordPrint,
          'deliveryOptions': ['RETURN_FILE'],
        },
        options: Options(responseType: ResponseType.bytes),
      );
      return Uint8List.fromList(response.data ?? []);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<CashierTicketDetailsView> getDetails(String ticketId) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/tickets/$ticketId',
      );
      return CashierTicketDetailsView.fromJson(
          response.data!['data'] as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<List<CashierTicketSummaryView>> listRecent({int size = 20}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/tickets',
        queryParameters: {
          'size': size,
          'sort': 'createdAt,desc',
        },
      );
      final items =
          (response.data?['data']?['items'] as List<dynamic>?) ?? [];
      return items
          .map((e) =>
              CashierTicketSummaryView.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Send a ticket receipt to the buyer via SMS, WhatsApp, or email.
  Future<void> sendReceipt(
    String ticketId, {
    required String deliveryMode, // SMS | WHATSAPP | EMAIL
    String? phoneNumber,
    String? email,
  }) async {
    try {
      await _dio.post<void>(
        '/tenant/cashier/tickets/$ticketId/send',
        data: {
          'deliveryOptions': [deliveryMode],
          'buyerPhoneNumber': phoneNumber,
          'buyerEmail': email,
        },
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Cancel a ticket within the cancel window.
  Future<void> cancel(String ticketId, {String? reason}) async {
    try {
      await _dio.post<void>(
        '/tenant/cashier/tickets/$ticketId/cancel',
        data: {'reason': reason ?? 'Annulation caissier'},
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Generate a fresh idempotency key (UUID v4) for a new sell attempt.
  static String newIdempotencyKey() {
    final rand = Random.secure();
    final bytes = List.generate(16, (_) => rand.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final h = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
  }
}

final cashierTicketServiceProvider = Provider<CashierTicketService>(
  (ref) => CashierTicketService(ref.watch(apiClientProvider)),
);
