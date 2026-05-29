/* eslint-disable simple-import-sort/imports */
import { Routes } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideState } from '@ngrx/store';

import { navAfterLoadFeature } from '@tchl/data-access/page';
import { SessionEffects, sessionFeature } from '@tchl/data-access/session';
import { AuthCallbackComponent, AuthLoginComponent, authGuard } from '@tchl/shared/auth';
import { HomePublicPage } from '@tchl/web/feature-home-public';
import { PUBLIC_ROUTES, PublicShellComponent } from '@tchl/web/public-pages';

export const appRoutes: Routes = [
  {
    path: '',
    component: PublicShellComponent,
    children: [
      {
        path: '',
        component: HomePublicPage,
      },
      { path: '404', loadComponent: () => import('@tchl/ui/layout').then(m => m.NotFoundComponent) },
      ...PUBLIC_ROUTES,
    ],
  },
  {
    path: 'auth/login',
    component: AuthLoginComponent,
  },
  {
    path: 'auth/callback',
    providers: [
      provideEffects([SessionEffects]), // 👈
      provideState(sessionFeature), // 👈
      provideState(navAfterLoadFeature), // 👈
    ],
    component: AuthCallbackComponent,
  },
  {
    path: 'app',
    canMatch: [authGuard],
    loadChildren: () => import('@tchl/web/private-pages').then(m => m.routes),
  },
  {
    path: '**',
    redirectTo: '404',
    pathMatch: 'full',
  },
];
