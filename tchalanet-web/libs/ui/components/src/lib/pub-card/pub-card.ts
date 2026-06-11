import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TchPubCardTone = 'default' | 'primary' | 'featured';
export type TchPubCardDensity = 'comfortable' | 'compact';

@Component({
  selector: 'tch-public-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-content select="[cardHeader]" />
    <ng-content select="[cardBody]" />
    <ng-content select="[cardActions]" />
    <ng-content />
  `,
  host: {
    '[attr.data-tone]': 'tone()',
    '[attr.data-density]': 'density()',
  },
  styles: [
    `
      :host {
        --pub-card-padding: 1.5rem;
        --pub-card-bg: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        --pub-card-border-color: var(--tch-color-outline-variant, var(--mat-sys-outline-variant));

        display: grid;
        gap: 0.75rem;
        padding: var(--pub-card-padding);
        border: 1px solid var(--pub-card-border-color);
        border-radius: var(--tch-radius-xl, 16px);
        background: var(--pub-card-bg);
      }

      :host([data-tone='primary']) {
        --pub-card-bg: var(--tch-color-primary-container, var(--mat-sys-primary-container));
        --pub-card-border-color: transparent;
      }

      :host([data-tone='featured']) {
        --pub-card-bg: var(--tch-color-primary-container, var(--mat-sys-primary-container, #2e3192));
        --pub-card-border-color: transparent;
        position: relative;
        overflow: hidden;
      }

      :host([data-density='compact']) {
        --pub-card-padding: 1rem;
        gap: 0.5rem;
      }
    `,
  ],
})
export class TchPubCard {
  readonly tone = input<TchPubCardTone>('default');
  readonly density = input<TchPubCardDensity>('comfortable');
}
