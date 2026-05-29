import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  standalone: true,
  selector: 'tchl-tenant-admin-dashboard-page',
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h1 class="text-xl font-semibold mb-2">Dashboard administrateur de tenant</h1>
      <p class="opacity-70">Contenu du dashboard tenant admin à implémenter…</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TenantAdminDashboardPage {}
