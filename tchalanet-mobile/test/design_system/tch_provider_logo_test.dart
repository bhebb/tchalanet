import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/design_system/components/tch_provider_logo.dart';

void main() {
  group('TchProviderLogo.providerCodeFromLabel', () {
    test('resolves known provider display labels', () {
      expect(
        TchProviderLogo.providerCodeFromLabel('Haïti • New York • Midday'),
        'NY',
      );
      expect(
        TchProviderLogo.providerCodeFromLabel('California · Evening'),
        'CA',
      );
      expect(
        TchProviderLogo.providerCodeFromLabel('Pennsylvania • Soir'),
        'PA',
      );
    });

    test('does not invent a provider for an unknown label', () {
      expect(TchProviderLogo.providerCodeFromLabel('Tirage spécial'), isNull);
    });
  });
}
