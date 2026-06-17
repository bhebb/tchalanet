import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Responsive wrapper: visible on medium+ (≥600px), hidden on compact.
 * Place a mat-table inside — this component only handles visibility + theming.
 */
@Component({
  selector: 'tch-admin-data-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  host: { class: 'tch-admin-data-table' },
  styles: [
    `
      :host {
        display: none;
      }

      @media (min-width: 600px) {
        :host {
          display: block;
          overflow-x: auto;
          border-radius: var(--tch-radius-lg, 12px);
          border: 1px solid var(--tch-color-outline-variant);
          background: var(--tch-color-surface-container-lowest);
        }
      }

      :host ::ng-deep table.mat-mdc-table {
        width: 100%;
        background: transparent;
      }

      :host ::ng-deep .mat-mdc-row:last-child .mat-mdc-cell {
        border-bottom: none;
      }

      :host ::ng-deep .mat-mdc-header-row {
        background: var(--tch-color-surface-container-low);
      }
    `,
  ],
})
export class AdminDataTable {}
