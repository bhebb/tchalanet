import { TchClaim, UserSession } from '@tchl/types';

export interface SessionState {
  authenticated: boolean;
  user: UserSession | null;
  claims: TchClaim | null;
  loading: boolean;
  error: any | null;
}
export const initialSessionState: SessionState = {
  authenticated: false,
  loading: false,
  error: null,
  user: null,
  claims: null,
};
