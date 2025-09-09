// apps/tchalanet-web/src/app/app.routes.ts
import { Routes } from '@angular/router';

export const appRoutes: Routes = [
    {
      path: '',
      loadComponent: () =>
        import('web/ui-shell-public').then(m => m.PublicHomeShellComponent),
      children: [
        {
          path: '',
          loadComponent: () =>
            import('web/feature-home-public').then(m => m.HomePublicPage)
        }
      ]
    },
    {
      path: '**',
      redirectTo: '',
      pathMatch: 'full'
    }
  ]
;
