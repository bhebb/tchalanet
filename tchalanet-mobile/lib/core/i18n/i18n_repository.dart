import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../runtime/runtime_controller.dart';
import 'i18n_models.dart';
import 'locale_repository.dart';

const defaultLocale = 'ht';
const supportedLocaleCodes = {'ht', 'fr', 'en'};
const localI18nBundles = [
  'common',
  'domain',
  'component',
  'surface-admin',
  'surface-platform',
  'surface-seller-terminal',
  'feature-auth',
  'feature-public',
  'feature-admin',
  'feature-platform',
  'feature-seller-terminal',
];

String resolveStartupLocale({
  required String? savedLocale,
  required String? deviceLocale,
}) {
  if (savedLocale != null && supportedLocaleCodes.contains(savedLocale)) {
    return savedLocale;
  }
  if (deviceLocale != null && supportedLocaleCodes.contains(deviceLocale)) {
    return deviceLocale;
  }
  return defaultLocale;
}

/// App-scoped active locale, restored before runApp by the locale Repository.
class LocaleNotifier extends Notifier<String> {
  late LocaleRepository _repository;

  @override
  String build() {
    _repository = ref.watch(localeRepositoryProvider);
    final deviceLocale = PlatformDispatcher.instance.locale.languageCode;
    return resolveStartupLocale(
      savedLocale: _repository.readSavedLocale(),
      deviceLocale: deviceLocale,
    );
  }

  Future<void> setLocale(String locale) async {
    if (!supportedLocaleCodes.contains(locale) || locale == state) return;
    state = locale;
    await _repository.saveLocale(locale);
  }
}

final localeProvider = NotifierProvider<LocaleNotifier, String>(
  LocaleNotifier.new,
);

/// Local bundle loaded from bundled assets — always available offline.
final _localBundleProvider = FutureProvider.family<I18nBundle, String>((
  ref,
  locale,
) async {
  final merged = <String, dynamic>{};
  for (final bundle in localI18nBundles) {
    final raw = await rootBundle.loadString('assets/i18n/$locale/$bundle.json');
    final map = jsonDecode(raw) as Map<String, dynamic>;
    deepMergeTranslationTree(merged, map);
  }
  return I18nBundle(
    locale: locale,
    translations: flattenTranslationTree(merged),
  );
});

/// Merged bundle: bundled translations plus the active public or tenant bootstrap
/// overrides. Bootstrap composition is authoritative; mobile never calls i18n
/// catalog endpoints directly.
final i18nProvider = FutureProvider<I18nBundle>((ref) async {
  final locale = ref.watch(localeProvider);
  final local = await ref.watch(_localBundleProvider(locale).future);
  final runtimeOverrides = I18nOverrides(
    locale: locale,
    translations: ref.watch(runtimeI18nOverridesProvider),
  );
  return local.mergeWith(runtimeOverrides);
});

/// Synchronous helper — returns the merged bundle when loaded,
/// empty bundle while loading or on error.
/// Use [i18nProvider] (AsyncValue) for the full async lifecycle.
final i18nBundleProvider = Provider<I18nBundle>((ref) {
  return ref
      .watch(i18nProvider)
      .when(
        data: (bundle) => bundle,
        loading: () =>
            const I18nBundle(locale: defaultLocale, translations: {}),
        error: (_, _) =>
            const I18nBundle(locale: defaultLocale, translations: {}),
      );
});

Map<String, String> flattenTranslationTree(Map<String, dynamic> tree) {
  final flattened = <String, String>{};

  void visit(String prefix, Object? value) {
    if (value is Map) {
      for (final entry in value.entries) {
        final key = entry.key.toString();
        visit(prefix.isEmpty ? key : '$prefix.$key', entry.value);
      }
      return;
    }

    if (value is String) {
      flattened[prefix] = value;
    }
  }

  visit('', tree);
  return flattened;
}

void deepMergeTranslationTree(
  Map<String, dynamic> target,
  Map<String, dynamic> source,
) {
  for (final entry in source.entries) {
    final existing = target[entry.key];
    final incoming = entry.value;
    if (existing is Map<String, dynamic> && incoming is Map<String, dynamic>) {
      deepMergeTranslationTree(existing, incoming);
    } else {
      target[entry.key] = incoming;
    }
  }
}
