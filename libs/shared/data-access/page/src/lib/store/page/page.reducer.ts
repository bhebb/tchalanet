import { createFeature, createReducer, on } from '@ngrx/store';
import { PageActions } from './page.actions';
import { initialState } from './page.state';
import { I18N_FEATURE_KEY } from '@tchl/data-access/i18n';
import { PAGE_FEATURE_KEY } from './page.selectors';

const reducer = createReducer(
  initialState,

  // Quand on commence à charger, on passe `loading` à true
  on(PageActions.loadPage, state => ({
    ...state,
    loading: true,
    error: null,
  })),

  // En cas de succès, on stocke les données de la page et on repasse `loading` à false
  on(PageActions.loadPageSuccess, (state, { page }) => ({
    ...state,
    page,
    loading: false,
  })),

  // En cas d'échec, on stocke l'erreur et on repasse `loading` à false
  on(PageActions.loadPageFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  //en cas d'echec fall back
  on(PageActions.fallbackSucceeded, (state, { page }) => ({
    ...state,
    loading: false,
    page,
  })),
);

export const pageFeature = createFeature({ name: PAGE_FEATURE_KEY, reducer });
