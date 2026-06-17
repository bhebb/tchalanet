import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'tch-admin-form-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `
    <div class="form-section">
      <div class="form-section__header">
        @if (icon()) {
          <mat-icon class="form-section__icon">{{ icon() }}</mat-icon>
        }
        <div class="form-section__title-group">
          <h2 class="form-section__title">{{ title() }}</h2>
          @if (description()) {
            <p class="form-section__description">{{ description() }}</p>
          }
        </div>
        <div class="form-section__actions">
          <ng-content select="[actions]" />
        </div>
      </div>
      <div class="form-section__body">
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      .form-section {
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest);
        border: 1px solid var(--tch-color-outline-variant);
        overflow: hidden;
      }

      .form-section__header {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem 1.25rem;
        border-bottom: 1px solid var(--tch-color-outline-variant);
        background: var(--tch-color-surface-container-low);
      }

      .form-section__icon {
        color: var(--tch-color-primary);
        flex-shrink: 0;
      }

      .form-section__title-group {
        flex: 1;
        min-width: 0;
      }

      .form-section__title {
        margin: 0;
        font-size: var(--tch-font-size-title-sm, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface);
      }

      .form-section__description {
        margin: 0.125rem 0 0;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface-variant);
      }

      .form-section__actions {
        display: flex;
        gap: 0.5rem;
        flex-shrink: 0;
      }

      .form-section__body {
        padding: 1.25rem;
      }
    `,
  ],
})
export class AdminFormSection {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly icon = input<string | null>(null);
}
