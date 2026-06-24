import { Route } from '@angular/router';

export const authRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./login/login.page').then(m => m.LoginPage),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./forgot-password/forgot-password.page').then(m => m.ForgotPasswordPage),
  },
];
