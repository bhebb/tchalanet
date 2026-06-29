import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tch-public-filter-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="pub-filter-bar"><ng-content /></div>`,
  styles: [
    `
      .pub-filter-bar {
        display: grid;
        gap: 0.625rem;
        padding: 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }
    `,
  ],
})
export class PublicFilterBarComponent {}
