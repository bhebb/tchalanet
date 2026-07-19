import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/services/terminal_stats_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';

void main() {
  test('prepare response accepts a persisted preparation', () {
    final response = CashierTicketPreviewResponse.fromJson({
      'preparationId': 'preparation-1',
      'status': 'PREPARED',
      'currency': 'HTG',
      'totalAmount': 15.0,
      'lines': const [],
      'promotionLines': const [],
      'notices': [
        {'code': 'promotion.decision.applied', 'severity': 'INFO'},
      ],
    });

    expect(response.preparationId, 'preparation-1');
    expect(response.isAccepted, isTrue);
    expect(response.notices.single.code, 'promotion.decision.applied');
  });

  test('prepare response retains generated Maryaj gratis lines', () {
    final response = CashierTicketPreviewResponse.fromJson({
      'preparationId': 'preparation-1',
      'promotionLines': [
        {
          'gameCode': 'MARYAJ',
          'selection': '12-21',
          'selectionSource': 'PROMOTION_GENERATED',
          'choiceMode': 'AUTO_GENERATE',
        },
      ],
    });

    final line = response.promotionLines.single;
    expect(line.isMaryaj, isTrue);
    expect(line.isGenerated, isTrue);
    expect(line.selection, '12-21');
  });

  test('confirm response reads the nested sale ticket', () {
    final response = CashierSellTicketResponse.fromJson({
      'preparationId': 'preparation-1',
      'ticketId': 'ticket-1',
      'alreadyConfirmed': false,
      'sale': {
        'outcome': 'SOLD',
        'ticket': {
          'ticketId': 'ticket-1',
          'ticketCode': 'TCK-1',
          'publicCode': 'PUB-1',
          'saleStatus': 'APPROVED',
        },
      },
    });

    expect(response.ticketId, 'ticket-1');
    expect(response.ticketCode, 'TCK-1');
    expect(response.publicCode, 'PUB-1');
    expect(response.outcome, 'SOLD');
  });

  test('idempotent confirm replay preserves the ticket identifier', () {
    final response = CashierSellTicketResponse.fromJson({
      'preparationId': 'preparation-1',
      'ticketId': 'ticket-1',
      'alreadyConfirmed': true,
      'sale': null,
    });

    expect(response.ticketId, 'ticket-1');
    expect(response.outcome, 'CONFIRMED');
    expect(response.ticketCode, isEmpty);
  });

  test('available draw retains provider and tenant-local schedules', () {
    final draw = CashierAvailableDrawView.fromJson({
      'drawId': 'draw-1',
      'channelLabel': 'Georgia - Evening',
      'gameCodes': const ['HT_BOLET'],
      'status': 'OPEN',
      'scheduledAt': '2026-07-19T22:59:00Z',
      'providerDate': '2026-07-19',
      'providerTime': '18:59:00',
      'providerTimezone': 'America/New_York',
      'localDate': '2026-07-19',
      'localTime': '18:59:00',
      'localTimezone': 'America/Port-au-Prince',
    });

    expect(draw.providerScheduleLabel, '2026-07-19 · 18:59 · America/New_York');
    expect(
      draw.localScheduleLabel,
      '2026-07-19 · 18:59 · America/Port-au-Prince',
    );
  });

  test('selection shape follows the configured bet option', () {
    final game = CashierGameOptionResponse.fromJson({
      'gameCode': 'HT_LOTO4',
      'gameLabel': 'Loto 4',
      'betType': 'LOTTO4_PATTERN',
      'betTypeLabel': 'Loto 4',
      'requiresOption': true,
      'selectionDigits': 4,
      'selectionSegments': 1,
      'options': [
        {
          'code': 3,
          'label': 'Premye de chif',
          'selectionDigits': 2,
          'selectionSegments': 1,
        },
      ],
    });

    expect(game.selectionShapeFor(null).digits, 4);
    expect(game.selectionShapeFor(3).digits, 2);
  });

  test('daily terminal stats retain the ticket commission snapshot total', () {
    final stats = TerminalDailyStats.fromJson({
      'ticketCount': 3,
      'salesTotalCents': 12500,
      'sellerCommissionTotalCents': 1625,
      'currency': 'HTG',
    });

    expect(stats.ticketCount, 3);
    expect(stats.salesTotalCents, 12500);
    expect(stats.sellerCommissionTotalCents, 1625);
  });
}
