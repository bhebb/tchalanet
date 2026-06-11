import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _localePreferenceKey = 'app_locale';

abstract interface class LocaleRepository {
  String? readSavedLocale();

  Future<void> saveLocale(String locale);
}

class SharedPreferencesLocaleRepository implements LocaleRepository {
  const SharedPreferencesLocaleRepository(this._preferences);

  final SharedPreferences _preferences;

  @override
  String? readSavedLocale() => _preferences.getString(_localePreferenceKey);

  @override
  Future<void> saveLocale(String locale) async {
    await _preferences.setString(_localePreferenceKey, locale);
  }
}

class EmptyLocaleRepository implements LocaleRepository {
  const EmptyLocaleRepository();

  @override
  String? readSavedLocale() => null;

  @override
  Future<void> saveLocale(String locale) async {}
}

final localeRepositoryProvider = Provider<LocaleRepository>(
  (ref) => const EmptyLocaleRepository(),
);

Future<LocaleRepository> loadLocaleRepository() async {
  try {
    return SharedPreferencesLocaleRepository(
      await SharedPreferences.getInstance(),
    );
  } catch (_) {
    return const EmptyLocaleRepository();
  }
}
