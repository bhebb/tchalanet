import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  input,
  signal,
} from '@angular/core';

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
    <tch-card class="section-card" data-testid="admin-detail-section">
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
      </div>
      <div class="section-card__status">
        <ng-content select="[status]" />
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
      <div class="section-card__actions">
        <ng-content select="[actions]" />
      </div>
    </tch-card>
  `,
  styles: [
    `
      @use 'breakpoints' as ui;

      :host {
        display: block;
      }

      tch-card {
        --tch-radius-lg: var(--tch-radius-2xl, 1.5rem);
        --comp-card-bg: var(--tch-color-surface);
        display: grid;
        padding: 0;
        overflow: hidden;
        box-shadow:
          0 1px 4px color-mix(in oklab, var(--tch-color-on-surface) 8%, transparent),
          0 0 0 1px color-mix(in oklab, var(--tch-color-on-surface) 4%, transparent);
        border: none;
      }

      .section-card__header {
        display: grid;
        grid-template-columns: auto minmax(0, 1fr);
        align-items: start;
        gap: 0.625rem;
        padding: 1rem;
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

      .section-card__status,
      .section-card__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .section-card__status {
        padding: 1rem 1.5rem 0;
      }

      .section-card__actions {
        padding: 0 1.5rem 1.5rem;
      }

      .section-card__status:empty,
      .section-card__actions:empty {
        display: none;
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

      @include ui.up(expanded) {
        tch-card {
          grid-template-columns: minmax(0, 1fr) auto auto;
          align-items: center;
        }

        .section-card__header {
          display: flex;
          align-items: center;
          grid-column: 1;
          padding: 1rem 1.5rem;
        }

        .section-card__status,
        .section-card__actions {
          border-bottom: 1px solid var(--tch-color-outline-variant, #c8c5d0);
          padding: 1rem 1.5rem 1rem 0;
          flex-shrink: 0;
        }

        .section-card__error,
        .section-card__body {
          grid-column: 1 / -1;
        }
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
