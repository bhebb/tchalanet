import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { VerificationStatus } from '@tch/page-model';
import { TchActionButton, TchCard, TchLoading } from '@tch/ui/components';

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

const STAMP_LINES: Record<VerificationStatus, string[]> = {
  PAYABLE:              ['PAYÉ', 'VALIDÉ'],
  NOT_PAYABLE:          ['NON', 'PAYABLE'],
  PENDING_RESULT:       ['EN', 'ATTENTE'],
  INVALID_OR_CANCELLED: ['ANNULÉ'],
  NOT_FOUND:            ['NON', 'TROUVÉ'],
  SERVICE_UNAVAILABLE:  ['HORS', 'LIGNE'],
};

@Component({
  selector: 'tch-public-check-ticket-page',
  imports: [TranslatePipe, TchCard, TchActionButton, TchLoading],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="check-page">

      <!-- ─── Form / Loading: hero + panel + help ─── -->
      @if (state().kind !== 'result') {

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

        <tch-card class="check-page__panel">
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
                      <span class="material-symbols-outlined" aria-hidden="true">qr_code_scanner</span>
                    </button>
                  </div>
                  <p class="check-page__hint">
                    <span class="material-symbols-outlined" aria-hidden="true">info</span>
                    <span>{{ 'public.check.code_hint' | translate }}</span>
                  </p>
                </div>
                <button
                  tch-action
                  class="check-page__submit"
                  type="submit"
                  style="--comp-action-bg: var(--tch-color-accent, #fecb00); --comp-action-fg: #1a1a1a;"
                >
                  {{ 'public.ticket.cta' | translate }}
                </button>
              </form>
            }

            @case ('loading') {
              <tch-loading
                [label]="'public.check.loading_title' | translate"
                [ariaLabel]="'public.check.loading_title' | translate"
              />
            }

          }
        </tch-card>

        <tch-card class="check-page__help-card" aria-labelledby="check-help-title">
          <div class="check-page__help-icon" aria-hidden="true">
            <span class="material-symbols-outlined">receipt_long</span>
          </div>
          <div>
            <h2 id="check-help-title">{{ 'public.check.help_title' | translate }}</h2>
            <p>{{ 'public.check.help_body' | translate }}</p>
          </div>
        </tch-card>

      }

      <!-- ─── Result state: full-width redesign ─── -->
      @if (state().kind === 'result') {
        <section class="check-result" aria-live="polite">

          <div class="check-result__header">
            <div>
              <p class="check-result__eyebrow">{{ 'public.check.result_eyebrow' | translate }}</p>
              <h1 class="check-result__code">{{ code() }}</h1>
            </div>
            <button
              tch-action
              class="check-result__verify-another"
              type="button"
              style="--comp-action-bg: var(--tch-color-accent, #fecb00); --comp-action-fg: #1a1a1a;"
              (click)="reset()"
            >
              <span class="material-symbols-outlined" aria-hidden="true">refresh</span>
              {{ 'public.check.verify_another' | translate }}
            </button>
          </div>

          <div class="check-result__grid">

            <!-- Status card -->
            <tch-card
              class="check-result__status-card"
              [class.is-success]="resultCopy().tone === 'success'"
              [class.is-warning]="resultCopy().tone === 'warning'"
              [class.is-danger]="resultCopy().tone  === 'danger'"
              [class.is-neutral]="resultCopy().tone === 'neutral'"
            >
              <span class="check-result__status-corner" aria-hidden="true"></span>
              <div class="check-result__status-icon" aria-hidden="true">
                <span class="material-symbols-outlined">{{ resultCopy().icon }}</span>
              </div>
              <div class="check-result__status-body">
                <h2 class="check-result__status-title">
                  {{ resultCopy().titleKey | translate }}
                </h2>
                <p class="check-result__status-desc">
                  {{ resultCopy().bodyKey | translate }}
                </p>
              </div>
            </tch-card>

            <!-- Security / trust card -->
            <tch-card class="check-result__security">
              <div class="check-result__security-icon" aria-hidden="true">
                <span class="material-symbols-outlined">security</span>
              </div>
              <div>
                <h3 class="check-result__security-title">
                  {{ 'public.check.security_title' | translate }}
                </h3>
                <p class="check-result__security-body">
                  {{ 'public.check.security_body' | translate }}
                </p>
              </div>
            </tch-card>

            <!-- Thermal receipt -->
            <div class="check-result__receipt-wrap">
              <div class="check-result__receipt">

                <div class="check-result__receipt-header">
                  <span class="check-result__receipt-brand">TCHALANET</span>
                  <span class="check-result__receipt-sub">BUREAU CENTRAL PORT-AU-PRINCE</span>
                  <span class="check-result__receipt-sub">TEL: +509 0000-0000</span>
                </div>

                <dl class="check-result__receipt-meta">
                  <div><dt>DATE:</dt><dd>{{ receiptDate() }}</dd></div>
                  <div><dt>CODE:</dt><dd>{{ code() }}</dd></div>
                  <div><dt>TERMINAL:</dt><dd>POS-HT-0000</dd></div>
                </dl>

                <div class="check-result__receipt-rule"></div>

                <div class="check-result__stamp-wrap">
                  <div
                    class="check-result__stamp"
                    [class.is-success]="resultCopy().tone === 'success'"
                    [class.is-warning]="resultCopy().tone === 'warning'"
                    [class.is-danger]="resultCopy().tone  === 'danger'"
                    [class.is-neutral]="resultCopy().tone === 'neutral'"
                    aria-hidden="true"
                  >
                    @for (line of stampLines(); track $index) {
                      <span>{{ line }}</span>
                    }
                  </div>
                </div>

                <div class="check-result__receipt-rule"></div>

                <p class="check-result__receipt-footer">
                  MERCI DE VOTRE CONFIANCE.<br />WWW.TCHALANET.COM
                </p>

                <div class="check-result__barcode" aria-hidden="true"></div>
              </div>
            </div>

          </div>
        </section>
      }

    </div>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      /* ─── Page wrapper ─── */
      .check-page {
        display: grid;
        gap: 1.5rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        padding: 1.5rem 0 calc(5rem + var(--tch-page-gutter, 16px));
      }

      /* ─── Hero ─── */
      .check-page__hero { display: grid; gap: 1.5rem; }

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
          linear-gradient(145deg,
            var(--tch-color-primary, var(--mat-sys-primary)),
            var(--tch-color-primary-container, var(--mat-sys-primary-container))
          );
      }

      .check-page__ticket-image {
        position: absolute;
        inset: auto 0 -1.25rem auto;
        width: min(76%, 21rem);
        transform: rotate(-4deg);
        filter: drop-shadow(0 1rem 1.25rem
          color-mix(in oklab, var(--tch-color-on-surface, var(--mat-sys-on-surface)) 22%, transparent));
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
        p, h1 { margin: 0; }
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

      .check-page__intro p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      /* ─── Panel (TchCard override) ─── */
      tch-card.check-page__panel {
        padding: 1rem;
        border-radius: var(--tch-radius-xl, 24px);
      }

      /* ─── Form ─── */
      .check-page__form,
      .check-page__field { display: grid; gap: 0.875rem; }

      .check-page__label {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .check-page__input-wrap { position: relative; }

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

        &:focus {
          border-color: var(--tch-color-primary, var(--mat-sys-primary));
          outline: 3px solid color-mix(in oklab,
            var(--tch-color-primary, var(--mat-sys-primary)) 20%, transparent);
        }
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
        cursor: pointer;
      }

      .check-page__hint {
        display: flex;
        align-items: flex-start;
        gap: 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        margin: 0;

        .material-symbols-outlined {
          color: var(--tch-color-outline, var(--mat-sys-outline));
          font-size: 1rem;
        }
      }

      /* submit = TchActionButton, only need width override */
      button.check-page__submit[tch-action] {
        width: 100%;
        box-sizing: border-box;
        font-weight: 800;
      }

      /* ─── Help card (TchCard override) ─── */
      tch-card.check-page__help-card {
        display: flex;
        gap: 1rem;
        border-radius: var(--tch-radius-xl, 24px);
        background:
          radial-gradient(
            var(--tch-color-outline-variant, var(--mat-sys-outline-variant)) 0.5px,
            transparent 0.5px
          ),
          var(--tch-color-surface-tonal, var(--mat-sys-surface-container-low));
        background-size: 0.5rem 0.5rem;

        h2 { margin: 0; }
        p  { margin: 0; color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant)); }
      }

      .check-page__help-icon {
        display: inline-grid;
        flex: 0 0 auto;
        place-items: center;
        width: 3rem;
        height: 3rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: color-mix(in oklab,
          var(--tch-color-primary, var(--mat-sys-primary)) 12%, transparent);
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      /* ─── Result section ─── */
      .check-result {
        grid-column: 1 / -1;
        display: grid;
        gap: 2rem;
      }

      .check-result__header {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 1rem;
      }

      .check-result__eyebrow {
        margin: 0 0 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: var(--tch-color-outline, var(--mat-sys-outline));
      }

      .check-result__code {
        margin: 0;
        font-family: var(--tch-font-family-mono, monospace);
        font-size: clamp(1.75rem, 5vw, 2.5rem);
        font-weight: 800;
        letter-spacing: 0.06em;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      /* verify-another = TchActionButton, only need layout extras */
      button.check-result__verify-another[tch-action] {
        gap: 0.5rem;
        font-weight: 800;
        white-space: nowrap;
      }

      /* ─── Content grid ─── */
      .check-result__grid { display: grid; gap: 1.5rem; align-items: start; }

      /* ─── Status card (TchCard override) ─── */
      tch-card.check-result__status-card {
        position: relative;
        overflow: hidden;
        padding: clamp(1.5rem, 4vw, 2rem);
        border-radius: var(--tch-radius-xl, 20px);
        display: flex;
        gap: 1.25rem;
        align-items: flex-start;
      }

      tch-card.check-result__status-card.is-success {
        --comp-card-border: color-mix(in oklab,
          var(--tch-color-status-ready, #10b981) 40%, transparent);
        .check-result__status-corner { background: var(--tch-color-status-ready, #10b981); }
        .check-result__status-icon   { background: var(--tch-color-status-ready, #10b981); color: #fff; }
        .check-result__status-title  { color: var(--tch-color-status-ready, #10b981); }
      }

      tch-card.check-result__status-card.is-warning {
        --comp-card-border: color-mix(in oklab,
          var(--tch-color-status-warning, #f59e0b) 40%, transparent);
        .check-result__status-corner { background: var(--tch-color-status-warning, #f59e0b); }
        .check-result__status-icon   { background: var(--tch-color-status-warning, #f59e0b); color: #1a1a1a; }
        .check-result__status-title  { color: var(--tch-color-status-warning, #f59e0b); }
      }

      tch-card.check-result__status-card.is-danger {
        --comp-card-border: color-mix(in oklab,
          var(--tch-color-error, var(--mat-sys-error)) 40%, transparent);
        .check-result__status-corner { background: var(--tch-color-error, var(--mat-sys-error)); }
        .check-result__status-icon   { background: var(--tch-color-error, var(--mat-sys-error)); color: #fff; }
        .check-result__status-title  { color: var(--tch-color-error, var(--mat-sys-error)); }
      }

      tch-card.check-result__status-card.is-neutral {
        .check-result__status-corner {
          background: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        }
        .check-result__status-icon {
          background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
          color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        }
        .check-result__status-title {
          color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        }
      }

      .check-result__status-corner {
        position: absolute;
        top: -2rem; right: -2rem;
        width: 7rem; height: 7rem;
        border-radius: 50%;
        opacity: 0.1;
        pointer-events: none;
      }

      .check-result__status-icon {
        position: relative;
        z-index: 1;
        flex-shrink: 0;
        width: 3.5rem; height: 3.5rem;
        border-radius: 50%;
        display: grid;
        place-items: center;

        .material-symbols-outlined { font-size: 1.75rem; }
      }

      .check-result__status-body {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 0.5rem;
        align-content: start;
      }

      .check-result__status-title {
        margin: 0;
        font-size: clamp(1.25rem, 3vw, 1.75rem);
        line-height: 1.2;
        font-weight: 800;
      }

      .check-result__status-desc {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        line-height: 1.6;
      }

      /* ─── Security card (TchCard override) ─── */
      tch-card.check-result__security {
        --comp-card-bg: var(--tch-color-primary, var(--mat-sys-primary));
        --comp-card-border: transparent;
        display: flex;
        gap: 1rem;
        align-items: flex-start;
        padding: 1.5rem;
        border-radius: var(--tch-radius-xl, 20px);
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .check-result__security-icon {
        flex-shrink: 0;
        margin-top: 0.125rem;
        .material-symbols-outlined { font-size: 1.5rem; opacity: 0.8; }
      }

      .check-result__security-title {
        margin: 0 0 0.375rem;
        font-weight: 800;
        font-size: var(--tch-font-size-title-md, 1.125rem);
      }

      .check-result__security-body {
        margin: 0;
        opacity: 0.88;
        line-height: 1.6;
      }

      /* ─── Thermal receipt ─── */
      .check-result__receipt-wrap {
        display: flex;
        justify-content: center;
      }

      .check-result__receipt {
        width: 100%;
        max-width: 22rem;
        padding: 2rem 1.5rem 3rem;
        border-radius: var(--tch-radius-control, 8px) var(--tch-radius-control, 8px) 0 0;
        background-color: var(--tch-color-surface-container-lowest, #fff);
        background-image: radial-gradient(
          color-mix(in oklab, var(--tch-color-outline-variant, #c7c5d4) 60%, transparent) 0.5px,
          transparent 0.5px
        );
        background-size: 10px 10px;
        box-shadow:
          0 4px 6px -1px color-mix(in oklab,
            var(--tch-color-primary, var(--mat-sys-primary)) 10%, transparent),
          0 20px 48px -8px color-mix(in oklab,
            var(--tch-color-primary, var(--mat-sys-primary)) 20%, transparent);
        clip-path: polygon(
          0% 0%, 100% 0%, 100% 96%,
          98% 100%, 96% 96%, 94% 100%, 92% 96%, 90% 100%, 88% 96%, 86% 100%,
          84% 96%, 82% 100%, 80% 96%, 78% 100%, 76% 96%, 74% 100%, 72% 96%,
          70% 100%, 68% 96%, 66% 100%, 64% 96%, 62% 100%, 60% 96%, 58% 100%,
          56% 96%, 54% 100%, 52% 96%, 50% 100%, 48% 96%, 46% 100%, 44% 96%,
          42% 100%, 40% 96%, 38% 100%, 36% 96%, 34% 100%, 32% 96%, 30% 100%,
          28% 96%, 26% 100%, 24% 96%, 22% 100%, 20% 96%, 18% 100%, 16% 96%,
          14% 100%, 12% 96%, 10% 100%, 8% 96%, 6% 100%, 4% 96%, 2% 100%, 0% 96%
        );
      }

      .check-result__receipt-header {
        display: grid;
        gap: 0.125rem;
        text-align: center;
        padding-bottom: 1.25rem;
        border-bottom: 1px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        margin-bottom: 1.25rem;
      }

      .check-result__receipt-brand {
        display: block;
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.125rem;
        font-weight: 800;
        letter-spacing: 0.15em;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .check-result__receipt-sub {
        display: block;
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.6875rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        letter-spacing: 0.04em;
      }

      .check-result__receipt-meta {
        margin: 0;
        display: grid;
        gap: 0.3rem;

        > div {
          display: flex;
          justify-content: space-between;
          font-family: var(--tch-font-family-mono, monospace);
          font-size: 0.75rem;
          color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        }
        dt { opacity: 0.6; }
        dd { margin: 0; font-weight: 700; }
      }

      .check-result__receipt-rule {
        border: none;
        border-top: 1px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        margin: 1.25rem 0;
      }

      .check-result__stamp-wrap {
        display: flex;
        justify-content: center;
        padding: 1rem 0 0.5rem;
      }

      .check-result__stamp {
        width: 5.5rem; height: 5.5rem;
        border-radius: 50%;
        border: 3px double currentColor;
        display: grid;
        place-items: center;
        align-content: center;
        transform: rotate(-15deg);
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.625rem;
        font-weight: 800;
        letter-spacing: 0.08em;
        text-align: center;
        line-height: 1.4;

        span { display: block; }

        &.is-success { color: var(--tch-color-status-ready, #10b981); }
        &.is-warning { color: var(--tch-color-status-warning, #f59e0b); }
        &.is-danger  { color: var(--tch-color-error, var(--mat-sys-error)); }
        &.is-neutral { color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant)); }
      }

      .check-result__receipt-footer {
        margin: 0;
        text-align: center;
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.625rem;
        color: var(--tch-color-outline, var(--mat-sys-outline));
        line-height: 1.5;
        letter-spacing: 0.04em;
      }

      .check-result__barcode {
        margin-top: 1.25rem;
        height: 2rem;
        opacity: 0.28;
        background: repeating-linear-gradient(
          to right,
          var(--tch-color-on-surface, #1a1c1e) 0px,
          var(--tch-color-on-surface, #1a1c1e) 2px,
          transparent 2px, transparent 5px,
          var(--tch-color-on-surface, #1a1c1e) 5px,
          var(--tch-color-on-surface, #1a1c1e) 8px,
          transparent 8px, transparent 12px,
          var(--tch-color-on-surface, #1a1c1e) 12px,
          var(--tch-color-on-surface, #1a1c1e) 15px,
          transparent 15px, transparent 19px,
          var(--tch-color-on-surface, #1a1c1e) 19px,
          var(--tch-color-on-surface, #1a1c1e) 22px,
          transparent 22px, transparent 25px,
          var(--tch-color-on-surface, #1a1c1e) 25px,
          var(--tch-color-on-surface, #1a1c1e) 26px,
          transparent 26px, transparent 30px
        );
      }

      /* ─── Responsive ─── */
      @include bp.up(medium) {
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

        tch-card.check-page__panel { padding: 1.5rem; }

        tch-card.check-page__help-card { grid-column: 2; }
      }

      @media (min-width: 960px) {
        .check-result__grid {
          grid-template-columns: minmax(0, 7fr) minmax(0, 5fr);
          grid-template-rows: auto auto;
        }

        tch-card.check-result__status-card { grid-row: 1; grid-column: 1; }
        tch-card.check-result__security    { grid-row: 2; grid-column: 1; }

        .check-result__receipt-wrap {
          grid-row: 1 / 3;
          grid-column: 2;
          position: sticky;
          top: 5rem;
          align-self: start;
        }
      }
    `,
  ],
})
export class PublicCheckTicketPage {
  private readonly route = inject(ActivatedRoute);

  readonly code = signal('');
  readonly state = signal<CheckState>({ kind: 'default' });

  constructor() {
    const rawCode = this.route.snapshot.queryParamMap.get('code');
    if (rawCode) this.code.set(formatPublicCode(rawCode));
  }

  readonly resultCopy = computed(() => {
    const s = this.state();
    return s.kind === 'result' ? verificationCopy(s.status) : verificationCopy('SERVICE_UNAVAILABLE');
  });

  readonly stampLines = computed(() => {
    const s = this.state();
    return s.kind === 'result' ? STAMP_LINES[s.status] : [];
  });

  readonly receiptDate = computed(() => {
    const d = new Date();
    const m = ['JAN','FÉV','MAR','AVR','MAI','JUN','JUL','AOU','SEP','OCT','NOV','DÉC'];
    return `${String(d.getDate()).padStart(2, '0')} ${m[d.getMonth()]} ${d.getFullYear()}`;
  });

  updateCode(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatPublicCode(input.value);
    this.code.set(formatted);
    input.value = formatted;
  }

  submit(event: Event): void {
    event.preventDefault();
    const compact = this.code().replace(/-/g, '');
    if (compact.length < 6 || !CODE_PATTERN.test(this.code())) {
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
  if (compact.length <= 4) return compact;
  if (compact.length <= 8) return `${compact.slice(0, 4)}-${compact.slice(4)}`;
  return `${compact.slice(0, 4)}-${compact.slice(4, 7)}-${compact.slice(7)}`;
}

export function verificationCopy(status: VerificationStatus): VerificationCopy {
  const map: Record<VerificationStatus, VerificationCopy> = {
    PENDING_RESULT: {
      icon: 'schedule', tone: 'warning',
      titleKey: 'public.check.status.PENDING_RESULT.title',
      bodyKey:  'public.check.status.PENDING_RESULT.body',
    },
    NOT_PAYABLE: {
      icon: 'remove_circle', tone: 'neutral',
      titleKey: 'public.check.status.NOT_PAYABLE.title',
      bodyKey:  'public.check.status.NOT_PAYABLE.body',
    },
    PAYABLE: {
      icon: 'task_alt', tone: 'success',
      titleKey: 'public.check.status.PAYABLE.title',
      bodyKey:  'public.check.status.PAYABLE.body',
    },
    INVALID_OR_CANCELLED: {
      icon: 'block', tone: 'danger',
      titleKey: 'public.check.status.INVALID_OR_CANCELLED.title',
      bodyKey:  'public.check.status.INVALID_OR_CANCELLED.body',
    },
    NOT_FOUND: {
      icon: 'search_off', tone: 'danger',
      titleKey: 'public.check.status.NOT_FOUND.title',
      bodyKey:  'public.check.status.NOT_FOUND.body',
    },
    SERVICE_UNAVAILABLE: {
      icon: 'cloud_off', tone: 'neutral',
      titleKey: 'public.check.status.SERVICE_UNAVAILABLE.title',
      bodyKey:  'public.check.status.SERVICE_UNAVAILABLE.body',
    },
  };
  return map[status];
}
