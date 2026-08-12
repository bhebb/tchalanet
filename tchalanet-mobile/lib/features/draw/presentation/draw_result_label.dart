import '../../../core/i18n/draw_identity_label.dart';
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

/// Resolves stable result identifiers for seller-facing views. Provider names
/// stay official while slot labels follow the active locale.
/// The server label remains a fallback for catalog entries unknown to this client.
String localizedPublicDrawResultLabel(
  String providerCode,
  String slotKey,
  String fallback,
  I18nBundle translations,
) {
  return localizedDrawIdentityLabel(
    providerCode: providerCode,
    slotKey: slotKey,
    fallback: fallback,
    translations: translations,
  );
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
