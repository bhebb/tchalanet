import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-loading',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="loading" role="status" [attr.aria-label]="ariaLabel() || label() || 'Chargement'">      <div class="loading__spinner" aria-hidden="true"></div>
      @if (label()) {
        <p class="loading__label">{{ label() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .loading {
        display: grid;
        justify-items: center;
        gap: 0.75rem;
        padding: 2rem 1rem;
      }
      .loading__spinner {
        width: 2rem;
        height: 2rem;
        border-radius: 50%;
        border: 3px solid var(--tch-color-surface-container-high, #e8e8eb);
        border-top-color: var(--tch-color-primary, #1a1b4b);
        animation: tch-spin 0.8s linear infinite;
      }
      @keyframes tch-spin {
        to { transform: rotate(360deg); }
      }
      .loading__label {
        margin: 0;
        color: var(--tch-color-on-surface-variant, #464652);
        font-size: var(--tch-font-size-small, 0.9375rem);
      }
    `,
  ],
})
export class TchLoading {
  readonly label = input('');
  readonly ariaLabel = input('');
}
