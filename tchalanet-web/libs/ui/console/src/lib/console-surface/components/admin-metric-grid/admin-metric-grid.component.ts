import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Row of {@link AdminMetricCardComponent}s.
 *
 * Mobile-first: two columns on phones — matching the dashboard, which is the reference for KPI
 * density — then as many as fit from `medium` up. Pairing the grid with the card keeps the
 * spacing consistent across surfaces instead of each page picking its own `minmax()`.
 */
@Component({
  selector: 'tch-admin-metric-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<ng-content />',
  styles: [
    `
      @use 'breakpoints' as ui;

      :host {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
        align-items: stretch;
      }

      @include ui.up(medium) {
        :host {
          grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
        }
      }
    `,
  ],
})
export class AdminMetricGridComponent {}
