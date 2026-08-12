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
          'quality': 'COMPLETE',
          'lots': {'LOT1': '123', 'LOT2': null, 'LOT3': null, 'LOT4': '45'},
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
    expect(history.items.single.quality, 'COMPLETE');
    expect(history.items.single.lotValue('LOT1'), '123');
    expect(history.items.single.lotValue('LOT2'), isNull);
    expect(history.items.single.hasKnownLots, isTrue);
    expect(history.items.single.numbers, ['123', '45']);
  });

  test('result status and quality preserve their business meaning', () {
    expect(
      drawResultDisplayStatus('CONFIRMED'),
      DrawResultDisplayStatus.confirmed,
    );
    expect(
      drawResultDisplayStatus('PROVISIONAL'),
      DrawResultDisplayStatus.provisional,
    );
    expect(
      drawResultDisplayStatus('OVERRIDDEN'),
      DrawResultDisplayStatus.corrected,
    );
    expect(
      drawResultDisplayStatus('ERROR'),
      DrawResultDisplayStatus.unavailable,
    );
    expect(
      drawResultDisplayStatus('FUTURE_STATUS'),
      DrawResultDisplayStatus.unknown,
    );
    expect(
      drawResultDisplayQuality('COMPLETE'),
      DrawResultDisplayQuality.complete,
    );
    expect(
      drawResultDisplayQuality('SUSPECT'),
      DrawResultDisplayQuality.suspect,
    );
    expect(
      drawResultDisplayQuality('INVALID'),
      DrawResultDisplayQuality.invalid,
    );
    expect(drawResultDisplayQuality(null), isNull);
  });

  test('result labels keep official provider names and localize slots', () {
    final label = localizedPublicDrawResultLabel(
      'NY',
      'NY_MID',
      'New York · Midday',
      haitianCreole,
    );

    expect(label, 'New York · Midi');
  });

  test('unknown result slots retain stable display codes', () {
    final label = localizedPublicDrawResultLabel(
      'ZZ',
      'ZZ_UNKNOWN',
      'Unknown provider · Unknown slot',
      haitianCreole,
    );

    expect(label, 'ZZ · UNKNOWN');
  });
}
