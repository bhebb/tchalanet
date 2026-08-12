import 'i18n_models.dart';
import 'provider_identity_label.dart';

/// Resolves provider/slot identity from stable catalog codes.
///
/// A server display label is deliberately only a fallback. Provider names stay
/// official while slot labels follow the active locale.
String localizedDrawIdentityLabel({
  required String? providerCode,
  required String? slotKey,
  String? channelCode,
  required String fallback,
  required I18nBundle translations,
}) {
  final resolvedProviderCode = drawProviderCodeFrom(
    providerCode: providerCode,
    slotKey: slotKey,
    channelCode: channelCode,
  );
  final provider = officialProviderName(resolvedProviderCode);
  final slot = _localizedSlot(slotKey ?? channelCode, translations);
  if (provider.isEmpty || slot == null) {
    return _nonGenericFallback(fallback, slotKey, channelCode);
  }
  return '$provider · $slot';
}

String? _localizedSlot(String? slotKey, I18nBundle translations) {
  final rawSlot = _rawSlotCode(slotKey);
  if (rawSlot == null) return null;
  final slot = switch (rawSlot.toLowerCase()) {
    'mid' || 'midday' => 'midday',
    'eve' || 'evening' => 'evening',
    'morning' => 'morning',
    'late' || 'night' => 'late',
    'day' => 'day',
    _ => null,
  };
  if (RegExp(r'^\d{3,4}$').hasMatch(rawSlot)) return _timeSlotLabel(rawSlot);
  if (slot == null) return rawSlot;
  final key = 'pos.draw.slots.$slot';
  final label = translations.translate(key);
  return label == key ? rawSlot : label;
}

String _nonGenericFallback(
  String fallback,
  String? slotKey,
  String? channelCode,
) {
  final cleaned = fallback.trim();
  if (cleaned.isNotEmpty && !_isGenericDrawLabel(cleaned)) return cleaned;
  return channelCode?.trim().isNotEmpty == true
      ? channelCode!.trim()
      : slotKey?.trim().isNotEmpty == true
      ? slotKey!.trim()
      : cleaned;
}

String? _rawSlotCode(String? value) {
  final parts = value
      ?.trim()
      .split(RegExp(r'[_-]'))
      .where((part) => part.trim().isNotEmpty)
      .toList(growable: false);
  if (parts == null || parts.isEmpty) return null;
  return parts.last.toUpperCase();
}

String _timeSlotLabel(String rawSlot) {
  final padded = rawSlot.padLeft(4, '0');
  return '${padded.substring(0, 2)}:${padded.substring(2, 4)}';
}

bool _isGenericDrawLabel(String value) {
  final cleaned = value.trim().toLowerCase();
  return cleaned == 'draw' || cleaned == 'tiraj' || cleaned == 'tirage';
}
