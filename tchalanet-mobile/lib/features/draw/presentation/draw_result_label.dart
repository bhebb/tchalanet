import '../../../core/i18n/i18n_models.dart';
import '../data/models/draw_models.dart';

enum DrawResultDisplayStatus {
  confirmed,
  provisional,
  corrected,
  unavailable,
  unknown,
}

enum DrawResultDisplayQuality { complete, suspect, invalid, unknown }

DrawResultDisplayStatus drawResultDisplayStatus(String status) =>
    switch (status) {
      'CONFIRMED' => DrawResultDisplayStatus.confirmed,
      'PROVISIONAL' => DrawResultDisplayStatus.provisional,
      'OVERRIDDEN' => DrawResultDisplayStatus.corrected,
      'ERROR' => DrawResultDisplayStatus.unavailable,
      _ => DrawResultDisplayStatus.unknown,
    };

DrawResultDisplayQuality? drawResultDisplayQuality(String? quality) {
  if (quality == null || quality.isEmpty) return null;
  return switch (quality) {
    'COMPLETE' => DrawResultDisplayQuality.complete,
    'SUSPECT' => DrawResultDisplayQuality.suspect,
    'INVALID' => DrawResultDisplayQuality.invalid,
    _ => DrawResultDisplayQuality.unknown,
  };
}

/// Translates stable result slot/provider identifiers for seller-facing views.
/// The server label remains a fallback for catalog entries unknown to this client.
String localizedPublicDrawResultLabel(
  String providerCode,
  String slotKey,
  String fallback,
  I18nBundle translations,
) {
  final providerKey = 'pos.draw.providers.${providerCode.toLowerCase()}';
  final provider = translations.translate(providerKey);
  final slot = _slotLabel(slotKey, translations);
  if (provider == providerKey || slot == null) return fallback;
  return '$provider · $slot';
}

String localizedPublicDrawResultRowLabel(
  PublicDrawResultRow result,
  I18nBundle translations,
) => localizedPublicDrawResultLabel(
  result.provider,
  result.slotKey,
  result.drawChannelLabel,
  translations,
);

String localizedPublicDrawResultSlotLabel(
  PublicDrawResultSlot slot,
  I18nBundle translations,
) => localizedPublicDrawResultLabel(
  slot.provider,
  slot.slotKey,
  slot.label,
  translations,
);

String? _slotLabel(String slotKey, I18nBundle translations) {
  final normalized = slotKey.toLowerCase();
  final key = switch (normalized) {
    final key when key.contains('mid') => 'midday',
    final key when key.contains('eve') => 'evening',
    final key when key.contains('morning') => 'morning',
    final key when key.contains('late') => 'late',
    final key when key.contains('day') => 'day',
    _ => null,
  };
  if (key == null) return null;
  final translationKey = 'pos.draw.slots.$key';
  final label = translations.translate(translationKey);
  return label == translationKey ? null : label;
}
