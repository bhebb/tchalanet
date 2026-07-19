import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/cashier_draw_label.dart';

void main() {
  const draw = CashierAvailableDrawView(
    drawId: 'draw-1',
    drawChannelId: 'channel-1',
    resultSlotKey: 'HT_NY_MID',
    channelLabel: 'Haïti • NY • Midday',
    gameCodes: ['BORLETTE'],
    status: 'OPEN',
    providerDate: '2026-07-19',
    providerTime: '12:30',
    providerTimezone: 'America/New_York',
    localDate: '2026-07-19',
    localTime: '12:30',
    localTimezone: 'America/Port-au-Prince',
  );

  test('localizes the provider and slot instead of rendering catalog code', () {
    const translations = I18nBundle(
      locale: 'ht',
      translations: {
        'pos.draw.providers.ny': 'Nouyòk',
        'pos.draw.slots.midday': 'Midi',
      },
    );

    expect(localizedCashierDrawLabel(draw, translations), 'Nouyòk · Midi');
  });
}
