import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/haitian_flutter_localizations.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('Haitian Creole is the default locale', () {
    expect(defaultLocale, 'ht');
    expect(supportedLocaleCodes, containsAll({'ht', 'fr', 'en'}));
  });

  test(
    'framework localization delegates explicitly support Haitian Creole',
    () {
      const locale = Locale('ht');

      expect(
        const HaitianMaterialLocalizationsDelegate().isSupported(locale),
        isTrue,
      );
      expect(
        const HaitianCupertinoLocalizationsDelegate().isSupported(locale),
        isTrue,
      );
    },
  );

  test('local bundles expose the same keys', () async {
    final bundles = <String, Set<String>>{};

    for (final locale in supportedLocaleCodes) {
      final raw = await rootBundle.loadString('assets/i18n/$locale.json');
      bundles[locale] = (jsonDecode(raw) as Map<String, dynamic>).keys.toSet();
    }

    expect(bundles['ht'], bundles['fr']);
    expect(bundles['ht'], bundles['en']);
  });
}
