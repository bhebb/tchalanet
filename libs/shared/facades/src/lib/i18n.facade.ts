import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { I18nActions, selectAvailableLangs, selectCurrentLang } from '@tchl/data-access/i18n';

@Injectable({ providedIn: 'root' })
export class I18nFacade {
  private store = inject(Store);
  private translateService = inject(TranslateService);
  available = this.store.selectSignal(selectAvailableLangs);
  current = this.store.selectSignal(selectCurrentLang);

  initFromPage(langs: string[], currentLang: string, backendBundle?: any) {
    this.store.dispatch(I18nActions.initFromPage({ langs, current: currentLang }));
  }

  setCurrent(lang: string) {
    this.store.dispatch(I18nActions.setCurrent({ lang }));
  }

  instant(key: string, params?: any) {
    return this.translateService.instant(key, params);
  }

  label(l: string) {
    return ({ fr: 'Français', en: 'English', ht: 'kreyòl' } as any)[l] ?? l;
  }

  applyOverrides(i18nOverrides: any, locale: string) {
    console.log('applyOverrides', i18nOverrides, 'local', locale);
  }
}
