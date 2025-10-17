import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, mergeMap, of, tap } from 'rxjs';
import { PageActions } from './page.actions';
import { HttpClient } from '@angular/common/http';
import { ThemeService } from '@tchl/ui/theme';
import { PageModel } from '@tchl/types';
import { Router } from '@angular/router';
import { I18nFacade } from '@tchl/facades';
import { PageApi } from '@tchl/api';
import { selectPendingTarget } from '../navigation/nav.reducer';
import { Store } from '@ngrx/store';
import { NavAfterLoadActions } from '../navigation/nav.actions';
import { concatLatestFrom } from '@ngrx/operators';

@Injectable()
export class PageEffects {
  private http = inject(HttpClient);
  private theme = inject(ThemeService);
  private router = inject(Router);
  private actions$ = inject(Actions);
  private pageApi = inject(PageApi);
  private i18nFacade = inject(I18nFacade);
  private store = inject(Store);

  loadPage$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PageActions.loadPage),
      exhaustMap(({ context, tenantId }) =>
        this.pageApi.getPage(context, tenantId).pipe(
          map(page => PageActions.loadPageSuccess({ page })),
          catchError(error => of(PageActions.loadPageFailure({ error }))),
        ),
      ),
    ),
  );

  fallback$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PageActions.loadPageFailure, PageActions.fallbackRequested),
      mergeMap(() =>
        this.http.get<PageModel>('/assets/config/page-default.json').pipe(
          map(page => PageActions.fallbackSucceeded({ page })),
          catchError(() => of(PageActions.navigateNotFound())),
        ),
      ),
    ),
  );

  applyPageConfiguration = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PageActions.loadPageSuccess, PageActions.fallbackSucceeded),
        concatLatestFrom(() => this.store.select(selectPendingTarget)),
        tap(([action, target]) => {
          const page = (action as { page: PageModel }).page;
          const currentLang = page.currentLang;
          this.i18nFacade.initFromPage(page.langs, currentLang, page.i18n);
          this.theme.applyPublicTheme(page.theme);
          if (!target) return;
          this.router.navigateByUrl(target);
          this.store.dispatch(NavAfterLoadActions.clear());
        }),
      ),
    { dispatch: false },
  );

  navigateNotFound$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PageActions.navigateNotFound),
        tap(() => this.router.navigate(['/404'])),
      ),
    { dispatch: false },
  );
}
