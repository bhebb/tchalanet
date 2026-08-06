import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/core/network/connectivity_repository.dart';
import 'package:tchalanet_mobile/design_system/layout/screen_size.dart';
import 'package:tchalanet_mobile/features/auth/data/models/user_session.dart';
import 'package:tchalanet_mobile/features/auth/presentation/view_models/auth_controller.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/models/cashier_home_models.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/services/terminal_stats_service.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/views/cashier_home_page.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';

/// Sunmi V2 class POS terminal: 1440 x 720 physical at DPR 2 → 360 x 720
/// logical. Narrower and much shorter than the phones used for development,
/// so it is the strict lower bound the POS screens must survive.
const Size _posTerminal = Size(360, 720);

const _translations = I18nBundle(
  locale: 'ht',
  translations: {
    'pos.dashboard.available_draws': 'Tiraj disponib',
    'pos.dashboard.daily_summary': 'Rezime jodi a',
    'pos.dashboard.last_ticket': 'Dènye tikè',
    'pos.dashboard.sell': 'Vann',
    'pos.dashboard.filter_draws': 'Filtre tiraj',
    'pos.dashboard.all_providers': 'Tout Eta yo',
    'pos.dashboard.no_draws': 'Pa gen tiraj.',
    'pos.dashboard.closes_in_minutes': 'Fèmen nan {minutes} min',
    'pos.dashboard.closes_soon': 'Ap fèmen',
    'pos.dashboard.closed': 'Fèmen',
    'pos.draw.providers.ny': 'Nouyòk',
    'pos.draw.slots.evening': 'Aswè',
    'common.cashier_stats.sales_today': 'Vant jodi a',
    'common.cashier_stats.tickets': 'Tikè',
    'common.status.online': 'Anliy',
    'common.status.offline': 'Òfliy',
  },
);

/// The longest realistic label, to squeeze the flexible middle column between
/// the provider logo, the chevron and the sell button.
CashierAvailableDrawView _draw(int index) => CashierAvailableDrawView(
  drawId: 'draw-$index',
  drawChannelId: 'channel-$index',
  channelCode: 'HT_NY_EVE',
  resultSlotKey: 'NY_EVE',
  channelLabel: 'Haïti • New York • Evening',
  gameCodes: const ['HT_BOLET'],
  status: 'OPEN',
  providerDate: '2026-08-06',
  providerTime: '22:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-08-06',
  localTime: '22:00:00',
  localTimezone: 'America/Port-au-Prince',
);

Widget _harness({
  required int drawCount,
  SurfaceContext surface = SurfaceContext.posTerminal,
}) => ProviderScope(
  overrides: [
    i18nBundleProvider.overrideWithValue(_translations),
    availableDrawsProvider.overrideWith(
      (ref) async => [for (var i = 0; i < drawCount; i++) _draw(i)],
    ),
    terminalDailyStatsProvider.overrideWith(
      (ref) async => const TerminalDailyStats(
        available: true,
        trustState: 'READY',
        trustReasonCode: 'analytics.trust.ready',
        ticketCount: 12,
        salesTotalCents: 134500,
        sellerCommissionTotalCents: 17485,
        currency: 'HTG',
      ),
    ),
    cashierHomeProvider.overrideWith(
      (ref) async => const CashierHomeResponse(
        quickActions: [],
        widgets: [],
        navigation: [],
        notices: [],
      ),
    ),
    cashierReadinessProvider.overrideWith(
      (ref) async => const CashierReadinessResponse(
        ready: true,
        attentionLevel: CashierAttentionLevel.none,
        badges: [],
        notifications: [],
        blockers: [],
      ),
    ),
    latestTicketProvider.overrideWith((ref) async => null),
    isOnlineProvider.overrideWith((ref) => Stream.value(true)),
    userSessionProvider.overrideWithValue(
      const UserSession(authenticated: true, displayName: 'Jan Batis'),
    ),
  ],
  child: PosContextProvider(
    context: surface,
    child: const MaterialApp(home: CashierHomePage()),
  ),
);

void main() {
  // A RenderFlex overflow is reported as a test failure, so simply rendering
  // at the terminal size is the assertion: no row may spill at 360 dp wide.
  testWidgets('home survives a POS terminal screen with a full draw list', (
    tester,
  ) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    // The backend caps the list at 20 (limit=20, lookaheadHours=24).
    await tester.pumpWidget(_harness(drawCount: 20));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });

  testWidgets('the daily summary is reachable without scrolling past the draws', (
    tester,
  ) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(_harness(drawCount: 20));
    await tester.pumpAndSettle();

    final summary = find.text('Rezime jodi a');
    final draws = find.text('Tiraj disponib');
    expect(summary, findsOneWidget);
    expect(draws, findsOneWidget);

    // Summary above the list: on a 720 dp screen the seller must not scroll
    // past up to 20 cards to read the day's takings.
    expect(
      tester.getTopLeft(summary).dy,
      lessThan(tester.getTopLeft(draws).dy),
    );
    // And it is on screen at rest.
    expect(tester.getTopLeft(summary).dy, lessThan(_posTerminal.height));
  });

  testWidgets('the sell button meets the POS touch target', (tester) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(_harness(drawCount: 3));
    await tester.pumpAndSettle();

    final button = find.byKey(const ValueKey('draw-sell-action:draw-0'));
    expect(button, findsOneWidget);
    // 56 dp on a POS terminal, per context.minTouchTarget.
    expect(tester.getSize(button).height, greaterThanOrEqualTo(56.0));
  });

  testWidgets('the sell button keeps the Material minimum on a phone', (
    tester,
  ) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      _harness(drawCount: 3, surface: SurfaceContext.mobile),
    );
    await tester.pumpAndSettle();

    final button = find.byKey(const ValueKey('draw-sell-action:draw-0'));
    expect(tester.getSize(button).height, greaterThanOrEqualTo(48.0));
  });
}
