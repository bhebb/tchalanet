import { createFeature, createReducer, on } from '@ngrx/store';

import { I18nActions } from './i18n.actions';
import { i18nInitialState } from './i18n.state';

export const i18nFeature = createFeature({
  name: 'i18n',
  reducer: createReducer(
    i18nInitialState,
    on(I18nActions.init, (state, { languages, defaultLanguage }) => ({
      ...state,
      currentLanguage: defaultLanguage,
      languages,
    })),
    on(I18nActions.setCurrent, (state, { language }) => ({
      ...state,
      currentLanguage: language,
      error: null,
    })),
    on(I18nActions.setError, (state, { error }) => ({
      ...state,
      error,
    })),
  ),
});
