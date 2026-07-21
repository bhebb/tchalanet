import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/cashier/home/data/models/cashier_home_models.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_sell_catalog_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_sell_page.dart';

class _FakeSellCatalogService extends CashierSellCatalogService {
  _FakeSellCatalogService() : super(Dio());

  @override
  Future<List<CashierAvailableDrawView>> fetchAvailableDraws({
    int lookaheadHours = 24,
    int limit = 20,
  }) async => const [
    CashierAvailableDrawView(
      drawId: 'draw-1',
      drawChannelId: 'channel-1',
      channelLabel: 'Haïti • New York • Evening',
      gameCodes: ['BOLET'],
      status: 'OPEN',
      providerDate: '2026-07-21',
      providerTime: '22:00:00',
      providerTimezone: 'America/New_York',
      localDate: '2026-07-21',
      localTime: '22:00:00',
      localTimezone: 'America/Port-au-Prince',
    ),
  ];

  @override
  Future<List<CashierGameOptionResponse>> fetchAvailableGames() async => const [
    CashierGameOptionResponse(
      gameCode: 'BOLET',
      gameLabel: 'Bolet',
      betType: 'STRAIGHT',
      betTypeLabel: 'Direct',
      requiresOption: false,
      options: [],
      selectionDigits: 2,
      selectionSegments: 1,
    ),
  ];
}

void main() {
  testWidgets('sell page switches to compact entry while keyboard is open', (
    tester,
  ) async {
    final keyboardInset = ValueNotifier<double>(0);
    addTearDown(keyboardInset.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'pos.sale.title': 'Vann',
                'pos.sale.cancel': 'Annuler',
                'pos.sale.draw_label': 'Tirage',
                'pos.sale.game_label': 'Jeu',
                'pos.sale.selection_label': 'Numéro',
                'pos.sale.stake_label': 'Mise',
                'pos.sale.ticket_label': 'Ticket',
                'pos.sale.ticket_total': 'Total ticket',
                'pos.sale.add_line': 'Ajouter',
                'pos.sale.verify_sale': 'Vérifier la vente',
                'pos.sale.checking_sale': 'Vérification',
                'pos.sale.change_draw': 'Changer',
                'pos.sale.no_draws': 'Aucun tirage',
                'pos.sale.remove_line': 'Retirer',
                'pos.dashboard.closed': 'Fermé',
                'pos.dashboard.closes_soon': 'Bientôt fermé',
                'pos.dashboard.closes_in_hours_minutes':
                    'Ferme dans {hours}h {minutes}m',
                'pos.dashboard.closes_in_minutes': 'Ferme dans {minutes}m',
                'common.preparation.total': 'Total',
              },
            ),
          ),
          cashierHomeProvider.overrideWith(
            (ref) async => const CashierHomeResponse(
              quickActions: [],
              widgets: [],
              navigation: [],
              notices: [],
              currency: 'HTG',
              primaryDraw: CashierHomeDrawSummary(drawId: 'draw-1'),
              operationalContext: CashierHomeOpCtx(
                ready: true,
                trusted: true,
                missing: [],
                sellerTerminalId: 'terminal-1',
              ),
            ),
          ),
          cashierSellCatalogServiceProvider.overrideWithValue(
            _FakeSellCatalogService(),
          ),
        ],
        child: ValueListenableBuilder<double>(
          valueListenable: keyboardInset,
          builder: (context, inset, _) => MaterialApp(
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(viewInsets: EdgeInsets.only(bottom: inset)),
              child: child!,
            ),
            home: const CashierSellPage(),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    await tester.tap(find.text('Bolet'));
    await tester.pumpAndSettle();

    final selectionField = find.byType(TextField).first;
    final stakeField = find.byType(TextField).last;
    await tester.enterText(selectionField, '42');
    await tester.enterText(stakeField, '10');
    await tester.pumpAndSettle();

    await tester.tap(find.text('Ajouter'));
    await tester.pumpAndSettle();

    expect(find.text('Total'), findsOneWidget);
    expect(find.text('Vérifier la vente'), findsOneWidget);

    keyboardInset.value = 320;
    await tester.pumpAndSettle();

    expect(find.text('Ajouter'), findsOneWidget);
    expect(find.text('Annuler'), findsWidgets);
    expect(find.text('Vérifier la vente'), findsNothing);
    expect(find.text('Total'), findsNothing);
    expect(find.textContaining('Bolet  42'), findsOneWidget);
  });
}
