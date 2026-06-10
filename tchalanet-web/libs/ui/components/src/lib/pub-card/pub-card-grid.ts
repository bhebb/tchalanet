import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TchPubCardDensity } from './pub-card';

@Component({
  selector: 'tch-public-card-grid',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  host: {
    '[attr.data-density]': 'density()',
  },
  styles: [
    `
      :host {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(var(--pub-card-grid-min, 240px), 1fr));
        gap: 1rem;
      }

      :host([data-density='compact']) {
        gap: 0.75rem;
      }

      @media (max-width: 479px) {
        :host {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class TchPubCardGrid {
  readonly density = input<TchPubCardDensity>('comfortable');
}
