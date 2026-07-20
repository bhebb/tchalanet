import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';

/// Seller-facing draw identity. The backend supplies stable provider/slot codes;
/// this layer turns them into the active locale instead of rendering raw catalog
/// labels such as "NY · Midday".
String localizedCashierDrawLabel(
  CashierAvailableDrawView draw,
  I18nBundle translations,
) {
  final provider = localizedCashierProviderLabel(
    draw.providerCode,
    translations,
  );
  final slotKey = _slotKey(draw.displayChannelLabel);
  if (provider == draw.providerCode || slotKey == null) {
    return draw.displayChannelLabel;
  }

  final slotTranslationKey = 'pos.draw.slots.$slotKey';
  final slot = translations.translate(slotTranslationKey);
  if (slot == slotTranslationKey) return draw.displayChannelLabel;
  return '$provider · $slot';
}

String localizedCashierProviderLabel(
  String providerCode,
  I18nBundle translations,
) {
  final providerKey = 'pos.draw.providers.${providerCode.toLowerCase()}';
  final provider = translations.translate(providerKey);
  return provider == providerKey ? providerCode : provider;
}

String? _slotKey(String label) {
  final normalized = label.toLowerCase();
  if (normalized.contains('midday') || normalized.contains('midi')) {
    return 'midday';
  }
  if (normalized.contains('evening') || normalized.contains('soir')) {
    return 'evening';
  }
  if (normalized.contains('morning') || normalized.contains('matin')) {
    return 'morning';
  }
  if (normalized.contains('late') || normalized.contains('tard')) {
    return 'late';
  }
  if (normalized.contains('day') || normalized.contains('jou')) {
    return 'day';
  }
  return null;
}
