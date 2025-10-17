// i18n.reducer.ts
import { createFeature, createReducer, createSelector, on } from '@ngrx/store';
import { I18nActions } from './i18n.actions';
import { i18nInitial } from './i18n.state';
import { I18N_FEATURE_KEY } from './i18n.selectors';

const reducer = createReducer(
  i18nInitial,
  on(I18nActions.initFromPage, (s, { langs, current }) => ({ ...s, langs, current })),
  on(I18nActions.setCurrent, (s, { lang }) => ({ ...s, current: lang })),
);

export const i18nFeature = createFeature({
  name: I18N_FEATURE_KEY,
  reducer,
  extraSelectors: ({ selectI18nState }) => ({
    selectHasError: createSelector(selectI18nState, s => s.error != null),
  }),
});
