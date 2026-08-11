import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/views/cashier_scan_page.dart';

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
}
