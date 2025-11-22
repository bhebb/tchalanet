import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  standalone: true,
  selector: 'tchl-super-admin-dashboard-page',
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h1 class="text-xl font-semibold mb-2">Dashboard super-admin</h1>
      <p class="opacity-70">Contenu du dashboard super-admin à implémenter…</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuperAdminDashboardPage {}
