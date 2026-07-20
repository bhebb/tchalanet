import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/features/draw/data/models/draw_models.dart';
import 'package:tchalanet_mobile/features/draw/presentation/draw_result_label.dart';

void main() {
  const haitianCreole = I18nBundle(
    locale: 'ht',
    translations: {
      'pos.draw.providers.ny': 'Nouyòk',
      'pos.draw.slots.midday': 'Midi',
    },
  );

  test('result history decoding tolerates absent optional values', () {
    final history = PublicDrawResultHistory.fromJson({
      'items': [
        {
          'drawResultId': 'result-1',
          'slotKey': 'NY_MID',
          'provider': 'NY',
          'drawChannelLabel': null,
          'resultDate': '2026-07-19',
          'drawTime': null,
          'status': 'CONFIRMED',
          'numbers': ['123', null, '45'],
        },
      ],
      'page': 0,
      'totalItems': 1,
      'totalPages': 1,
    });

    expect(history.items, hasLength(1));
    expect(history.items.single.drawChannelLabel, isEmpty);
    expect(history.items.single.displayDrawTime, isEmpty);
    expect(history.items.single.numbers, ['123', '45']);
  });

  test('result labels resolve provider and slot in the active locale', () {
    final label = localizedPublicDrawResultLabel(
      'NY',
      'NY_MID',
      'New York · Midday',
      haitianCreole,
    );

    expect(label, 'Nouyòk · Midi');
  });

  test('unknown result slots retain the safe server display fallback', () {
    final label = localizedPublicDrawResultLabel(
      'ZZ',
      'ZZ_UNKNOWN',
      'Unknown provider · Unknown slot',
      haitianCreole,
    );

    expect(label, 'Unknown provider · Unknown slot');
  });
}
