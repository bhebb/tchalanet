import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  host: {
    class: 'tch-card',
    '[class.tch-card--elevated]': 'elevated()',
  },
  styles: [
    `
      :host {
        display: block;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, #fff);
        border: 1px solid var(--tch-color-outline-variant, #c7c5d4);
        padding: 1rem;
      }
      :host.tch-card--elevated {
        border: none;
        box-shadow:
          0 1px 3px color-mix(in oklab, var(--tch-color-on-surface, #1a1c1e) 10%, transparent),
          0 4px 12px color-mix(in oklab, var(--tch-color-on-surface, #1a1c1e) 6%, transparent);
      }
    `,
  ],
})
export class TchCard {
  readonly elevated = input(false);
}
