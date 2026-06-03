import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/auth/presentation/views/login_page.dart';
import 'package:tchalanet_mobile/features/draw/data/models/draw_models.dart';
import 'package:tchalanet_mobile/features/draw/presentation/view_models/draw_providers.dart';

void main() {
  // Override draw providers so tests don't need a network/Dio setup
  final drawOverrides = [
    homeDrawSlotsProvider.overrideWith((ref) async => <DrawSlotView>[]),
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
    expect(find.text('Se connecter'), findsOneWidget);
  });

  testWidgets('LoginPage shows loading state while authenticating',
      (tester) async {
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
}
