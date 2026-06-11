import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

/// Flutter does not currently ship Material/Cupertino translations for `ht`.
///
/// Keep framework controls functional in Haitian Creole mode by explicitly using
/// the French framework bundle until native Haitian delegates are implemented.
class HaitianMaterialLocalizationsDelegate
    extends LocalizationsDelegate<MaterialLocalizations> {
  const HaitianMaterialLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => locale.languageCode == 'ht';

  @override
  Future<MaterialLocalizations> load(Locale locale) =>
      GlobalMaterialLocalizations.delegate.load(const Locale('fr'));

  @override
  bool shouldReload(HaitianMaterialLocalizationsDelegate old) => false;
}

class HaitianCupertinoLocalizationsDelegate
    extends LocalizationsDelegate<CupertinoLocalizations> {
  const HaitianCupertinoLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => locale.languageCode == 'ht';

  @override
  Future<CupertinoLocalizations> load(Locale locale) =>
      GlobalCupertinoLocalizations.delegate.load(const Locale('fr'));

  @override
  bool shouldReload(HaitianCupertinoLocalizationsDelegate old) => false;
}
