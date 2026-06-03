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

  /// Maps the server's I18nBundleView (surfaces grouped) to a flat overrides map.
  /// All surfaces are merged into a single translation map — later surfaces win.
  factory I18nOverrides.fromBundleView(Map<String, dynamic> json) {
    final locale = json['locale'] as String? ?? '';
    final surfaces = json['surfaces'] as Map<String, dynamic>? ?? {};
    final flat = <String, String>{};
    for (final surfaceMap in surfaces.values) {
      if (surfaceMap is Map) {
        flat.addAll(Map<String, String>.from(surfaceMap));
      }
    }
    return I18nOverrides(locale: locale, translations: flat);
  }
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
