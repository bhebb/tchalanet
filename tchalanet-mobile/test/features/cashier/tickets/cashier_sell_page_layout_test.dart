import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/design_system/layout/screen_size.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/models/cashier_home_models.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/view_models/sell_controller.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_sell_page.dart';

const _posTerminal = Size(360, 720);

const _translations = I18nBundle(
  locale: 'ht',
  translations: {
    'pos.sale.title': 'Vann yon tikè',
    'pos.sale.draw_label': 'Tiraj',
    'pos.sale.game_label': 'Jwèt',
    'pos.sale.selection_label': 'Nimewo / seleksyon',
    'pos.sale.stake_label': 'Miz',
    'pos.sale.ticket_label': 'Tikè',
    'pos.sale.ticket_total': 'Total pou peye',
    'pos.sale.add_line': 'Ajoute',
    'pos.sale.verify_sale': 'Verifye vant la',
    'pos.sale.change_draw': 'Chanje',
    'pos.sale.cancel': 'Anile',
    'pos.sale.no_draws': 'Pa gen tiraj.',
    'common.preparation.total': 'Total pou peye',
  },
);

CashierGameOptionResponse _game(String code, String label) =>
    CashierGameOptionResponse(
      gameCode: code,
      gameLabel: label,
      betType: 'SINGLE',
      betTypeLabel: label,
      requiresOption: false,
      options: const [],
      selectionDigits: 2,
      selectionSegments: 1,
    );

const _draw = CashierAvailableDrawView(
  drawId: 'draw-1',
  drawChannelId: 'channel-1',
  channelCode: 'HT_GA_EVE',
  resultSlotKey: 'GA_EVE',
  channelLabel: 'Haïti • Georgia • Evening',
  gameCodes: ['HT_BOLET'],
  status: 'OPEN',
  providerDate: '2026-08-06',
  providerTime: '23:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-08-06',
  localTime: '23:00:00',
  localTimezone: 'America/Port-au-Prince',
);

void main() {
  testWidgets('the sell page fits a 360x720 POS terminal', (tester) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    final form = SellFormData(
      draws: [_draw],
      games: [
        _game('HT_MARYAJ', 'Maryaj'),
        _game('HT_LOTO4', 'Loto 4'),
        _game('HT_BOLET', 'Bòlèt'),
        _game('HT_LOTO5', 'Loto 5'),
        _game('HT_LOTO3', 'Loto 3'),
      ],
      selectedDrawId: 'draw-1',
      selectedGameCode: 'HT_LOTO5',
      selectedBetType: 'SINGLE',
      // A ticket in progress — the state the seller actually works in.
      committedLines: const [
        SellLine(
          gameCode: 'HT_BOLET',
          gameLabel: 'Bòlèt',
          betType: 'SINGLE',
          betTypeLabel: 'Bòlèt',
          selection: '12',
          stake: 15,
        ),
        SellLine(
          gameCode: 'HT_BOLET',
          gameLabel: 'Bòlèt',
          betType: 'SINGLE',
          betTypeLabel: 'Bòlèt',
          selection: '16',
          stake: 19,
        ),
        SellLine(
          gameCode: 'HT_LOTO3',
          gameLabel: 'Loto 3',
          betType: 'SINGLE',
          betTypeLabel: 'Loto 3',
          selection: '123',
          stake: 10,
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(_translations),
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
          sellControllerProvider.overrideWith(() => _FakeSell(form)),
        ],
        child: const PosContextProvider(
          context: SurfaceContext.posTerminal,
          child: MaterialApp(home: CashierSellPage()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // A RenderFlex overflow fails the test, so rendering at the terminal size
    // is itself the assertion. It caught two: full-width game chips, and the
    // total row in the bottom bar.
    expect(tester.takeException(), isNull);

    // All five games sit on one scrolling row, not stacked.
    final gamesRow = tester.getTopLeft(find.text('Maryaj')).dy;
    for (final g in ['Loto 3', 'Loto 4', 'Loto 5', 'Bòlèt']) {
      expect(tester.getTopLeft(find.text(g)).dy, gamesRow, reason: g);
    }

    // Number and stake share a row.
    expect(
      tester.getTopLeft(find.text('Miz')).dy,
      tester.getTopLeft(find.text('Nimewo / seleksyon')).dy,
    );

    // The point of all of it: the seller sees the lines making up the total.
    // Before this they started below 720 and never appeared without scrolling,
    // so the screen showed a total with nothing to explain it.
    for (final selection in ['12', '16', '123']) {
      final line = find.text(selection);
      expect(line, findsOneWidget, reason: selection);
      expect(
        tester.getTopLeft(line).dy,
        lessThan(720),
        reason: 'line $selection must be visible without scrolling',
      );
    }
  });
}

class _FakeSell extends SellController {
  _FakeSell(this._form);
  final SellFormData _form;

  @override
  SellState build() => SellReady(_form);

  // The page loads the catalog on init; the fixture supplies it instead.
  @override
  Future<void> loadCatalog({
    String? preselectedDrawId,
    String? currency,
  }) async {}
}
