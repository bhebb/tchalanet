class I18nOverrides {
  const I18nOverrides({required this.locale, required this.translations});

  final String locale;
  final Map<String, String> translations;

  static const empty = I18nOverrides(locale: '', translations: {});

  factory I18nOverrides.fromJson(Map<String, dynamic> json) => I18nOverrides(
    locale: json['locale'] as String? ?? '',
    translations: Map<String, String>.from(
      json['translations'] as Map? ?? {},
    ),
  );
}

class I18nBundle {
  const I18nBundle({required this.locale, required this.translations});

  final String locale;
  final Map<String, String> translations;

  String translate(String key, {String? fallback}) =>
      translations[key] ?? fallback ?? key;

  /// Backend overrides win on duplicate keys.
  I18nBundle mergeWith(I18nOverrides overrides) => I18nBundle(
    locale: locale,
    translations: {...translations, ...overrides.translations},
  );

  factory I18nBundle.fromJson(Map<String, dynamic> json) => I18nBundle(
    locale: json['locale'] as String? ?? 'fr',
    translations: Map<String, String>.from(
      json['translations'] as Map? ?? {},
    ),
  );
}
