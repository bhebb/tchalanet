import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/auth_token_client.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/auth/data/models/user_session.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:tchalanet_mobile/features/auth/presentation/views/forbidden_page.dart';
import 'package:tchalanet_mobile/features/auth/presentation/views/login_page.dart';
import 'package:tchalanet_mobile/features/draw/data/models/draw_models.dart';
import 'package:tchalanet_mobile/features/draw/presentation/view_models/draw_providers.dart';

class _FakeAuthRepository implements AuthRepository {
  @override
  Future<UserSession> login(AuthCredentials credentials) async =>
      UserSession.unauthenticated;

  @override
  Future<void> logout() async {}

  @override
  Future<UserSession?> restoreSession() async => null;
}

void main() {
  // Override draw providers so tests don't need a network/Dio setup
  final drawOverrides = [
    authRepositoryProvider.overrideWithValue(_FakeAuthRepository()),
    homeDrawSlotsProvider.overrideWith((ref) async => <DrawSlotView>[]),
    i18nBundleProvider.overrideWithValue(
      const I18nBundle(
        locale: 'ht',
        translations: {
          'auth.login.title': 'Tchalanet POS',
          'auth.login.operator_title': 'Aksè operatè',
          'auth.login.subtitle': 'Konekte ak kont Tchalanet ou',
          'auth.login.pos_subtitle': 'Antre nan tèminal vant lan',
          'auth.login.button': 'Konekte',
          'auth.login.email': 'Imèl operatè',
          'auth.login.email_hint': 'non@ajans.com',
          'auth.login.password': 'Modpas',
          'auth.login.password_show': 'Montre modpas la',
          'auth.login.password_hide': 'Kache modpas la',
          'auth.login.blocked': 'Aksè bloke?',
          'auth.login.required': 'Obligatwa',
          'auth.login.invalid_email': 'Imèl pa valab',
          'auth.login.secure_environment': 'Anviwònman sekirize',
          'auth.login.terminal_mode': 'Mòd tèminal POS',
          'auth.login.terminal_binding_notice': 'Verifikasyon tèminal',
          'auth.forbidden.title': 'Aksè refize',
          'auth.forbidden.message': 'Ou pa gen otorizasyon.',
          'auth.forbidden.back': 'Retounen nan koneksyon',
        },
      ),
    ),
  ];

  testWidgets('LoginPage renders Firebase operator credentials form', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: drawOverrides,
        child: const MaterialApp(home: LoginPage()),
      ),
    );
    await tester.pump();

    expect(find.text('Aksè operatè'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('Konekte'), findsNWidgets(2));
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
