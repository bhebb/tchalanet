import { registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeFr from '@angular/common/locales/fr';
import localeFrHt from '@angular/common/locales/fr-HT';
import localeHt from '@angular/common/locales/ht';
import { EnvironmentProviders, LOCALE_ID, makeEnvironmentProviders } from '@angular/core';

let registered = false;

export function provideTchAngularLocales(defaultLocale = 'fr'): EnvironmentProviders {
  registerTchAngularLocaleData();
  return makeEnvironmentProviders([{ provide: LOCALE_ID, useValue: defaultLocale }]);
}

export function registerTchAngularLocaleData(): void {
  if (registered) {
    return;
  }

  registerLocaleData(localeFr, 'fr');
  registerLocaleData(localeFr, 'fr-FR');
  registerLocaleData(localeFrHt, 'fr-HT');
  registerLocaleData(localeHt, 'ht');
  registerLocaleData(localeEn, 'en');
  registerLocaleData(localeEn, 'en-US');
  registered = true;
}
