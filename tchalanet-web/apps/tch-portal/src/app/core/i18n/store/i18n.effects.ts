import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { EMPTY, catchError, map, switchMap, tap } from 'rxjs';

import { I18nActions } from './i18n.actions';

const languageStorageKey = 'tchalanet.web.language';

@Injectable()
export class I18nEffects {
  private readonly actions$ = inject(Actions);
  private readonly document = inject(DOCUMENT);
  private readonly store = inject(Store);
  private readonly translate = inject(TranslateService);

  readonly init$ = createEffect(() =>
    this.actions$.pipe(
      ofType(I18nActions.init),
      map(({ languages, defaultLanguage }) => {
        this.translate.addLangs([...languages]);
        const stored = localStorage.getItem(languageStorageKey);
        const language = stored && languages.includes(stored) ? stored : defaultLanguage;

        return I18nActions.setCurrent({ language });
      }),
    ),
  );

  readonly setCurrent$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(I18nActions.setCurrent),
        switchMap(({ language }) =>
          this.translate.use(language).pipe(
            tap(() => {
              this.document.documentElement.lang = language;
              localStorage.setItem(languageStorageKey, language);
            }),
            catchError((error: unknown) => {
              this.store.dispatch(I18nActions.setError({ error }));
              return EMPTY;
            }),
          ),
        ),
      ),
    { dispatch: false },
  );
}
