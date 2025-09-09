import { createFeatureSelector, createSelector } from '@ngrx/store';
import { PageState } from './page.state';

// Le nom de notre "feature" dans le store, ex: 'page'
export const PAGE_FEATURE_KEY = 'appConfig';

// 1. Sélecteur pour la feature entière
const selectPageState = createFeatureSelector<PageState>(PAGE_FEATURE_KEY);

// 2. Sélecteurs pour chaque propriété du state
export const selectPage = createSelector(selectPageState, state => state.page);

export const selectIsLoading = createSelector(selectPageState, state => state.loading);

export const selectError = createSelector(selectPageState, state => state.error);

// 3. Sélecteurs "dérivés" pour un accès direct aux parties de la page
export const selectLayout = createSelector(
  selectPage,
  page => page?.layout, // Utilise l'optional chaining pour la sécurité
);

export const selectHeader = createSelector(selectPage, page => page?.header);

export const selectFooter = createSelector(selectPage, page => page?.footer);

export const selectI18n = createSelector(selectPage, page => page?.i18n);

export const selectBackendLangs = createSelector(selectPage, page => page?.langs);
export const selectBackendCurrent = createSelector(selectPage, page => page?.currentLang);
export const selectSidenav = createSelector(selectPage, p => p?.nav?.sidenav ?? []);
export const selectFeatures = createSelector(selectPage, p => new Set(p?.features ?? []));
