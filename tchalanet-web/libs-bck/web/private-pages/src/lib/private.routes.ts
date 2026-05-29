import { Routes } from '@angular/router';

import { AdminPage } from './admin-page/admin.page';
import { CashierDashboardPage } from './cashier-dashboard/cashier-dashboard.page';
import { DashboardContainerComponent } from './dashboard-container/dashboard-container.component';
import { DashboardPage } from './dashboard-page/dashboard.page';
import { DrawsPage } from './draws-page/draws.page';
import { PrivateShellComponent } from './private-shell/private-shell.component';
import { ProfilePage } from './profile-page/profile.page';
import { ReportsPage } from './reports-page/reports.page';
import { SuperAdminDashboardPage } from './super-admin-dashboard/super-admin-dashboard.page';
import { TenantAdminDashboardPage } from './tenant-admin-dashboard/tenant-admin-dashboard.page';
import { TicketsPage } from './tickets-page/tickets.page';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => Promise.resolve(PrivateShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        // Conteneur de toutes les variantes de dashboard (par rôle)
        path: 'dashboard',
        loadComponent: () => Promise.resolve(DashboardContainerComponent),
        children: [
          {
            // Accès direct à /dashboard : on redirige en fonction du rôle via canMatch
            path: '',
            pathMatch: 'full',
            canMatch: [
              () =>
                import('./dashboard-container/role-dashboard-routing').then(
                  m => m.dashboardRoleRedirectCanMatch,
                ),
            ],
            // Ce composant est un placeholder : normalement jamais affiché
            loadComponent: () => Promise.resolve(DashboardPage),
          },
          {
            path: 'super-admin',
            loadComponent: () => Promise.resolve(SuperAdminDashboardPage),
            data: { role: 'SUPER_ADMIN' },
          },
          {
            path: 'tenant-admin',
            loadComponent: () => Promise.resolve(TenantAdminDashboardPage),
            data: { role: 'TENANT_ADMIN' },
          },
          {
            path: 'cashier',
            loadComponent: () => Promise.resolve(CashierDashboardPage),
            data: { role: 'CASHIER' },
          },
        ],
      },
      {
        path: 'tickets',
        loadComponent: () => Promise.resolve(TicketsPage),
      },
      {
        path: 'tirages',
        loadComponent: () => Promise.resolve(DrawsPage),
      },
      {
        path: 'rapports',
        loadComponent: () => Promise.resolve(ReportsPage),
      },
      {
        path: 'gestion',
        loadComponent: () => Promise.resolve(AdminPage),
      },
      {
        path: 'profile',
        loadComponent: () => Promise.resolve(ProfilePage),
      },
    ],
  },
];
