import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tch-admin-dashboard-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="dashboard-grid"><ng-content /></div>`,
  styles: [
    `
      .dashboard-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2rem;
      }
    `,
  ],
})
export class AdminDashboardGridComponent {}
