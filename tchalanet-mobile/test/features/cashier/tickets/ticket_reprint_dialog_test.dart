import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/core/network/api_exception.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/print_ticket_action.dart';

void main() {
  testWidgets('reprint pre-fills an editable audit reason and offers presets', (
    tester,
  ) async {
    const translations = I18nBundle(
      locale: 'en',
      translations: {
        'common.cancel': 'Cancel',
        'pos.tickets.reprint_title': 'Reprint ticket',
        'pos.tickets.reprint_message': 'Choose or edit the reprint reason.',
        'pos.tickets.reprint_preset': 'Common reason',
        'pos.tickets.reprint_reason': 'Reason',
        'pos.tickets.reprint_reason_invalid': 'Reason is too short.',
        'pos.tickets.reprint_default': 'Seller requested reprint',
        'pos.tickets.reprint_printer_failure': 'Printer issue',
        'pos.tickets.reprint_illegible': 'Illegible ticket',
        'pos.tickets.reprint_customer_request': 'Customer request',
        'pos.tickets.reprint_confirm': 'Reprint',
      },
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [i18nBundleProvider.overrideWithValue(translations)],
        child: MaterialApp(
          home: Scaffold(
            body: Consumer(
              builder: (context, ref, _) => FilledButton(
                onPressed: () => requestTicketReprint(context, ref, 'ticket-1'),
                child: const Text('Open'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();

    expect(find.text('Reprint ticket'), findsOneWidget);
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      'SELLER_REQUESTED_REPRINT',
    );

    await tester.tap(find.byType(DropdownButtonFormField<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Printer issue').last);
    await tester.pump();

    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      'PRINTER_FAILURE',
    );
  });

  testWidgets('print action displays localized API error code copy', (
    tester,
  ) async {
    const translations = I18nBundle(
      locale: 'fr',
      translations: {
        'common.errors.codes.validation.failed.message':
            'Corrigez les informations du ticket.',
        'common.errors.categories.validation.message':
            'Certaines informations sont invalides.',
        'common.error.unknown': 'Erreur inconnue',
      },
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(translations),
          cashierTicketServiceProvider.overrideWithValue(
            _ThrowingPrintTicketService(),
          ),
        ],
        child: MaterialApp(
          home: Scaffold(
            body: Consumer(
              builder: (context, ref, _) => FilledButton(
                onPressed: () => printTicket(context, ref, 'ticket-1'),
                child: const Text('Print'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Print'));
    await tester.pumpAndSettle();

    expect(find.text('Corrigez les informations du ticket.'), findsOneWidget);
    expect(find.textContaining('Raw server validation detail'), findsNothing);
  });
}

class _ThrowingPrintTicketService extends CashierTicketService {
  _ThrowingPrintTicketService() : super(Dio());

  @override
  Future<Never> print(
    String ticketId, {
    bool recordPrint = true,
    String? reprintReason,
  }) async {
    throw ApiException(
      message: 'Raw server validation detail',
      statusCode: 400,
      code: 'validation.failed',
      category: 'validation',
      retryPolicy: 'AFTER_USER_ACTION',
      retryable: true,
    );
  }
}
