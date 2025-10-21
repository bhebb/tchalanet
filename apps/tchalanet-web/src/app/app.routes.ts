import { Routes } from '@angular/router';
import { SessionEffects, sessionFeature } from '@tchl/data-access/session';
import { provideState } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { authGuard } from '@tchl/shared/auth';
import { navAfterLoadFeature } from '@tchl/data-access/page';
import { PUBLIC_ROUTES } from '@tchl/web/public-pages';

export const appRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('@tchl/web/public-pages').then(m => m.PublicShellComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('@tchl/web/feature-home-public').then(m => m.HomePublicPage),
      },
      {
        path: 'results',
        loadComponent: () => import('@tchl/search').then(m => m.SearchResultsPage),
      },
      { path: '404', loadComponent: () => import('@tchl/ui/layout').then(m => m.NotFoundComponent) },
      ...PUBLIC_ROUTES,
    ],
  },
  {
    path: 'auth/login',
    loadComponent: () => import('@tchl/shared/auth').then(m => m.AuthLoginComponent),
  },
  {
    path: 'auth/callback',
    providers: [
      provideEffects([SessionEffects]), // 👈
      provideState(sessionFeature), // 👈
      provideState(navAfterLoadFeature), // 👈
    ],
    loadComponent: () => import('@tchl/shared/auth').then(m => m.AuthCallbackComponent),
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
