import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, input, signal } from '@angular/core';

import { TchCard, TchSectionError, TchSectionErrorSeverity } from '@tch/ui/components';

export interface AdminSectionCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-admin-section-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchCard, TchSectionError],
  template: `
    <tch-card class="section-card">
      <div class="section-card__header">
        @if (icon()) {
          <span class="section-card__icon material-symbols-outlined" aria-hidden="true">
            {{ icon() }}
          </span>
        }
        <div class="section-card__title-group">
          <h2 class="section-card__title">{{ title() }}</h2>
          @if (description()) {
            <p class="section-card__description">{{ description() }}</p>
          }
        </div>
        <div class="section-card__actions">
          <ng-content select="[actions]" />
        </div>
      </div>
      @if (sectionError(); as error) {
        <div class="section-card__error">
          <tch-section-error
            [title]="error.title"
            [message]="error.message"
            [severity]="error.severity ?? 'error'"
          />
        </div>
      }
      <div class="section-card__body">
        <ng-content />
      </div>
    </tch-card>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      tch-card {
        --tch-radius-lg: var(--tch-radius-2xl, 1.5rem);
        --comp-card-bg: var(--tch-color-surface);
        padding: 0;
        overflow: hidden;
        box-shadow: 0 1px 4px color-mix(in oklab, var(--tch-color-on-surface) 8%, transparent),
                    0 0 0 1px color-mix(in oklab, var(--tch-color-on-surface) 4%, transparent);
        border: none;
      }

      .section-card__header {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        padding: 1rem 1.5rem;
        border-bottom: 1px solid var(--tch-color-outline-variant, #c8c5d0);
      }

      .section-card__icon {
        font-size: 1.25rem;
        color: var(--tch-color-secondary, #745b00);
        flex-shrink: 0;
        font-variation-settings:
          'FILL' 0,
          'wght' 400,
          'GRAD' 0,
          'opsz' 24;
      }

      .section-card__title-group {
        flex: 1;
        min-width: 0;
      }

      .section-card__title {
        margin: 0;
        font-size: 1.0625rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .section-card__description {
        margin: 0.125rem 0 0;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .section-card__actions {
        display: flex;
        gap: 0.5rem;
        flex-shrink: 0;
      }

      .section-card__error {
        padding: 1rem 1.5rem 0;
      }

      .section-card__body {
        padding: 1.5rem;
      }

      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
      }
    `,
  ],
})
export class AdminSectionCardComponent {
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly icon = input<string | null>(null);
  readonly sectionError = signal<AdminSectionCardError | null>(null);

  setSectionError(error: AdminSectionCardError | null): void {
    this.sectionError.set(error);
    this.changeDetector.markForCheck();
  }
}
