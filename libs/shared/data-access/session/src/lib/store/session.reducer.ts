import { createReducer, on } from '@ngrx/store';
import { SessionActions } from './session.actions';
import { initialSessionState, SessionState } from './session.state';

export const SESSION_FEATURE_KEY = 'session';
export const sessionReducer = createReducer(
  initialSessionState,
  on(
    SessionActions.loginSuccess,
    (s, { user, claims }): SessionState => ({
      loading: false,
      error: null,
      authenticated: true,
      user: user,
      claims: claims,
    }),
  ),
  on(SessionActions.authCallbackStart, s => ({ ...s, loading: true })),
  on(SessionActions.authCallbackSuccess, (s, { user, claims }) => ({
    authenticated: true,
    user,
    claims,
    loading: false,
    error: null,
  })),
  on(SessionActions.authCallbackFailure, s => ({ ...initialSessionState, loading: false, error: s.error ?? 'auth_error' })),

  on(SessionActions.logout, (): SessionState => initialSessionState),
);
