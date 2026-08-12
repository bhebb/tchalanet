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

/// Sunmi V2 class POS terminal: 1440 x 720 physical at DPR 2 → 360 x 720
/// logical. Narrower and much shorter than the phones used for development.
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
    'pos.sale.change_draw_title': 'Chanje tiraj la?',
    'pos.sale.change_draw_message': 'Sa ap retire liy ki sou tikè a.',
    'pos.sale.change_draw_confirm': 'Wi, chanje tiraj',
    'pos.sale.change_draw_cancel': 'Non, kenbe tikè a',
    'pos.sale.cancel': 'Anile',
    'pos.sale.no_draws': 'Pa gen tiraj.',
    'pos.sale.remove_line': 'Retire liy la',
    'pos.sale.abandon_title': 'Kite tikè sa a?',
    'pos.sale.abandon_message': 'Ou gen {count} liy ki poko vann.',
    'pos.sale.abandon_confirm': 'Wi, kite l',
    'pos.sale.abandon_cancel': 'Non, kontinye vann',
    'pos.sale.line_removed': 'Liy la retire',
    'pos.sale.undo': 'Defè',
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
  gameCodes: ['HT_MARYAJ', 'HT_LOTO4', 'HT_BOLET', 'HT_LOTO5', 'HT_LOTO3'],
  status: 'OPEN',
  providerDate: '2026-08-06',
  providerTime: '23:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-08-06',
  localTime: '23:00:00',
  localTimezone: 'America/Port-au-Prince',
);

const _draw2 = CashierAvailableDrawView(
  drawId: 'draw-2',
  drawChannelId: 'channel-2',
  channelCode: 'HT_NY_MID',
  resultSlotKey: 'NY_MID',
  channelLabel: 'Haïti • New York • Midday',
  gameCodes: ['HT_BOLET'],
  status: 'OPEN',
  providerDate: '2026-08-06',
  providerTime: '12:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-08-06',
  localTime: '12:00:00',
  localTimezone: 'America/Port-au-Prince',
);

/// A ticket in progress — the state the seller actually works in.
final _form = SellFormData(
  draws: const [_draw],
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

Widget _harness({double keyboardInset = 0, SellFormData? form}) =>
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
        sellControllerProvider.overrideWith(() => _FakeSell(form ?? _form)),
      ],
      child: PosContextProvider(
        context: SurfaceContext.posTerminal,
        child: MaterialApp(
          home: MediaQuery(
            data: MediaQueryData(
              viewInsets: EdgeInsets.only(bottom: keyboardInset),
            ),
            child: const CashierSellPage(),
          ),
        ),
      ),
    );

Future<void> _pumpAtTerminalSize(WidgetTester tester) async {
  tester.view.physicalSize = _posTerminal;
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(_harness());
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('the sell page fits a 360x720 POS terminal', (tester) async {
    await _pumpAtTerminalSize(tester);

    // A RenderFlex overflow fails the test, so rendering at the terminal size
    // is itself the assertion. It caught two: full-width game chips, and the
    // total row in the bottom bar.
    expect(tester.takeException(), isNull);

    // Every game is reachable without a gesture. A horizontally scrolling row
    // was tried first: it fit in 56 dp but showed only three of five at 360 dp,
    // with no chip peeking at the edge to hint at the rest — and the selected
    // game could itself be off-screen.
    const games = ['Maryaj', 'Loto 4', 'Bòlèt', 'Loto 5', 'Loto 3'];
    final rows = <double>{};
    for (final g in games) {
      final f = find.text(g);
      expect(f, findsOneWidget, reason: g);
      final left = tester.getTopLeft(f);
      expect(
        left.dx + tester.getSize(f).width,
        lessThanOrEqualTo(360.0),
        reason: '$g must be on screen, not behind a horizontal scroll',
      );
      rows.add(left.dy);
    }
    // Two rows, not the three they took when each chip was ~107 dp wide.
    expect(rows.length, lessThanOrEqualTo(2));

    // Number and stake share a row.
    expect(
      tester.getTopLeft(find.text('Miz')).dy,
      tester.getTopLeft(find.text('Nimewo / seleksyon')).dy,
    );

    // The point of all of it: the seller sees the lines making up the total.
    // Before this they started below 720 and never appeared without scrolling,
    // so the screen showed a total with nothing to explain it. Clearing 720 is
    // not enough — a line must finish above the action bar, or it is clipped.
    final barTop = tester.getTopLeft(find.text('Total pou peye')).dy;
    for (final line in ['#1 Bòlèt', '#2 Bòlèt', '#3 Loto 3']) {
      final f = find.text(line);
      expect(f, findsOneWidget, reason: line);
      expect(
        tester.getTopLeft(f).dy + tester.getSize(f).height,
        lessThan(barTop),
        reason: '$line must be fully visible above the action bar',
      );
    }
  });

  testWidgets('leaving a ticket in progress asks first', (tester) async {
    await _pumpAtTerminalSize(tester);

    // The × sat one tap from the work area and discarded the ticket silently.
    // A tooltip is no answer: it needs a long press and a convention a seller
    // has no reason to know.
    await tester.tap(find.byIcon(Icons.close_rounded));
    await tester.pumpAndSettle();

    expect(find.text('Kite tikè sa a?'), findsOneWidget);
    // Both buttons say what they do, rather than OK and Cancel.
    expect(find.text('Wi, kite l'), findsOneWidget);
    expect(find.text('Non, kontinye vann'), findsOneWidget);

    await tester.tap(find.text('Non, kontinye vann'));
    await tester.pumpAndSettle();
    expect(find.text('#1 Bòlèt'), findsOneWidget);
  });

  testWidgets('changing draw with ticket lines asks first', (tester) async {
    final form = _form.copyWith(draws: const [_draw, _draw2]);
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(_harness(form: form));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Chanje'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(ListTile).last);
    await tester.pumpAndSettle();

    expect(find.text('Chanje tiraj la?'), findsOneWidget);
    expect(find.text('Sa ap retire liy ki sou tikè a.'), findsOneWidget);

    await tester.tap(find.text('Non, kenbe tikè a'));
    await tester.pumpAndSettle();
    expect(find.text('#1 Bòlèt'), findsOneWidget);
  });

  testWidgets('removing a line is one tap, and undoable', (tester) async {
    await _pumpAtTerminalSize(tester);
    expect(find.text('#1 Bòlèt'), findsOneWidget);

    // One tap. A ticket can run to twenty numbers; a dialog in front of every
    // correction taxes the common case.
    await tester.tap(find.byIcon(Icons.delete_outline_rounded).first);
    await tester.pumpAndSettle();

    // The offer is inline, in the list. A SnackBar sat on the bottom action
    // area with the keyboard up and covered Ajoute, which blocked the seller
    // from entering the next number until it timed out.
    expect(find.text('Liy la retire'), findsOneWidget);
    expect(find.byType(SnackBar), findsNothing);
    // Ajoute stays reachable while the offer is on screen.
    expect(find.text('Ajoute'), findsOneWidget);

    // And the stray tap is recoverable, in place.
    await tester.tap(find.text('Defè'));
    await tester.pumpAndSettle();
    expect(find.text('#1 Bòlèt'), findsOneWidget);
    expect(find.text('#3 Loto 3'), findsOneWidget);
  });

  testWidgets('the last line is echoed once while typing', (tester) async {
    tester.view.physicalSize = _posTerminal;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    // Keyboard up — the state the seller enters numbers in.
    await tester.pumpWidget(_harness(keyboardInset: 280));
    await tester.pumpAndSettle();

    // It used to render in the body *and* in the bottom action bar, so the
    // seller saw the same bet twice. Caught on a real device, not by a test.
    expect(find.text('Loto 3'), findsWidgets);
    expect(find.textContaining('123'), findsOneWidget);
  });

  testWidgets('completing a bolet number moves focus to stake', (tester) async {
    await _pumpAtTerminalSize(tester);

    await tester.tap(find.text('Bòlèt'));
    await tester.pumpAndSettle();

    final selectionField = find.byType(TextField).first;
    await tester.tap(selectionField);
    await tester.enterText(selectionField, '12');
    await tester.pump();

    final stakeField = find.byType(TextField).last;
    expect(tester.widget<TextField>(stakeField).focusNode?.hasFocus, isTrue);
  });
}

class _FakeSell extends SellController {
  _FakeSell(this._formData);
  final SellFormData _formData;

  @override
  SellState build() => SellReady(_formData);

  // The page loads the catalog on init; the fixture supplies it instead.
  @override
  Future<void> loadCatalog({
    String? preselectedDrawId,
    String? currency,
  }) async {}
}
