import { ChangeDetectionStrategy, Component, ViewEncapsulation, input } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'tch-admin-dialog-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [MatDialogModule],
  template: `
    <div class="tch-admin-dialog-shell">
      <h2 mat-dialog-title class="tch-admin-dialog-shell__title">
        @if (icon()) {
          <span class="tch-admin-dialog-shell__icon material-symbols-outlined" aria-hidden="true">{{ icon() }}</span>
        }
        <span>{{ title() }}</span>
      </h2>
      @if (description()) {
        <p class="tch-admin-dialog-shell__description">{{ description() }}</p>
      }
      <mat-dialog-content class="tch-admin-dialog-shell__content">
        <ng-content />
      </mat-dialog-content>
      <mat-dialog-actions align="end" class="tch-admin-dialog-shell__actions">
        <ng-content select="[actions]" />
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .tch-admin-dialog-shell {
        --comp-admin-dialog-gap: 0.875rem;
        --comp-admin-dialog-max-field: 32rem;
        display: block;
      }

      .tch-admin-dialog-shell__title {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        margin: 0;
        color: var(--tch-color-on-surface);
      }

      .tch-admin-dialog-shell__icon {
        color: var(--tch-color-primary);
        font-size: 1.35rem;
        font-variation-settings:
          'FILL' 0,
          'wght' 500,
          'GRAD' 0,
          'opsz' 24;
      }

      .tch-admin-dialog-shell__description {
        margin: -0.25rem 1.5rem 0.5rem;
        color: var(--tch-color-on-surface-variant);
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        line-height: 1.45;
      }

      .tch-admin-dialog-shell__content {
        min-width: min(100vw - 3rem, var(--comp-admin-dialog-max-field));
      }

      .tch-admin-dialog-form {
        display: grid;
        gap: var(--comp-admin-dialog-gap);
        padding-top: 0.5rem;
      }

      .tch-admin-dialog-readonly {
        display: grid;
        gap: 0.25rem;
        padding: 0.5rem 0;
        color: var(--tch-color-on-surface-variant);
        font-size: var(--tch-font-size-body-sm, 0.875rem);
      }

      .tch-admin-dialog-spinner {
        display: inline-block;
        animation: tch-admin-dialog-spin 0.8s linear infinite;
        font-size: 1rem;
        margin-inline-end: 0.25rem;
      }

      .tch-admin-dialog-shell__actions {
        border-top: 1px solid var(--tch-color-outline-variant);
        padding: 0.75rem 1.5rem 1rem;
      }

      @keyframes tch-admin-dialog-spin {
        to {
          transform: rotate(360deg);
        }
      }

      @media (max-width: 560px) {
        .tch-admin-dialog-shell__content {
          min-width: 0;
        }

        .tch-admin-dialog-shell__actions {
          align-items: stretch;
          flex-direction: column-reverse;
        }

        .tch-admin-dialog-shell__actions button {
          width: 100%;
        }
      }
    `,
  ],
})
export class AdminDialogShellComponent {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly icon = input<string | null>(null);
}
