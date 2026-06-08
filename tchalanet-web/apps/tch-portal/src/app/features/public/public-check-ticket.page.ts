import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { VerificationStatus } from '@tch/page-model';

type CheckState =
  | { readonly kind: 'default' }
  | { readonly kind: 'loading' }
  | { readonly kind: 'result'; readonly status: VerificationStatus };

interface VerificationCopy {
  readonly icon: string;
  readonly tone: 'warning' | 'neutral' | 'success' | 'danger';
  readonly titleKey: string;
  readonly bodyKey: string;
}

const CODE_PATTERN = /^[A-Z0-9]{3,4}-?[A-Z0-9]{3,4}-?[A-Z0-9]{0,3}$/;

@Component({
  selector: 'tch-public-check-ticket-page',
  imports: [RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="check-page">
        <section class="check-page__hero" aria-labelledby="check-title">
          <div class="check-page__visual" aria-hidden="true">
            <img
              class="check-page__ticket-image"
              src="/assets/public/ticket-verification-preview.svg"
              alt=""
            />
            <div class="check-page__visual-badge">
              <span class="material-symbols-outlined">verified_user</span>
              <span>{{ 'public.check.visual_badge' | translate }}</span>
            </div>
          </div>

          <div class="check-page__intro">
            <p class="check-page__eyebrow">{{ 'public.ticket.eyebrow' | translate }}</p>
            <h1 id="check-title">{{ 'public.ticket.title' | translate }}</h1>
            <p>{{ 'public.ticket.description' | translate }}</p>
          </div>
        </section>

        <section class="check-page__panel" aria-labelledby="check-form-title">
          @switch (state().kind) {
            @case ('default') {
              <form class="check-page__form" (submit)="submit($event)">
                <div class="check-page__field">
                  <label class="check-page__label" for="public-ticket-code">
                    {{ 'public.ticket.code_label' | translate }}
                  </label>
                  <div class="check-page__input-wrap">
                    <input
                      id="public-ticket-code"
                      class="check-page__code-input"
                      name="code"
                      autocomplete="off"
                      inputmode="text"
                      maxlength="11"
                      [value]="code()"
                      [placeholder]="'public.ticket.placeholder' | translate"
                      (input)="updateCode($event)"
                    />
                    <button
                      class="check-page__scan-button"
                      type="button"
                      [attr.aria-label]="'public.ticket.scan_qr' | translate"
                    >
                      <span class="material-symbols-outlined">qr_code_scanner</span>
                    </button>
                  </div>
                  <p class="check-page__hint">
                    <span class="material-symbols-outlined">info</span>
                    <span>{{ 'public.check.code_hint' | translate }}</span>
                  </p>
                </div>

                <button class="check-page__submit" type="submit">
                  <span>{{ 'public.ticket.cta' | translate }}</span>
                  <span class="material-symbols-outlined">search</span>
                </button>
              </form>
            }
            @case ('loading') {
              <div class="check-page__loading" role="status" aria-live="polite">
                <span class="check-page__spinner" aria-hidden="true"></span>
                <h2 id="check-form-title">{{ 'public.check.loading_title' | translate }}</h2>
                <p>{{ 'public.check.loading_body' | translate }}</p>
              </div>
            }
            @case ('result') {
              <article
                class="check-page__result"
                [class.is-success]="resultCopy().tone === 'success'"
                [class.is-warning]="resultCopy().tone === 'warning'"
                [class.is-danger]="resultCopy().tone === 'danger'"
                [class.is-neutral]="resultCopy().tone === 'neutral'"
                aria-live="polite"
              >
                <div class="check-page__result-icon">
                  <span class="material-symbols-outlined">{{ resultCopy().icon }}</span>
                </div>
                <div class="check-page__result-copy">
                  <p class="check-page__result-label">{{ 'public.check.status_label' | translate }}</p>
                  <h2>{{ resultCopy().titleKey | translate }}</h2>
                  <p>{{ resultCopy().bodyKey | translate }}</p>
                </div>
                <div class="check-page__result-actions">
                  <button class="check-page__secondary-action" type="button" (click)="reset()">
                    {{ 'public.check.verify_another' | translate }}
                  </button>
                  <a class="check-page__text-action" routerLink="/public/help">
                    {{ 'public.ticket.help' | translate }}
                  </a>
                </div>
              </article>
            }
          }
        </section>

        <section class="check-page__help-card" aria-labelledby="check-help-title">
          <div class="check-page__help-icon">
            <span class="material-symbols-outlined">receipt_long</span>
          </div>
          <div>
            <h2 id="check-help-title">{{ 'public.check.help_title' | translate }}</h2>
            <p>{{ 'public.check.help_body' | translate }}</p>
          </div>
        </section>
    </div>
  `,
  styles: [
    `
      .check-page {
        display: grid;
        gap: 1.5rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        padding: 1.5rem 0 calc(5rem + var(--tch-page-gutter, 16px));
      }

      .check-page .material-symbols-outlined {
        display: inline-block;
        overflow: hidden;
        max-width: 1em;
        line-height: 1;
        vertical-align: middle;
        white-space: nowrap;
      }

      .check-page__hero {
        display: grid;
        gap: 1.5rem;
      }

      .check-page__visual {
        position: relative;
        overflow: hidden;
        min-height: 13.5rem;
        border-radius: var(--tch-radius-lg, 12px);
        background:
          radial-gradient(
            circle at 80% 10%,
            color-mix(in oklab, var(--tch-color-accent, var(--mat-sys-tertiary)) 38%, transparent),
            transparent 34%
          ),
          linear-gradient(
            145deg,
            var(--tch-color-primary, var(--mat-sys-primary)),
            var(--tch-color-primary-container, var(--mat-sys-primary-container))
          );
        box-shadow: var(--tch-elevation-sm, 0 1px 2px color-mix(in oklab, var(--tch-color-on-surface, var(--mat-sys-on-surface)) 14%, transparent));
      }

      .check-page__ticket-image {
        position: absolute;
        inset: auto 0 -1.25rem auto;
        width: min(76%, 21rem);
        transform: rotate(-4deg);
        filter: drop-shadow(0 1rem 1.25rem color-mix(in oklab, var(--tch-color-on-surface, var(--mat-sys-on-surface)) 22%, transparent));
      }

      .check-page__visual-badge {
        position: absolute;
        left: 0.75rem;
        bottom: 0.75rem;
        display: inline-flex;
        align-items: center;
        gap: 0.375rem;
        min-height: 2.25rem;
        padding: 0 0.75rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: color-mix(in oklab, var(--tch-color-surface, var(--mat-sys-surface)) 92%, transparent);
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .check-page__visual-badge .material-symbols-outlined {
        color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
        font-size: 1.125rem;
      }

      .check-page__intro {
        display: grid;
        gap: 0.5rem;
      }

      .check-page__eyebrow,
      .check-page__intro h1,
      .check-page__intro p,
      .check-page__hint,
      .check-page__loading h2,
      .check-page__loading p,
      .check-page__result h2,
      .check-page__result p,
      .check-page__help-card h2,
      .check-page__help-card p {
        margin: 0;
      }

      .check-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .check-page__intro h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .check-page__intro p,
      .check-page__hint,
      .check-page__loading p,
      .check-page__result-copy p,
      .check-page__help-card p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .check-page__panel,
      .check-page__help-card {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .check-page__panel {
        padding: 1rem;
      }

      .check-page__form,
      .check-page__field {
        display: grid;
        gap: 0.875rem;
      }

      .check-page__label {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .check-page__input-wrap {
        position: relative;
      }

      .check-page__code-input {
        width: 100%;
        min-height: 4rem;
        box-sizing: border-box;
        border: 2px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.25rem;
        letter-spacing: 0.16em;
        padding: 0 3.75rem 0 1rem;
        text-transform: uppercase;
      }

      .check-page__code-input:focus {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        outline: 3px solid color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 20%, transparent);
      }

      .check-page__scan-button {
        position: absolute;
        right: 0.625rem;
        top: 50%;
        display: inline-grid;
        place-items: center;
        width: 2.75rem;
        height: 2.75rem;
        border: 0;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        transform: translateY(-50%);
      }

      .check-page__hint {
        display: flex;
        align-items: flex-start;
        gap: 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }

      .check-page__hint .material-symbols-outlined {
        color: var(--tch-color-outline, var(--mat-sys-outline));
        font-size: 1rem;
      }

      .check-page__submit,
      .check-page__secondary-action,
      .check-page__text-action {
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-lg, 12px);
        font-weight: 800;
      }

      .check-page__submit {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.625rem;
        width: 100%;
        border: 0;
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-size: 1rem;
      }

      .check-page__loading {
        display: grid;
        justify-items: center;
        gap: 0.75rem;
        padding: 2rem 1rem;
        text-align: center;
      }

      .check-page__spinner {
        width: 4rem;
        height: 4rem;
        border: 0.25rem solid color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 18%, transparent);
        border-top-color: var(--tch-color-primary, var(--mat-sys-primary));
        border-radius: var(--tch-radius-pill, 9999px);
        animation: check-page-spin 0.9s linear infinite;
      }

      .check-page__result {
        display: grid;
        gap: 1rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        border-left: 0.25rem solid var(--tch-color-status-missing, var(--mat-sys-outline));
      }

      .check-page__result.is-success {
        border-left-color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .check-page__result.is-warning {
        border-left-color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .check-page__result.is-danger {
        border-left-color: var(--tch-color-status-blocked, var(--mat-sys-error));
      }

      .check-page__result-icon {
        display: inline-grid;
        place-items: center;
        width: 3rem;
        height: 3rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .check-page__result-label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .check-page__result-copy {
        display: grid;
        gap: 0.375rem;
      }

      .check-page__result-copy h2,
      .check-page__help-card h2 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        line-height: var(--tch-line-height-title-md, 1.5rem);
      }

      .check-page__result-actions {
        display: grid;
        gap: 0.75rem;
      }

      .check-page__secondary-action {
        border: 0;
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .check-page__text-action {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        text-decoration: none;
      }

      .check-page__help-card {
        display: flex;
        gap: 1rem;
        padding: 1rem;
        background:
          radial-gradient(var(--tch-color-outline-variant, var(--mat-sys-outline-variant)) 0.5px, transparent 0.5px),
          var(--tch-color-surface-tonal, var(--mat-sys-surface-container-low));
        background-size: 0.5rem 0.5rem;
      }

      .check-page__help-icon {
        display: inline-grid;
        flex: 0 0 auto;
        place-items: center;
        width: 3rem;
        height: 3rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 12%, transparent);
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      @media (min-width: 760px) {
        .check-page {
          grid-template-columns: minmax(0, 1fr) minmax(22rem, 28rem);
          align-items: start;
          gap: 2rem;
          padding: 3rem 0;
        }

        .check-page__hero {
          grid-column: 1 / -1;
          grid-template-columns: minmax(18rem, 0.9fr) minmax(0, 1.1fr);
          align-items: center;
        }

        .check-page__intro h1 {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }

        .check-page__panel {
          padding: 1.5rem;
        }

        .check-page__help-card {
          grid-column: 2;
        }
      }

      @keyframes check-page-spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class PublicCheckTicketPage {
  readonly code = signal('');
  readonly state = signal<CheckState>({ kind: 'default' });

  readonly resultCopy = computed(() => {
    const current = this.state();
    return current.kind === 'result'
      ? verificationCopy(current.status)
      : verificationCopy('SERVICE_UNAVAILABLE');
  });

  updateCode(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatPublicCode(input.value);
    this.code.set(formatted);
    input.value = formatted;
  }

  submit(event: Event): void {
    event.preventDefault();
    const compactCode = this.code().replace(/-/g, '');
    if (compactCode.length < 6 || !CODE_PATTERN.test(this.code())) {
      this.state.set({ kind: 'result', status: 'NOT_FOUND' });
      return;
    }

    this.state.set({ kind: 'loading' });
    window.setTimeout(() => {
      this.state.set({ kind: 'result', status: 'SERVICE_UNAVAILABLE' });
    }, 450);
  }

  reset(): void {
    this.code.set('');
    this.state.set({ kind: 'default' });
  }
}

export function formatPublicCode(value: string): string {
  const compact = value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 10);
  if (compact.length <= 4) {
    return compact;
  }
  if (compact.length <= 8) {
    return `${compact.slice(0, 4)}-${compact.slice(4)}`;
  }
  return `${compact.slice(0, 4)}-${compact.slice(4, 7)}-${compact.slice(7)}`;
}

export function verificationCopy(status: VerificationStatus): VerificationCopy {
  const copies: Record<VerificationStatus, VerificationCopy> = {
    PENDING_RESULT: {
      icon: 'schedule',
      tone: 'warning',
      titleKey: 'public.check.status.PENDING_RESULT.title',
      bodyKey: 'public.check.status.PENDING_RESULT.body',
    },
    NOT_PAYABLE: {
      icon: 'remove_circle',
      tone: 'neutral',
      titleKey: 'public.check.status.NOT_PAYABLE.title',
      bodyKey: 'public.check.status.NOT_PAYABLE.body',
    },
    PAYABLE: {
      icon: 'task_alt',
      tone: 'success',
      titleKey: 'public.check.status.PAYABLE.title',
      bodyKey: 'public.check.status.PAYABLE.body',
    },
    INVALID_OR_CANCELLED: {
      icon: 'block',
      tone: 'danger',
      titleKey: 'public.check.status.INVALID_OR_CANCELLED.title',
      bodyKey: 'public.check.status.INVALID_OR_CANCELLED.body',
    },
    NOT_FOUND: {
      icon: 'search_off',
      tone: 'danger',
      titleKey: 'public.check.status.NOT_FOUND.title',
      bodyKey: 'public.check.status.NOT_FOUND.body',
    },
    SERVICE_UNAVAILABLE: {
      icon: 'cloud_off',
      tone: 'neutral',
      titleKey: 'public.check.status.SERVICE_UNAVAILABLE.title',
      bodyKey: 'public.check.status.SERVICE_UNAVAILABLE.body',
    },
  };

  return copies[status];
}
