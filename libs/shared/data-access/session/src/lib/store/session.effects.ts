import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Router } from '@angular/router';
import { mergeMap, of } from 'rxjs';
import { catchError, exhaustMap, map, tap } from 'rxjs/operators';

import { SessionActions } from './session.actions';
import { AuthService } from '@tchl/shared/auth';
import { NavAfterLoadActions, PageActions } from '@tchl/data-access/page';

@Injectable()
export class SessionEffects {
  private actions$ = inject(Actions);
  private auth = inject(AuthService);
  private router = inject(Router);

  authCallback$ = createEffect(() =>
    this.actions$.pipe(
      ofType(SessionActions.authCallbackStart),
      exhaustMap(() => this.auth.checkAuth$()),
      map(login => {
        if (!login?.isAuthenticated)
          return SessionActions.authCallbackFailure({ error: 'not_authenticated' });
        this.auth.hydrateFrom(login);
        const tch = this.auth.tch();
        const user = {
          id: (login.userData as any)?.sub ?? '',
          username: (login.userData as any)?.preferred_username ?? '',
          email: (login.userData as any)?.email,
        };
        return SessionActions.authCallbackSuccess({ user, claims: tch! });
      }),
      catchError(err => of(SessionActions.authCallbackFailure({ error: err }))),
    ),
  );

  // 2) After success: apply i18n/theme, load context, then go to dashboard
  afterSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(SessionActions.authCallbackSuccess),
      mergeMap(({ claims }) => [
        PageActions.loadPage({ context: 'private', tenantId: claims.tenantId }),
        NavAfterLoadActions.request({ target: this.auth.consumeLoginTarget() }),
      ]),
    ),
  );

  // 3) On failure: back to home (or show an error page)
  afterFailure$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(SessionActions.authCallbackFailure),
        tap(() => this.router.navigateByUrl('/')),
      ),
    { dispatch: false },
  );
}
