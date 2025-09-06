import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./tabs/tabs.routes').then((m) => m.routes),
  },
  {
    path: 'tickets',
    loadComponent: () =>
      import('./features/tickets/tickets.page').then(m => m.TicketsPage),
  }
];
