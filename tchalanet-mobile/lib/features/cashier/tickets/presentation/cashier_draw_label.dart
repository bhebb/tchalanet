import 'package:tchalanet_mobile/core/i18n/draw_identity_label.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/provider_identity_label.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';

/// Seller-facing draw identity. The backend supplies stable provider/slot codes;
/// provider names stay official while slot labels follow the active locale.
String localizedCashierDrawLabel(
  CashierAvailableDrawView draw,
  I18nBundle translations,
) => localizedDrawIdentityLabel(
  providerCode: draw.providerCode,
  slotKey: draw.resultSlotKey,
  channelCode: draw.channelCode,
  fallback: draw.displayChannelLabel,
  translations: translations,
);

String localizedCashierProviderLabel(
  String providerCode,
  I18nBundle translations,
) => officialProviderName(providerCode);
