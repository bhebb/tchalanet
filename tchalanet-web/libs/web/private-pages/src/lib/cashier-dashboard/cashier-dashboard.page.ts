import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <div class="p-4">
    <p class="opacity-70">Contenu du dashboard caissier à implémenter…</p>
    <h1 class="text-xl font-semibold mb-2">Dashboard caissier</h1>
  </div>`,
  imports: [CommonModule],
  selector: 'tchl-cashier-dashboard-page',
  standalone: true,
})
export class CashierDashboardPage {}
