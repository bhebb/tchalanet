import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/auth/presentation/login_page.dart';

void main() {
  testWidgets('LoginPage renders username and password fields', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: LoginPage()),
      ),
    );

    expect(find.text('Tchalanet'), findsOneWidget);
    expect(find.text("Nom d'utilisateur"), findsOneWidget);
    expect(find.text('Mot de passe'), findsOneWidget);
    expect(find.text('Connexion'), findsOneWidget);
  });

  testWidgets('LoginPage shows validation errors on empty submit', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: LoginPage()),
      ),
    );

    await tester.tap(find.text('Connexion'));
    await tester.pump();

    expect(find.text("Le nom d'utilisateur est requis"), findsOneWidget);
    expect(find.text('Le mot de passe est requis'), findsOneWidget);
  });
}
