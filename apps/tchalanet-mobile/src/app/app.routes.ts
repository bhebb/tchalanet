
import { Routes } from '@angular/router';

export const appRoutes: Routes = [
  {
    path: '',
    redirectTo: 'tabs/tab1',
    pathMatch: 'full'
  },
  {
    path: 'tabs',
    loadComponent: () => import('./../features/tabs/tabs').then(m => m.Tabs),
    children: [
      {
        path: 'tab1',
        loadComponent: () => import('./../features/tab1/tab1.page').then(m => m.Tab1Page)
      },
      {
        path: 'tab2',
        loadComponent: () => import('./../features/tab2/tab2.page').then(m => m.Tab2Page)
      },
      {
        path: 'tab3',
        loadComponent: () => import('./../features/tab3/tab3.page').then(m => m.Tab3Page)
      },
      {
        path: '',
        redirectTo: 'tabs/tab1',
        pathMatch: 'full'
      }
    ]
  }
];
