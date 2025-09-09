import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { TranslateService } from '@ngx-translate/core';
import { I18nActions } from './i18n.actions';
import { map, tap } from 'rxjs';
import { LANG_KEY_ATTRIBUTE, LANG_KEY_STORAGE } from '@tchl/web/constants';

@Injectable()
export class I18nEffects {
  private actions$ = inject(Actions);
  private t = inject(TranslateService);

  initFromPage$ = createEffect(() =>
    this.actions$.pipe(
      ofType(I18nActions.initFromPage),
      map(({ langs, current }) => {
        this.t.addLangs(langs);
        const stored = localStorage.getItem(LANG_KEY_STORAGE);
        const lang = stored && langs.includes(stored) ? stored : current;
        return I18nActions.setCurrent({ lang });
      }),
    ),
  );

  setCurrent$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(I18nActions.setCurrent),
        tap(({ lang }) => {
          this.t.use(lang);
          document.documentElement.setAttribute(LANG_KEY_ATTRIBUTE, lang);
          localStorage.setItem(LANG_KEY_STORAGE, lang);
        }),
      ),
    { dispatch: false },
  );
}
