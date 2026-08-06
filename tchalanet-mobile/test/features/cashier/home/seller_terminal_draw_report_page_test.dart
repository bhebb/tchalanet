import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/services/terminal_stats_service.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/views/seller_terminal_draw_report_page.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';

const _translations = I18nBundle(
  locale: 'ht',
  translations: {
    'pos.reports.draw': 'Tiraj',
    'pos.reports.draw_sales': 'Vant tiraj la',
    'pos.reports.sell_this_draw': 'Vann yon tikè',
    'pos.reports.draw_commission': 'Komisyon',
    'pos.reports.draw_winnings': 'Lo pou peye',
    'pos.reports.draw_net_revenue': 'Revni nèt',
    'pos.reports.draw_net_revenue_hint': 'Estimasyon.',
    'pos.reports.draw_empty': 'Pa gen tikè pou tiraj sa a jounen an.',
    'pos.reports.tickets': 'Tikè',
    'pos.reports.load_error': 'Nou pa kapab chaje rapò a.',
    'pos.reports.analytics_unavailable': 'Chif vant yo poko disponib.',
    'common.cashier_stats.tickets': 'Tikè',
    'common.refresh': 'Rafrechi',
    'common.retry': 'Eseye ankò',
  },
);

TerminalDailyStats _statsWith(List<DrawStatLine> breakdown) =>
    TerminalDailyStats(
      available: true,
      trustState: 'READY',
      trustReasonCode: 'analytics.trust.ready',
      ticketCount: 3,
      salesTotalCents: 30000,
      sellerCommissionTotalCents: 3900,
      currency: 'HTG',
      breakdown: breakdown,
    );

CashierAvailableDrawView _openDraw(String drawId) => CashierAvailableDrawView(
  drawId: drawId,
  drawChannelId: 'channel-$drawId',
  channelCode: 'HT_NY_EVE',
  resultSlotKey: 'NY_EVE',
  channelLabel: 'Haïti • New York • Evening',
  gameCodes: const ['HT_BOLET'],
  status: 'OPEN',
  providerDate: '2026-08-05',
  providerTime: '22:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-08-05',
  localTime: '22:00:00',
  localTimezone: 'America/Port-au-Prince',
);

Widget _harness({
  required List<DrawStatLine> breakdown,
  required List<CashierTicketSummaryView> tickets,
  List<CashierAvailableDrawView> openDraws = const [],
  String drawId = 'draw-1',
  String isoDate = '2026-08-05',
  String? drawLabel = 'Nouyòk · Aswè',
}) => ProviderScope(
  overrides: [
    i18nBundleProvider.overrideWithValue(_translations),
    terminalStatsByDateProvider.overrideWith(
      (ref, date) async => _statsWith(breakdown),
    ),
    drawReportTicketsProvider.overrideWith((ref, key) async => tickets),
    availableDrawsProvider.overrideWith((ref) async => openDraws),
  ],
  child: MaterialApp(
    home: SellerTerminalDrawReportPage(
      drawId: drawId,
      isoDate: isoDate,
      drawLabel: drawLabel,
    ),
  ),
);

void main() {
  testWidgets('shows the selected draw totals and its tickets', (tester) async {
    await tester.pumpWidget(
      _harness(
        breakdown: const [
          DrawStatLine(
            drawId: 'draw-1',
            channelLabel: 'Haïti • New York • Evening',
            ticketCount: 2,
            totalCents: 11200,
            winningsCents: 3000,
            sellerCommissionCents: 1456,
            netRevenueCents: 6744,
          ),
          DrawStatLine(
            drawId: 'draw-2',
            channelLabel: 'Haïti • Florida • Midday',
            ticketCount: 1,
            totalCents: 18800,
            winningsCents: 500,
            sellerCommissionCents: 2444,
            netRevenueCents: 15856,
          ),
        ],
        tickets: const [
          CashierTicketSummaryView(
            id: 'ticket-id-1',
            ticketCode: 'TCK-260805-001000-PTC2F4-9',
            status: 'APPROVED',
            totalAmountCents: 5600,
            currency: 'HTG',
            drawId: 'draw-1',
          ),
        ],
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Nouyòk · Aswè'), findsOneWidget);
    expect(find.text('2026-08-05'), findsOneWidget);
    // Every figure comes from the matching breakdown line, never the
    // whole-day totals and never the other draw's line.
    expect(find.text('112.00'), findsOneWidget); // sales
    expect(find.text('14.56'), findsOneWidget); // commission
    expect(find.text('30.00'), findsOneWidget); // winnings
    expect(find.text('67.44'), findsOneWidget); // net revenue
    expect(find.text('188.00'), findsNothing); // draw-2 must not leak in
    expect(find.text('TCK-260805-001000-PTC2F4-9'), findsOneWidget);
  });

  testWidgets('shows an empty state when the draw sold nothing', (
    tester,
  ) async {
    await tester.pumpWidget(_harness(breakdown: const [], tickets: const []));

    await tester.pumpAndSettle();

    // Sales, commission, winnings and net revenue all fall back to zero.
    expect(find.text('0.00'), findsNWidgets(4));
    expect(find.text('Pa gen tikè pou tiraj sa a jounen an.'), findsOneWidget);
  });

  testWidgets('offers a sell action while the draw is still open', (
    tester,
  ) async {
    await tester.pumpWidget(
      _harness(
        breakdown: const [],
        tickets: const [],
        openDraws: [_openDraw('draw-1')],
      ),
    );

    await tester.pumpAndSettle();

    // A mis-tap on the card must not strand the seller: selling stays reachable
    // from the report without navigating back.
    expect(find.byKey(const Key('draw-report-sell-action')), findsOneWidget);
    expect(find.text('Vann yon tikè'), findsOneWidget);
  });

  testWidgets('hides the sell action once the draw is closed', (tester) async {
    await tester.pumpWidget(
      _harness(
        breakdown: const [],
        tickets: const [],
        // availableDrawsProvider only ever carries open draws.
        openDraws: [_openDraw('another-draw')],
      ),
    );

    await tester.pumpAndSettle();

    expect(find.byKey(const Key('draw-report-sell-action')), findsNothing);
  });
}
