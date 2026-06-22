import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TchActionButton } from '../action-button/action-button';

export type SubmitButtonVariant = 'primary' | 'secondary';

/**
 * Specialised submit button with a loading state. Composes {@link TchActionButton}
 * so colours/variants share a single button language — no parallel theming here.
 *
 *   <tch-submit-button [loading]="saving()" [disabled]="form.invalid" label="Provisionner" />
 */
@Component({
  selector: 'tch-submit-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchActionButton],
  template: `
    <button
      tch-action
      [variant]="variant()"
      type="submit"
      class="tch-submit-button"
      [class.tch-submit-button--loading]="loading()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading()"
    >
      @if (loading()) {
        <span class="tch-submit-button__spinner" aria-hidden="true"></span>
      }
      <span class="tch-submit-button__label">{{ label() }}</span>
      <ng-content />
    </button>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
      }

      .tch-submit-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        width: 100%;
      }

      .tch-submit-button__spinner {
        width: 1.125rem;
        height: 1.125rem;
        flex: none;
        border: 2px solid color-mix(in oklab, currentColor 30%, transparent);
        border-top-color: currentColor;
        border-radius: 50%;
        animation: tch-submit-spin 0.7s linear infinite;
      }

      @keyframes tch-submit-spin {
        to {
          transform: rotate(360deg);
        }
      }

      @media (prefers-reduced-motion: reduce) {
        .tch-submit-button__spinner {
          animation-duration: 1.5s;
        }
      }
    `,
  ],
})
export class TchSubmitButton {
  readonly label = input('');
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly variant = input<SubmitButtonVariant>('primary');
}
