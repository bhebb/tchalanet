import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/services/terminal_stats_service.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/views/seller_terminal_stats_page.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';

class _FakeCashierTicketService extends CashierTicketService {
  _FakeCashierTicketService() : super(Dio());

  @override
  Future<List<CashierTicketSummaryView>> listRecent({
    int size = 20,
    String? query,
    DateTime? fromDate,
    DateTime? toDate,
    String? drawId,
  }) async => const [
    CashierTicketSummaryView(
      id: 'ticket-id-1',
      ticketCode: 'TCK-260721-001000-PTC2F4-9',
      status: 'APPROVED',
      totalAmountCents: 11200,
      currency: 'HTG',
      resultProvider: 'NY',
      resultSlotKey: 'NY_EVE',
      drawChannelName: 'Haïti • New York • Evening',
    ),
  ];
}

void main() {
  testWidgets(
    'reports hide technical draw ids when a draw label is available',
    (tester) async {
      const drawId = 'draw-1';
      const drawChannelId = '2e07c4c7-a37d-4da4-9a69-f34a2706bc36';

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            i18nBundleProvider.overrideWithValue(
              const I18nBundle(
                locale: 'ht',
                translations: {
                  'pos.reports.title': 'Rapò vandè',
                  'pos.reports.today': 'Jodi a',
                  'pos.reports.yesterday': 'Yè',
                  'pos.reports.by_draw': 'Pa tiraj',
                  'pos.reports.all': 'Tout',
                  'pos.reports.pick_date': 'Chwazi yon dat',
                  'pos.reports.other_date': 'Yon lòt dat',
                  'pos.reports.draw': 'Tiraj',
                  'pos.dashboard.home': 'Akèy',
                  'pos.dashboard.history': 'Istorik',
                  'pos.dashboard.results': 'Rezilta',
                  'pos.dashboard.reports': 'Rapò',
                  'common.cashier_stats.total': 'Total',
                  'common.cashier_stats.sales_today': 'Vant jodi a',
                  'common.cashier_stats.tickets': 'Tikè',
                  'common.cashier_stats.commission_today': 'Komisyon jodi a',
                  'pos.draw.providers.ny': 'Nouyòk',
                  'pos.draw.slots.evening': 'Aswè',
                },
              ),
            ),
            terminalStatsByDateProvider.overrideWith(
              (ref, date) async => const TerminalDailyStats(
                available: true,
                trustState: 'READY',
                trustReasonCode: 'analytics.trust.ready',
                ticketCount: 1,
                salesTotalCents: 11200,
                sellerCommissionTotalCents: 1456,
                currency: 'HTG',
                breakdown: [
                  DrawStatLine(
                    drawId: drawId,
                    channelLabel: drawChannelId,
                    ticketCount: 1,
                    totalCents: 11200,
                  ),
                ],
              ),
            ),
            availableDrawsProvider.overrideWith(
              (ref) async => const [
                CashierAvailableDrawView(
                  drawId: drawId,
                  drawChannelId: drawChannelId,
                  channelCode: 'HT_NY_EVE',
                  resultSlotKey: 'NY_EVE',
                  channelLabel: 'Haïti • New York • Evening',
                  gameCodes: ['HT_BOLET'],
                  status: 'OPEN',
                  providerDate: '2026-07-20',
                  providerTime: '22:00:00',
                  providerTimezone: 'America/New_York',
                  localDate: '2026-07-20',
                  localTime: '22:00:00',
                  localTimezone: 'America/Port-au-Prince',
                ),
              ],
            ),
            cashierTicketServiceProvider.overrideWithValue(
              _FakeCashierTicketService(),
            ),
          ],
          child: const MaterialApp(home: SellerTerminalStatsPage()),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text(drawChannelId), findsNothing);
      expect(find.text('ticket-id-1'), findsNothing);

      // The date bar now carries a picker under the two shortcuts, so the
      // per-draw breakdown starts lower and has to be scrolled to.
      await tester.drag(find.byType(ListView), const Offset(0, -200));
      await tester.pumpAndSettle();
      expect(find.text('Nouyòk · Aswè'), findsWidgets);

      await tester.drag(find.byType(ListView), const Offset(0, -400));
      await tester.pumpAndSettle();
      expect(find.text('TCK-260721-001000-PTC2F4-9'), findsOneWidget);
    },
  );

  testWidgets('a report can be opened on a date older than yesterday', (
    tester,
  ) async {
    final requested = <String?>[];

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'pos.reports.title': 'Rapò vandè',
                'pos.reports.today': 'Jodi a',
                'pos.reports.yesterday': 'Yè',
                'pos.reports.pick_date': 'Chwazi yon dat',
                'pos.reports.other_date': 'Yon lòt dat',
                'pos.reports.by_draw': 'Pa tiraj',
                'pos.reports.all': 'Tout',
                'common.cashier_stats.total': 'Total',
                'common.cashier_stats.sales_today': 'Vant jodi a',
                'common.cashier_stats.tickets': 'Tikè',
                'common.cashier_stats.commission_today': 'Komisyon',
              },
            ),
          ),
          terminalStatsByDateProvider.overrideWith((ref, date) async {
            requested.add(date);
            return TerminalDailyStats.empty('HTG');
          }),
          availableDrawsProvider.overrideWith((ref) async => const []),
          cashierTicketServiceProvider.overrideWithValue(
            _FakeCashierTicketService(),
          ),
        ],
        child: const MaterialApp(home: SellerTerminalStatsPage()),
      ),
    );
    await tester.pumpAndSettle();

    // Two shortcuts used to be the whole bar, capping the report at two days
    // even though the backend parses any date.
    expect(find.byKey(const Key('reports-date-filter')), findsOneWidget);

    await tester.tap(find.byKey(const Key('reports-date-filter')));
    await tester.pumpAndSettle();
    expect(find.byType(DatePickerDialog), findsOneWidget);

    // No future dates: a report on a day that has not happened is meaningless.
    final picker = tester.widget<DatePickerDialog>(
      find.byType(DatePickerDialog),
    );
    final now = DateTime.now();
    expect(
      picker.lastDate.isAfter(DateTime(now.year, now.month, now.day)),
      isFalse,
    );
  });
}
