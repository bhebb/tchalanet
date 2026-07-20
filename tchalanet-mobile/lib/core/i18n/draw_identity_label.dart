import 'i18n_models.dart';

/// Resolves provider/slot identity from stable catalog codes.
///
/// A server display label is deliberately only a fallback: seller-facing POS
/// screens stay in the active locale even when the catalog was seeded in a
/// different language.
String localizedDrawIdentityLabel({
  required String? providerCode,
  required String? slotKey,
  required String fallback,
  required I18nBundle translations,
}) {
  final normalizedProvider = providerCode?.trim().toLowerCase() ?? '';
  final providerKey = 'pos.draw.providers.$normalizedProvider';
  final provider = translations.translate(providerKey);
  final slot = _localizedSlot(slotKey, translations);
  if (normalizedProvider.isEmpty || provider == providerKey || slot == null) {
    return fallback;
  }
  return '$provider · $slot';
}

String? _localizedSlot(String? slotKey, I18nBundle translations) {
  final normalized = slotKey?.toLowerCase() ?? '';
  final slot = switch (normalized) {
    final key when key.contains('mid') => 'midday',
    final key when key.contains('eve') => 'evening',
    final key when key.contains('morning') => 'morning',
    final key when key.contains('late') => 'late',
    final key when key.contains('day') => 'day',
    _ => null,
  };
  if (slot == null) return null;
  final key = 'pos.draw.slots.$slot';
  final label = translations.translate(key);
  return label == key ? null : label;
}
