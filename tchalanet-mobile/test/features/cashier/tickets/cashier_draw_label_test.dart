import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/draw_identity_label.dart';
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

  test('keeps official provider name and localizes slot', () {
    const translations = I18nBundle(
      locale: 'ht',
      translations: {
        'pos.draw.providers.ny': 'Nouyòk',
        'pos.draw.slots.midday': 'Midi',
      },
    );

    expect(localizedCashierDrawLabel(draw, translations), 'New York · Midi');
  });

  test(
    'ticket draw identity keeps official provider name and localizes slot',
    () {
      const translations = I18nBundle(
        locale: 'ht',
        translations: {
          'pos.draw.providers.ny': 'Nouyòk',
          'pos.draw.slots.midday': 'Midi',
        },
      );

      expect(
        localizedDrawIdentityLabel(
          providerCode: 'NY',
          slotKey: 'NY_MID',
          fallback: 'New York · Midday',
          translations: translations,
        ),
        'New York · Midi',
      );
    },
  );

  test('ticket draw identity derives provider from slot key when missing', () {
    const translations = I18nBundle(
      locale: 'ht',
      translations: {'pos.draw.slots.evening': 'Aswè'},
    );

    expect(
      localizedDrawIdentityLabel(
        providerCode: null,
        slotKey: 'FL_EVE',
        fallback: 'Tiraj',
        translations: translations,
      ),
      'Florida · Aswè',
    );
  });

  test('ticket draw identity keeps numeric slot codes as displayable time', () {
    const translations = I18nBundle(locale: 'ht', translations: {});

    expect(
      localizedDrawIdentityLabel(
        providerCode: null,
        slotKey: 'HT_TX_2212',
        fallback: 'Tiraj',
        translations: translations,
      ),
      'Texas · 22:12',
    );
  });
}
