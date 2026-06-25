import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/haitian_flutter_localizations.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const allowedTopLevelByBundle = {
    'common': {'common'},
    'domain': {'domain'},
    'component': {'component', 'notifications'},
    'surface-public': {'surface'},
    'surface-admin': {'surface'},
    'surface-platform': {'surface'},
    'surface-seller-terminal': {'app', 'surface'},
    'feature-auth': {'auth', 'feature'},
    'feature-public': {'feature'},
    'feature-admin': {'feature'},
    'feature-platform': {'feature'},
    'feature-seller-terminal': {'feature', 'pos'},
  };

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

  test('local bundle files are aligned across locales', () async {
    for (final locale in supportedLocaleCodes) {
      for (final bundle in localI18nBundles) {
        final raw = await rootBundle.loadString(
          'assets/i18n/$locale/$bundle.json',
        );
        expect(jsonDecode(raw), isA<Map<String, dynamic>>());
      }
    }
  });

  test('local bundles expose the same flattened keys', () async {
    final bundles = <String, Set<String>>{};

    for (final locale in supportedLocaleCodes) {
      final merged = <String, dynamic>{};
      for (final bundle in localI18nBundles) {
        final raw = await rootBundle.loadString(
          'assets/i18n/$locale/$bundle.json',
        );
        deepMergeTranslationTree(
          merged,
          jsonDecode(raw) as Map<String, dynamic>,
        );
      }
      bundles[locale] = flattenTranslationTree(merged).keys.toSet();
    }

    expect(bundles['ht'], bundles['fr']);
    expect(bundles['ht'], bundles['en']);
  });

  test('local bundles do not declare duplicate keys across files', () async {
    for (final locale in supportedLocaleCodes) {
      final seen = <String, String>{};
      final duplicates = <String>[];

      for (final bundle in localI18nBundles) {
        final raw = await rootBundle.loadString(
          'assets/i18n/$locale/$bundle.json',
        );
        final keys = flattenTranslationTree(
          jsonDecode(raw) as Map<String, dynamic>,
        ).keys;

        for (final key in keys) {
          final previousBundle = seen[key];
          if (previousBundle != null) {
            duplicates.add('$key ($previousBundle, $bundle)');
          } else {
            seen[key] = bundle;
          }
        }
      }

      expect(duplicates, isEmpty, reason: 'Duplicate i18n keys in $locale');
    }
  });

  test(
    'local bundle top-level namespaces stay in their owning files',
    () async {
      for (final locale in supportedLocaleCodes) {
        for (final bundle in localI18nBundles) {
          final raw = await rootBundle.loadString(
            'assets/i18n/$locale/$bundle.json',
          );
          final data = jsonDecode(raw) as Map<String, dynamic>;
          final allowed = allowedTopLevelByBundle[bundle]!;
          final invalid = data.keys.where((key) => !allowed.contains(key));

          expect(
            invalid,
            isEmpty,
            reason: '$locale/$bundle.json has top-level keys outside $allowed',
          );
        }
      }
    },
  );

  test('flattenTranslationTree exposes nested leaves as dot keys', () {
    expect(
      flattenTranslationTree({
        'common': {
          'action': {'save': 'Save'},
        },
        'feature': {
          'login': {'title': 'Welcome'},
        },
      }),
      {'common.action.save': 'Save', 'feature.login.title': 'Welcome'},
    );
  });

  test(
    'deepMergeTranslationTree keeps nested values and lets later bundles win',
    () {
      final target = {
        'common': {
          'action': {'save': 'Save', 'cancel': 'Cancel'},
        },
      };

      deepMergeTranslationTree(target, {
        'common': {
          'action': {'save': 'Save now'},
        },
      });

      expect(flattenTranslationTree(target), {
        'common.action.save': 'Save now',
        'common.action.cancel': 'Cancel',
      });
    },
  );
}
