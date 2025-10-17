import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('@tchl/web/private-pages').then(m => m.PrivateShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.DashboardPage),
      },
      {
        path: 'tickets',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.TicketsPage),
      },
      {
        path: 'tirages',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.DrawsPage),
      },
      {
        path: 'resultats',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.ResultsPage),
      },
      {
        path: 'rapports',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.ReportsPage),
      },
      {
        path: 'gestion',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.AdminPage),
      },
      {
        path: 'profile',
        loadComponent: () => import('@tchl/web/private-pages').then(m => m.ProfilePage),
      },
    ],
  },
];
