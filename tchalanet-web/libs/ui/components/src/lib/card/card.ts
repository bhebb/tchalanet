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
        --comp-card-bg: var(--tch-color-surface-container-lowest);
        --comp-card-border: var(--tch-color-outline-variant);
        display: block;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--comp-card-bg);
        border: 1px solid var(--comp-card-border);
        padding: 1rem;
      }
      :host.tch-card--elevated {
        border: none;
        box-shadow: var(--tch-elevation-1);
      }
    `,
  ],
})
export class TchCard {
  readonly elevated = input(false);
}
