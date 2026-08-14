import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Actions } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';
import { afterEach } from 'vitest';

import { mergeTranslationTrees, normalizeBackendTranslations } from './merged-translate-loader';
import { I18nActions } from './store/i18n.actions';
import { I18nEffects, TCH_I18N_LANGUAGE_STORAGE_KEY } from './store/i18n.effects';
import { i18nFeature } from './store/i18n.reducer';
import { i18nInitialState } from './store/i18n.state';

describe('i18n runtime', () => {
  const testLanguageStorageKey = 'tchalanet.web.test.language';

  afterEach(() => {
    localStorage.removeItem(testLanguageStorageKey);
    localStorage.removeItem('tchalanet.web.language');
  });

  it('preserves the selected language when init is reduced repeatedly', () => {
    const initializedState = i18nFeature.reducer(
      { ...i18nInitialState, currentLanguage: 'ht', initialized: true },
      I18nActions.init({ defaultLanguage: 'fr', languages: ['fr', 'en', 'ht'] }),
    );

    expect(initializedState.currentLanguage).toBe('ht');
    expect(initializedState.initialized).toBe(true);
  });

  it('calls TranslateService.use when the current language changes', () => {
    const actions$ = new Subject<ReturnType<typeof I18nActions.setCurrent>>();
    const translate = {
      addLangs: vi.fn(),
      use: vi.fn().mockReturnValue(of({})),
    };
    const store = {
      dispatch: vi.fn(),
    };
    const document = {
      documentElement: {
        lang: 'fr',
      },
    };

    TestBed.configureTestingModule({
      providers: [
        I18nEffects,
        { provide: Actions, useValue: new Actions(actions$) },
        { provide: DOCUMENT, useValue: document },
        { provide: Store, useValue: store },
        { provide: TranslateService, useValue: translate },
        { provide: TCH_I18N_LANGUAGE_STORAGE_KEY, useValue: testLanguageStorageKey },
      ],
    });

    const effects = TestBed.inject(I18nEffects);
    const subscription = effects.setCurrent$.subscribe();

    actions$.next(I18nActions.setCurrent({ language: 'ht' }));

    expect(translate.use).toHaveBeenCalledWith('ht');
    expect(document.documentElement.lang).toBe('ht');
    expect(localStorage.getItem(testLanguageStorageKey)).toBe('ht');
    expect(localStorage.getItem('tchalanet.web.language')).toBeNull();
    subscription.unsubscribe();
  });

  it('uses the configured app language storage key during initialization', () => {
    localStorage.setItem('tchalanet.web.language', 'ht');
    localStorage.setItem(testLanguageStorageKey, 'fr');

    const actions$ = new Subject<ReturnType<typeof I18nActions.init>>();
    const translate = {
      addLangs: vi.fn(),
      use: vi.fn().mockReturnValue(of({})),
    };
    const store = {
      dispatch: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        I18nEffects,
        { provide: Actions, useValue: new Actions(actions$) },
        { provide: DOCUMENT, useValue: { documentElement: { lang: '' } } },
        { provide: Store, useValue: store },
        { provide: TranslateService, useValue: translate },
        { provide: TCH_I18N_LANGUAGE_STORAGE_KEY, useValue: testLanguageStorageKey },
      ],
    });

    const effects = TestBed.inject(I18nEffects);
    const emitted: unknown[] = [];
    const subscription = effects.init$.subscribe(action => emitted.push(action));

    actions$.next(I18nActions.init({ defaultLanguage: 'en', languages: ['fr', 'en', 'ht'] }));

    expect(emitted).toEqual([I18nActions.setCurrent({ language: 'fr' })]);
    subscription.unsubscribe();
  });


  it('merges backend surfaces in configured common-to-specific order', () => {
    const translations = normalizeBackendTranslations(
      {
        data: {
          locale: 'fr',
          surfaces: {
            PUBLIC_HOME: {
              'home.title': 'Accueil',
              'shared.cta': 'Entrer',
            },
            PUBLIC_COMMON: {
              'common.ok': 'OK',
              'shared.cta': 'Continuer',
            },
          },
        },
      },
      ['PUBLIC_COMMON', 'PUBLIC_HOME'],
    );

    expect(translations).toEqual({
      'common.ok': 'OK',
      'home.title': 'Accueil',
      'shared.cta': 'Entrer',
    });
  });

  it('deep-merges local bundle trees in configured order', () => {
    const translations = mergeTranslationTrees([
      {
        common: {
          action: {
            save: 'Save',
            refresh: 'Refresh',
          },
        },
        feature: {
          dashboard: {
            title: 'Dashboard',
          },
        },
      },
      {
        common: {
          action: {
            save: 'Save now',
          },
        },
        feature: {
          dashboard: {
            subtitle: 'Today',
          },
        },
      },
    ]);

    expect(translations).toEqual({
      common: {
        action: {
          save: 'Save now',
          refresh: 'Refresh',
        },
      },
      feature: {
        dashboard: {
          title: 'Dashboard',
          subtitle: 'Today',
        },
      },
    });
  });
});
