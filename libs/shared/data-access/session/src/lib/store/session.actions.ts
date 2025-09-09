import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { TchClaim, UserSession } from '@tchl/types';
import { BootstrapDto } from '../../../../../api/src/lib/api/context.api';

export const SessionActions = createActionGroup({
  source: 'Session',
  events: {
    'Login Success': props<{
      user: UserSession;
      claims: TchClaim;
    }>(),
    'Auth Callback Start': emptyProps,
    'Auth Callback Success': props<{ user: UserSession; claims: TchClaim }>(),
    'Auth Callback Failure': props<{ error?: any }>(),
    'Context Loaded': props<{ context: BootstrapDto }>(),
    'Context Load Failed': props<{ error: any }>(),
    Logout: emptyProps,
  },
});
