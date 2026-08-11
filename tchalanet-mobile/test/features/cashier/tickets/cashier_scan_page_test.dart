import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_scan_page.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart';

void main() {
  test('ticket scan input preserves QR verification URL characters', () {
    final filter = FilteringTextInputFormatter.allow(
      RegExp(r'[A-Za-z0-9\-:/?&=._%+#]'),
    );
    final upper = UpperCaseTextFormatter();

    final filtered = filter.formatEditUpdate(
      TextEditingValue.empty,
      const TextEditingValue(
        text: 'https://tickets.test/public/check-ticket?code=75nr-hpd8',
      ),
    );
    final value = upper.formatEditUpdate(TextEditingValue.empty, filtered);

    expect(
      value.text,
      'HTTPS://TICKETS.TEST/PUBLIC/CHECK-TICKET?CODE=75NR-HPD8',
    );
  });

  test(
    'verify returns the backend response so scan can open ticket details',
    () async {
      final container = ProviderContainer(
        overrides: [
          cashierTicketServiceProvider.overrideWithValue(_FakeTicketService()),
        ],
      );
      addTearDown(container.dispose);

      final result = await container
          .read(verifyControllerProvider.notifier)
          .verify('75NR-HPD8');

      expect(result?.ticketId, 'ticket-1');
      expect(container.read(verifyControllerProvider), isA<VerifyResult>());
    },
  );

  testWidgets('ticket detail page shows verification result before actions', (
    tester,
  ) async {
    const translations = I18nBundle(
      locale: 'en',
      translations: {
        'pos.tickets.detail_title': 'Ticket details',
        'pos.tickets.public_code': 'Public code',
        'pos.tickets.status_placed': 'VALID',
        'pos.tickets.status_cancelled': 'CANCELLED',
        'pos.tickets.status_voided': 'VOIDED',
        'pos.tickets.status_unknown': 'UNKNOWN STATUS',
        'pos.tickets.sale_time': 'Sale time',
        'pos.tickets.draw': 'Draw',
        'pos.tickets.terminal': 'Terminal',
        'pos.tickets.seller': 'Seller',
        'pos.tickets.game_details': 'Game details',
        'pos.tickets.stake': 'Stake',
        'pos.tickets.total': 'Total',
        'pos.tickets.copied': 'Copied',
        'pos.tickets.reprint_confirm': 'Reprint',
        'pos.ticket.verify.not_payable_result_pending.title': 'Result pending',
        'pos.ticket.verify.not_payable_result_pending.message':
            'The draw result is not available yet.',
        'pos.ticket.verify.unknown.title': 'Ticket verification',
        'pos.ticket.verify.unknown.message': 'Unknown verification status.',
      },
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(translations),
          cashierTicketServiceProvider.overrideWithValue(_FakeTicketService()),
        ],
        child: const MaterialApp(
          home: CashierTicketDetailPage(
            ticketId: 'ticket-1',
            verification: CashierTicketVerificationResponse(
              status: 'PENDING',
              severity: 'INFO',
              ticketId: 'ticket-1',
              titleKey: 'pos.ticket.verify.not_payable_result_pending.title',
              messageKey:
                  'pos.ticket.verify.not_payable_result_pending.message',
              availableActions: [],
            ),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('8T1R-JYJF'), findsOneWidget);
    await tester.drag(find.byType(ListView), const Offset(0, -500));
    await tester.pumpAndSettle();

    expect(find.text('Result pending'), findsOneWidget);
    expect(find.text('The draw result is not available yet.'), findsOneWidget);
    expect(find.text('Reprint'), findsOneWidget);
  });
}

class _FakeTicketService extends CashierTicketService {
  _FakeTicketService() : super(Dio());

  @override
  Future<CashierTicketVerificationResponse> verify(
    CashierVerifyTicketRequest request,
  ) async {
    return const CashierTicketVerificationResponse(
      status: 'PENDING',
      severity: 'INFO',
      ticketId: 'ticket-1',
      titleKey: 'pos.ticket.verify.not_payable_result_pending.title',
      messageKey: 'pos.ticket.verify.not_payable_result_pending.message',
      availableActions: [],
    );
  }

  @override
  Future<CashierTicketDetailsView> getDetails(String ticketId) async {
    return CashierTicketDetailsView(
      id: ticketId,
      ticketCode: 'ticket-code',
      publicCode: '8T1R-JYJF',
      status: 'PLACED',
      totalAmountCents: 6800,
      currency: 'USD',
      drawChannelName: 'Draw',
      placedAt: DateTime.parse('2026-08-11T16:39:00Z'),
      terminalCode: 'POS-006',
      sellerDisplayName: 'Test Print',
      lines: const [
        CashierTicketLineDetail(
          lineNumber: 1,
          gameCode: 'HT_BOLET',
          gameLabel: 'HT_BOLET',
          betType: 'STRAIGHT',
          selection: '48',
          stakeAmountCents: 4900,
        ),
      ],
      stakeCents: 6800,
    );
  }
}
