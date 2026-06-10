import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tch-public-action-card',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="pub-action-card"
      [class.pub-action-card--selected]="selected()"
      [disabled]="disabled() || null"
      [attr.aria-pressed]="selected()"
      (click)="cardClick.emit()"
    >
      @if (icon()) {
        <span class="material-symbols-outlined pub-action-card__icon" aria-hidden="true">{{ icon() }}</span>
      }
      @if (title()) {
        <span class="pub-action-card__title">{{ title() | translate }}</span>
      }
      @if (description()) {
        <span class="pub-action-card__description">{{ description() | translate }}</span>
      }
    </button>
  `,
  styles: [
    `
      .pub-action-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem 0.75rem;
        border-radius: var(--tch-radius-xl, 16px);
        border: 1px solid transparent;
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-align: center;
        transition: background 0.15s, border-color 0.15s;
        width: 100%;
      }

      .pub-action-card:hover:not(:disabled) {
        background: var(--tch-color-surface-container-high, var(--mat-sys-surface-container-high));
      }

      .pub-action-card--selected {
        background: var(--tch-color-primary-container, var(--mat-sys-primary-container));
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pub-action-card:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .pub-action-card__icon {
        font-size: 1.5rem;
      }

      .pub-action-card__title {
        line-height: 1.3;
      }

      .pub-action-card__description {
        font-size: var(--tch-font-size-label-xs, 0.6875rem);
        font-weight: 400;
        opacity: 0.8;
      }
    `,
  ],
})
export class TchPubActionCard {
  readonly icon = input('');
  readonly title = input('');
  readonly description = input('');
  readonly selected = input(false);
  readonly disabled = input(false);
  readonly cardClick = output<void>();
}
