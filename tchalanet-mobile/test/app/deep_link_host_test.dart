import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/app/deep_link_parser.dart';

void main() {
  test('extracts public code from public check-ticket app link', () {
    final code = publicCodeFromTicketVerificationUri(
      Uri.parse('https://tchalanet.com/public/check-ticket?code=75nr-hpd8'),
    );

    expect(code, '75NR-HPD8');
  });

  test('extracts public code from public ticket path app link', () {
    final code = publicCodeFromTicketVerificationUri(
      Uri.parse('https://app.tchalanet.com/public/ticket/75NR-HPD8'),
    );

    expect(code, '75NR-HPD8');
  });

  test('extracts public code from custom development deep link', () {
    final code = publicCodeFromTicketVerificationUri(
      Uri.parse('tchalanet://check-ticket?code=75nr-hpd8'),
    );

    expect(code, '75NR-HPD8');
  });

  test('ignores unrelated links', () {
    final code = publicCodeFromTicketVerificationUri(
      Uri.parse('https://tchalanet.com/public/results'),
    );

    expect(code, isNull);
  });
}
