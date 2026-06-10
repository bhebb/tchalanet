import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tch-admin-crud-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="crud-shell">
      <div class="crud-shell__toolbar">
        <ng-content select="[toolbar]" />
      </div>
      <div class="crud-shell__content">
        <ng-content select="[content]" />
      </div>
      <div class="crud-shell__footer">
        <ng-content select="[footer]" />
      </div>
    </div>
  `,
  styles: [
    `
      .crud-shell {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .crud-shell__toolbar {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .crud-shell__content {
        flex: 1;
        min-width: 0;
      }

      .crud-shell__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding-top: 0.75rem;
        border-top: 1px solid var(--tch-color-outline-variant, #c8c5d0);
      }

      .crud-shell__footer:empty {
        display: none;
      }
    `,
  ],
})
export class AdminCrudShellComponent {}
