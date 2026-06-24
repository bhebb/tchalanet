import {
  ChangeDetectionStrategy,
  Component,
  Input,
  Output,
  EventEmitter,
  computed,
  signal,
} from '@angular/core';

import { copyToClipboard } from './copy-error-details';
import { ShellFeedbackItem, ShellFeedbackVerbosity } from './shell-feedback.model';

@Component({
  selector: 'tch-shell-feedback-banner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="sfb sfb--{{ item.severity }}"
      role="alert"
      [attr.aria-live]="item.severity === 'error' ? 'assertive' : 'polite'"
    >
      <span class="material-symbols-outlined sfb__icon" aria-hidden="true">{{ icon }}</span>

      <div class="sfb__body">
        <strong class="sfb__title">{{ item.title }}</strong>
        <span class="sfb__message">{{ item.message }}</span>

        @if (verbosity === 'verbose' && item.status) {
          <span class="sfb__meta">HTTP {{ item.status }}</span>
        }
        @if (verbosity === 'verbose' && item.source) {
          <span class="sfb__meta">{{ item.source }}</span>
        }

        @if (showDetails() && hasDiagnostic()) {
          <button
            type="button"
            class="sfb__details-toggle"
            [attr.aria-expanded]="expanded()"
            (click)="toggleDetails()"
          >
            <span>Détails techniques</span>
            <span class="material-symbols-outlined" aria-hidden="true">
              {{ expanded() ? 'expand_less' : 'expand_more' }}
            </span>
          </button>

          @if (expanded()) {
            <dl class="sfb__details" aria-label="Détails de diagnostic">
              @if (item.requestId) {
                <div class="sfb__detail-row">
                  <dt>requestId</dt>
                  <dd><code>{{ item.requestId }}</code></dd>
                </div>
              }
              @if (item.traceId) {
                <div class="sfb__detail-row">
                  <dt>traceId</dt>
                  <dd><code>{{ item.traceId }}</code></dd>
                </div>
              }
              @if (item.spanId) {
                <div class="sfb__detail-row">
                  <dt>spanId</dt>
                  <dd><code>{{ item.spanId }}</code></dd>
                </div>
              }
            </dl>
          }
        }
      </div>

      <div class="sfb__actions">
        @if (showDetails() && item.copyText) {
          <button
            type="button"
            class="sfb__btn"
            [attr.aria-label]="copied() ? 'Copié' : 'Copier les détails'"
            (click)="copy()"
          >
            <span class="material-symbols-outlined" aria-hidden="true">
              {{ copied() ? 'check' : 'content_copy' }}
            </span>
          </button>
        }
        @if (item.reportUrl) {
          <a
            class="sfb__btn"
            [href]="item.reportUrl"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Signaler ce problème"
          >
            <span class="material-symbols-outlined" aria-hidden="true">bug_report</span>
          </a>
        }
        @if (item.dismissible) {
          <button
            type="button"
            class="sfb__btn"
            aria-label="Fermer"
            (click)="dismissed.emit(item.id)"
          >
            <span class="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .sfb {
      --sfb-bg: var(--tch-color-surface-container-high);
      --sfb-fg: var(--tch-color-on-surface);
      --sfb-border: var(--tch-color-outline-variant);
      --sfb-icon-color: var(--tch-color-on-surface-variant, var(--tch-color-on-surface));

      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-left: 3px solid var(--sfb-border);
      border-radius: var(--tch-radius-sm, 4px);
      background: var(--sfb-bg);
      color: var(--sfb-fg);
      font-size: 0.875rem;
    }

    .sfb--warn {
      --sfb-bg: color-mix(in srgb, var(--tch-color-status-warning, #f59e0b) 12%, var(--tch-color-surface-container-high));
      --sfb-border: var(--tch-color-status-warning, #f59e0b);
      --sfb-icon-color: color-mix(in srgb, var(--tch-color-status-warning, #f59e0b) 80%, var(--tch-color-on-surface));
    }

    .sfb--error {
      --sfb-bg: var(--tch-color-error-container);
      --sfb-fg: var(--tch-color-on-error-container);
      --sfb-border: var(--tch-color-error);
      --sfb-icon-color: var(--tch-color-error);
    }

    .sfb__icon {
      flex-shrink: 0;
      font-size: 1.25rem;
      line-height: 1.5rem;
      color: var(--sfb-icon-color);
    }

    .sfb__body {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 0.125rem;
    }

    .sfb__title {
      font-weight: 600;
    }

    .sfb__message {
      color: color-mix(in srgb, var(--sfb-fg) 80%, transparent);
    }

    .sfb__meta {
      font-size: 0.75rem;
      opacity: 0.65;
    }

    .sfb__details-toggle {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      margin-top: 0.375rem;
      padding: 0;
      border: none;
      background: transparent;
      color: color-mix(in srgb, var(--sfb-fg) 70%, transparent);
      font-size: 0.75rem;
      cursor: pointer;
      line-height: 1.25rem;
    }

    .sfb__details-toggle:hover {
      color: var(--sfb-fg);
    }

    .sfb__details-toggle .material-symbols-outlined {
      font-size: 1rem;
    }

    .sfb__details {
      margin: 0.375rem 0 0;
      padding: 0.5rem 0.625rem;
      border-radius: var(--tch-radius-sm, 4px);
      background: color-mix(in srgb, var(--sfb-fg) 6%, transparent);
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .sfb__detail-row {
      display: flex;
      gap: 0.5rem;
      align-items: baseline;
    }

    .sfb__detail-row dt {
      flex-shrink: 0;
      font-size: 0.6875rem;
      opacity: 0.65;
      min-width: 5rem;
    }

    .sfb__detail-row dd {
      margin: 0;
    }

    .sfb__detail-row code {
      font-size: 0.6875rem;
      font-family: monospace;
      word-break: break-all;
    }

    .sfb__actions {
      display: flex;
      gap: 0.25rem;
      flex-shrink: 0;
    }

    .sfb__btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border: none;
      border-radius: var(--tch-radius-sm, 4px);
      background: transparent;
      color: var(--sfb-fg);
      cursor: pointer;
      opacity: 0.7;
      transition: opacity 150ms;
    }

    .sfb__btn:hover {
      opacity: 1;
      background: color-mix(in srgb, var(--sfb-fg) 10%, transparent);
    }

    .sfb__btn .material-symbols-outlined {
      font-size: 1.125rem;
    }
  `],
})
export class ShellFeedbackBannerComponent {
  @Input({ required: true }) item!: ShellFeedbackItem;
  @Input({ required: true }) verbosity!: ShellFeedbackVerbosity;
  @Output() dismissed = new EventEmitter<string>();

  protected readonly copied = signal(false);
  protected readonly expanded = signal(false);

  protected get icon(): string {
    switch (this.item.severity) {
      case 'error': return 'error';
      case 'warn':  return 'warning';
      default:      return 'info';
    }
  }

  protected showDetails = computed(() => this.verbosity === 'standard' || this.verbosity === 'verbose');

  protected hasDiagnostic(): boolean {
    return !!(this.item.requestId || this.item.traceId || this.item.spanId);
  }

  protected toggleDetails(): void {
    this.expanded.update(v => !v);
  }

  protected copy(): void {
    if (!this.item.copyText) return;
    copyToClipboard(this.item.copyText);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2000);
  }
}
