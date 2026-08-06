import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;

class DrawStatLine {
  const DrawStatLine({
    required this.drawId,
    required this.channelLabel,
    required this.ticketCount,
    required this.totalCents,
    this.winningsCents = 0,
    this.sellerCommissionCents = 0,
    this.netRevenueCents = 0,
  });

  final String drawId;
  final String channelLabel;
  final int ticketCount;
  final int totalCents;

  /// Winnings owed on this draw. Zero until the draw is settled.
  final int winningsCents;
  final int sellerCommissionCents;
  final int netRevenueCents;

  double get totalAmount => totalCents / 100.0;

  factory DrawStatLine.fromJson(Map<String, dynamic> json) => DrawStatLine(
    drawId: json['drawId'] as String? ?? '',
    channelLabel: json['channelLabel'] as String? ?? '',
    ticketCount: (json['ticketCount'] as num?)?.toInt() ?? 0,
    totalCents: (json['totalCents'] as num?)?.toInt() ?? 0,
    // Older API builds omit these; treat a missing figure as zero rather than
    // failing the whole report.
    winningsCents: (json['winningsCents'] as num?)?.toInt() ?? 0,
    sellerCommissionCents:
        (json['sellerCommissionCents'] as num?)?.toInt() ?? 0,
    netRevenueCents: (json['netRevenueCents'] as num?)?.toInt() ?? 0,
  );
}

class TerminalDailyStats {
  const TerminalDailyStats({
    required this.available,
    required this.trustState,
    required this.trustReasonCode,
    required this.ticketCount,
    required this.salesTotalCents,
    required this.sellerCommissionTotalCents,
    required this.currency,
    this.breakdown = const [],
  });

  final bool available;
  final String trustState;
  final String trustReasonCode;
  final int ticketCount;
  final int salesTotalCents;
  final int sellerCommissionTotalCents;
  final String currency;
  final List<DrawStatLine> breakdown;

  String get formattedTotal {
    final amount = salesTotalCents / 100.0;
    return '${amount.toStringAsFixed(2)} $currency';
  }

  factory TerminalDailyStats.fromJson(Map<String, dynamic> json) =>
      TerminalDailyStats(
        available: json['available'] as bool? ?? true,
        trustState: json['trustState'] as String? ?? 'READY',
        trustReasonCode:
            json['trustReasonCode'] as String? ?? 'analytics.trust.ready',
        ticketCount: (json['ticketCount'] as num?)?.toInt() ?? 0,
        salesTotalCents: (json['salesTotalCents'] as num?)?.toInt() ?? 0,
        sellerCommissionTotalCents: _sellerCommissionTotalCents(json),
        currency: json['currency'] as String? ?? 'HTG',
        breakdown:
            (json['breakdown'] as List<dynamic>?)
                ?.map((e) => DrawStatLine.fromJson(e as Map<String, dynamic>))
                .toList() ??
            const [],
      );

  static TerminalDailyStats empty(String currency) => TerminalDailyStats(
    available: false,
    trustState: 'UNAVAILABLE',
    trustReasonCode: 'analytics.trust.unknown',
    ticketCount: 0,
    salesTotalCents: 0,
    sellerCommissionTotalCents: 0,
    currency: currency,
  );
}

class TerminalStatsService {
  const TerminalStatsService(this._dio);

  final Dio _dio;

  Future<TerminalDailyStats> fetchDailyStats({String? date}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/tickets/stats',
        queryParameters: date != null ? {'date': date} : null,
      );
      return TerminalDailyStats.fromJson(
        response.data?['data'] as Map<String, dynamic>? ?? {},
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final terminalStatsServiceProvider = Provider<TerminalStatsService>(
  (ref) => TerminalStatsService(ref.watch(apiClientProvider)),
);

int _sellerCommissionTotalCents(Map<String, dynamic> json) {
  for (final key in const [
    'sellerCommissionTotalCents',
    'sellerCommissionCents',
    'commissionTotalCents',
  ]) {
    final value = json[key];
    if (value is num) return value.toInt();
  }

  final amount = json['sellerCommission'];
  if (amount is num) return (amount * 100).round();
  if (amount is String) {
    final parsed = double.tryParse(amount);
    if (parsed != null) return (parsed * 100).round();
  }

  return 0;
}
