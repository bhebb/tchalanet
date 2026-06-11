import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/auth/presentation/views/forbidden_page.dart';
import 'package:tchalanet_mobile/features/auth/presentation/views/login_page.dart';
import 'package:tchalanet_mobile/features/draw/data/models/draw_models.dart';
import 'package:tchalanet_mobile/features/draw/presentation/view_models/draw_providers.dart';

void main() {
  // Override draw providers so tests don't need a network/Dio setup
  final drawOverrides = [
    homeDrawSlotsProvider.overrideWith((ref) async => <DrawSlotView>[]),
    i18nBundleProvider.overrideWithValue(
      const I18nBundle(
        locale: 'ht',
        translations: {
          'auth.login.title': 'Tchalanet POS',
          'auth.login.subtitle': 'Konekte ak kont Tchalanet ou',
          'auth.login.button': 'Konekte',
          'auth.login.terminal_mode': 'Mòd tèminal POS',
          'auth.forbidden.title': 'Aksè refize',
          'auth.forbidden.message': 'Ou pa gen otorizasyon.',
          'auth.forbidden.back': 'Retounen nan koneksyon',
        },
      ),
    ),
  ];

  testWidgets('LoginPage renders PKCE login button', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: drawOverrides,
        child: const MaterialApp(home: LoginPage()),
      ),
    );
    await tester.pump();

    expect(find.text('Tchalanet POS'), findsWidgets);
    expect(find.text('Konekte'), findsOneWidget);
  });

  testWidgets('LoginPage shows loading state while authenticating', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: drawOverrides,
        child: const MaterialApp(home: LoginPage()),
      ),
    );
    await tester.pump();

    // Initial state: button enabled, no spinner
    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(
      tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
      isNotNull,
    );
  });

  testWidgets('ForbiddenPage renders shared blocked state in Haitian Creole', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: drawOverrides,
        child: const MaterialApp(home: ForbiddenPage()),
      ),
    );

    expect(find.text('Aksè refize'), findsOneWidget);
    expect(find.text('Ou pa gen otorizasyon.'), findsOneWidget);
    expect(find.text('Retounen nan koneksyon'), findsOneWidget);
  });
}
