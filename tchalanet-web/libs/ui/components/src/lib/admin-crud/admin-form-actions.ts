import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'tch-admin-form-actions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatProgressSpinnerModule],
  template: `
    <div class="form-actions">
      <ng-content select="[start]" />
      <div class="form-actions__main">
        @if (cancelLabel()) {
          <button type="button" mat-button (click)="cancelClick.emit()">
            {{ cancelLabel() }}
          </button>
        }
        <button
          type="submit"
          mat-flat-button
          class="form-actions__submit"
          [disabled]="submitDisabled() || submitPending()"
        >
          @if (submitPending()) {
            <mat-spinner diameter="18" />
          }
          {{ submitLabel() }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .form-actions {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding: 1rem 1.25rem;
        border-top: 1px solid var(--tch-color-outline-variant);
        background: var(--tch-color-surface-container-lowest);
        border-radius: 0 0 var(--tch-radius-lg, 12px) var(--tch-radius-lg, 12px);
      }

      .form-actions__main {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-left: auto;
      }

      /* Primary submit = navy, per the brand role mapping. */
      .form-actions__submit {
        --mdc-filled-button-container-color: var(--tch-color-primary);
        --mdc-filled-button-label-text-color: var(--tch-color-on-primary);
      }

      mat-spinner {
        display: inline-block;
        margin-right: 0.375rem;
        vertical-align: middle;
      }
    `,
  ],
})
export class AdminFormActions {
  readonly submitLabel = input.required<string>();
  readonly cancelLabel = input('');
  readonly submitDisabled = input(false);
  readonly submitPending = input(false);

  readonly cancelClick = output<void>();
}
