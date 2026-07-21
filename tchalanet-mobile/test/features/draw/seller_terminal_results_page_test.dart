import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/draw/data/models/draw_models.dart';
import 'package:tchalanet_mobile/features/draw/presentation/view_models/draw_providers.dart';
import 'package:tchalanet_mobile/features/draw/presentation/views/seller_terminal_results_page.dart';

void main() {
  testWidgets('renders initial filters without duplicate null keys', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'common.refresh': 'Rafrechi',
                'pos.dashboard.home': 'Akèy',
                'pos.dashboard.history': 'Istorik',
                'pos.dashboard.results': 'Rezilta',
                'pos.dashboard.reports': 'Rapò',
                'pos.results.title': 'Rezilta tiraj',
                'pos.results.last_7_days': '7 jou',
                'pos.results.last_30_days': '30 jou',
                'pos.results.provider_filter': 'Founisè',
                'pos.results.all_providers': 'Tout founisè',
                'pos.results.draw_filter': 'Tiraj',
                'pos.results.select_provider_first': 'Chwazi founisè anvan',
                'pos.results.draw_date_filter': 'Dat tiraj',
                'pos.results.select_draw_date': 'Chwazi dat',
                'pos.results.empty': 'Pa gen rezilta',
              },
            ),
          ),
          publicDrawResultSlotsProvider.overrideWith(
            (ref) async => const <PublicDrawResultSlot>[],
          ),
          publicDrawResultHistoryProvider.overrideWith(
            (ref, query) async => const PublicDrawResultHistory(
              items: [],
              page: 0,
              totalItems: 0,
              totalPages: 0,
            ),
          ),
        ],
        child: const MaterialApp(home: SellerTerminalResultsPage()),
      ),
    );

    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(find.text('Rezilta tiraj'), findsOneWidget);
    expect(find.byType(DropdownButtonFormField<String?>), findsNWidgets(2));
  });
}
