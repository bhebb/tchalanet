import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { TchPubCardTone } from './pub-card';

@Component({
  selector: 'tch-public-stat-card',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pub-stat-card" [attr.data-tone]="tone()">
      @if (icon()) {
        <span class="material-symbols-outlined pub-stat-card__icon" aria-hidden="true">{{ icon() }}</span>
      }
      <span class="pub-stat-card__value">{{ value() }}</span>
      @if (label()) {
        <span class="pub-stat-card__label">{{ label() | translate }}</span>
      }
      @if (hint()) {
        <span class="pub-stat-card__hint">{{ hint() | translate }}</span>
      }
    </div>
  `,
  styles: [
    `
      .pub-stat-card {
        display: grid;
        gap: 0.25rem;
        padding: 1.25rem 1rem;
        border-radius: var(--tch-radius-xl, 16px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      .pub-stat-card[data-tone='primary'] {
        background: var(--tch-color-primary-container, var(--mat-sys-primary-container));
        border-color: transparent;
      }

      .pub-stat-card__icon {
        color: var(--tch-color-outline, var(--mat-sys-outline));
        font-size: 1.25rem;
        margin-bottom: 0.25rem;
      }

      .pub-stat-card__value {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-display-sm, 1.75rem);
        font-weight: 800;
        line-height: 1;
      }

      .pub-stat-card__label {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 600;
      }

      .pub-stat-card__hint {
        color: var(--tch-color-outline, var(--mat-sys-outline));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }
    `,
  ],
})
export class TchPubStatCard {
  readonly value = input('');
  readonly label = input('');
  readonly hint = input('');
  readonly icon = input('');
  readonly tone = input<TchPubCardTone>('default');
}
