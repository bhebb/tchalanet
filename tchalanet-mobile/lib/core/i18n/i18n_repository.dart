import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'i18n_models.dart';
import 'i18n_service.dart';

/// Active locale — initialised from the device locale, falls back to 'fr'.
class LocaleNotifier extends Notifier<String> {
  @override
  String build() {
    final deviceLocale = PlatformDispatcher.instance.locale.languageCode;
    const supported = {'fr', 'en'};
    return supported.contains(deviceLocale) ? deviceLocale : 'fr';
  }

  void setLocale(String locale) => state = locale;
}

final localeProvider = NotifierProvider<LocaleNotifier, String>(
  LocaleNotifier.new,
);

/// Local bundle loaded from bundled assets — always available offline.
final _localBundleProvider = FutureProvider.family<I18nBundle, String>(
  (ref, locale) async {
    final raw = await rootBundle.loadString('assets/i18n/$locale.json');
    final map = jsonDecode(raw) as Map<String, dynamic>;
    return I18nBundle(
      locale: locale,
      translations: map.map((k, v) => MapEntry(k, v as String)),
    );
  },
);

/// Backend overrides — fetched after auth, fails safely (returns empty).
final _backendOverridesProvider = FutureProvider.family<I18nOverrides, String>(
  (ref, locale) async {
    try {
      return await ref.watch(i18nServiceProvider).fetchOverrides(locale);
    } catch (_) {
      return I18nOverrides.empty;
    }
  },
);

/// Merged bundle: local translations + backend overrides (backend wins).
final i18nProvider = FutureProvider<I18nBundle>((ref) async {
  final locale = ref.watch(localeProvider);
  final local = await ref.watch(_localBundleProvider(locale).future);
  final overrides = await ref.watch(_backendOverridesProvider(locale).future);
  return local.mergeWith(overrides);
});

/// Synchronous helper — returns the merged bundle when loaded,
/// empty bundle while loading or on error.
/// Use [i18nProvider] (AsyncValue) for the full async lifecycle.
final i18nBundleProvider = Provider<I18nBundle>((ref) {
  return ref.watch(i18nProvider).when(
    data: (bundle) => bundle,
    loading: () => const I18nBundle(locale: 'fr', translations: {}),
    error: (_, _) => const I18nBundle(locale: 'fr', translations: {}),
  );
});
