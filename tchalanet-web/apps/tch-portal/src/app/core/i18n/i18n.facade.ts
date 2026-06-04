import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';

import { I18nActions } from './store/i18n.actions';
import { i18nFeature } from './store/i18n.reducer';

@Injectable({ providedIn: 'root' })
export class I18nFacade {
  private readonly store = inject(Store);
  private readonly translate = inject(TranslateService);

  readonly currentLanguage = this.store.selectSignal(i18nFeature.selectCurrentLanguage);
  readonly initialized = this.store.selectSignal(i18nFeature.selectInitialized);
  readonly languages = this.store.selectSignal(i18nFeature.selectLanguages);

  init(languages: readonly string[] = ['fr', 'en', 'ht'], defaultLanguage = 'fr'): void {
    if (this.initialized()) {
      return;
    }

    this.store.dispatch(I18nActions.init({ defaultLanguage, languages }));
  }

  setCurrent(language: string): void {
    this.store.dispatch(I18nActions.setCurrent({ language }));
  }

  instant(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params) as string;
  }

  label(language: string): string {
    return (
      {
        en: 'English',
        fr: 'Français',
        ht: 'Kreyòl',
      }[language] ?? language
    );
  }
}
