import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/core/i18n/locale_repository.dart';

class _FakeLocaleRepository implements LocaleRepository {
  _FakeLocaleRepository({this.savedLocale});

  String? savedLocale;
  final writes = <String>[];

  @override
  String? readSavedLocale() => savedLocale;

  @override
  Future<void> saveLocale(String locale) async {
    savedLocale = locale;
    writes.add(locale);
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('startup locale prefers a supported saved locale', () {
    expect(resolveStartupLocale(savedLocale: 'fr', deviceLocale: 'en'), 'fr');
  });

  test(
    'startup locale uses a supported device locale without a saved choice',
    () {
      expect(resolveStartupLocale(savedLocale: null, deviceLocale: 'en'), 'en');
      expect(resolveStartupLocale(savedLocale: 'es', deviceLocale: 'fr'), 'fr');
    },
  );

  test('startup locale falls back to Haitian Creole', () {
    expect(
      resolveStartupLocale(savedLocale: null, deviceLocale: 'es'),
      defaultLocale,
    );
    expect(
      resolveStartupLocale(savedLocale: 'es', deviceLocale: 'pt'),
      defaultLocale,
    );
  });

  test('locale controller restores and persists supported changes', () async {
    final repository = _FakeLocaleRepository(savedLocale: 'fr');
    final container = ProviderContainer(
      overrides: [localeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);

    expect(container.read(localeProvider), 'fr');

    await container.read(localeProvider.notifier).setLocale('en');

    expect(container.read(localeProvider), 'en');
    expect(repository.writes, ['en']);
  });

  test('locale controller ignores unsupported and duplicate changes', () async {
    final repository = _FakeLocaleRepository(savedLocale: 'ht');
    final container = ProviderContainer(
      overrides: [localeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final controller = container.read(localeProvider.notifier);

    await controller.setLocale('es');
    await controller.setLocale('ht');

    expect(container.read(localeProvider), 'ht');
    expect(repository.writes, isEmpty);
  });

  test(
    'shared preferences repository survives a new repository instance',
    () async {
      SharedPreferences.setMockInitialValues({});
      final preferences = await SharedPreferences.getInstance();
      final repository = SharedPreferencesLocaleRepository(preferences);

      await repository.saveLocale('fr');

      final restored = SharedPreferencesLocaleRepository(
        await SharedPreferences.getInstance(),
      );
      expect(restored.readSavedLocale(), 'fr');
    },
  );

  testWidgets('changing locale updates listening widgets without restart', (
    tester,
  ) async {
    final repository = _FakeLocaleRepository(savedLocale: 'ht');
    late WidgetRef appRef;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [localeRepositoryProvider.overrideWithValue(repository)],
        child: Consumer(
          builder: (context, ref, _) {
            appRef = ref;
            final locale = ref.watch(localeProvider);
            return MaterialApp(locale: Locale(locale), home: Text(locale));
          },
        ),
      ),
    );

    expect(find.text('ht'), findsOneWidget);

    await appRef.read(localeProvider.notifier).setLocale('fr');
    await tester.pump();

    expect(find.text('fr'), findsOneWidget);
    expect(repository.savedLocale, 'fr');
  });
}
