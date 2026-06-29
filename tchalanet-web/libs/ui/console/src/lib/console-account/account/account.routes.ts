import { Route } from '@angular/router';

export const consoleAccountRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/activation/account-activation.page').then(m => m.AccountActivationPage),
  },
];
