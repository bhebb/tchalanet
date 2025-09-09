// Le nom de notre "feature" dans le store, ex: 'page'
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { I18nState } from './i18n.state';

export const I18N_FEATURE_KEY = 'i18n';

// 1. Sélecteur pour la feature entière
const selectI18nState = createFeatureSelector<I18nState>(I18N_FEATURE_KEY);

// 2. Sélecteurs pour chaque propriété du state
export const selectCurrentLang = createSelector(selectI18nState, state => state.current);

export const selectAvailableLangs = createSelector(selectI18nState, state => state.langs);
