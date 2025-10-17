// session.feature.ts
import { createFeature, createSelector } from '@ngrx/store';
import { SESSION_FEATURE_KEY, sessionReducer } from './session.reducer';
import { SessionState } from './session.state';

export const sessionFeature = createFeature({
  name: SESSION_FEATURE_KEY,
  reducer: sessionReducer,
});

export const {
  selectSessionState,
  selectAuthenticated,
  selectUser,
  selectClaims,
  selectLoading,
  selectError,
} = sessionFeature;

// Custom selectors
export const selectHasError = createSelector(selectError, e => e != null);
export const selectTenantId = createSelector(
  selectSessionState,
  (s: SessionState) => s.claims?.tenantId ?? 'default',
);
export const selectRoles = createSelector(
  selectSessionState,
  (s: SessionState) => s.claims?.roles ?? [],
);
export const hasRole = (role: string) => createSelector(selectRoles, roles => roles.includes(role));
