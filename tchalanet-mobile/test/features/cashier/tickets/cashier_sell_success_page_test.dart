import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/auth_token_client.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/auth/data/models/user_session.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_sell_success_page.dart';

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
  testWidgets('renders Haitian Creole completion copy', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authRepositoryProvider.overrideWithValue(_FakeAuthRepository()),
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'pos.sale_completion.back_home': 'Retounen sou akèy',
                'pos.sale_completion.accepted': 'Vant la aksepte',
                'pos.sale_completion.printing':
                    'N ap voye tikè a nan enprimant lan.',
                'pos.sale_completion.give_code': 'Bay kliyan an kòd sa a.',
                'pos.sale_completion.code_copied': 'Kòd la kopye',
                'pos.sale_completion.print': 'Enprime',
                'pos.sale_completion.new_ticket': 'NOUVO TIKÈ',
              },
            ),
          ),
        ],
        child: const MaterialApp(
          home: CashierSellSuccessPage(
            ticketId: 'ticket-1',
            ticketCode: 'TCK-1',
            autoPrint: false,
          ),
        ),
      ),
    );

    expect(find.text('Vant la aksepte'), findsOneWidget);
    expect(find.text('N ap voye tikè a nan enprimant lan.'), findsOneWidget);
    expect(find.text('ENPRIME'), findsOneWidget);
    expect(find.text('NOUVO TIKÈ'), findsOneWidget);
  });
}
