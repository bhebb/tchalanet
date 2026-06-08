import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ResultStatus } from '@tch/page-model';

interface PublicResultDetail {
  readonly id: string;
  readonly gameName: string;
  readonly drawDateKey: string;
  readonly drawTime: string;
  readonly status: ResultStatus;
  readonly numbers: readonly string[];
  readonly sourceLabelKey: string;
  readonly updatedAtKey: string;
  readonly related: readonly RelatedResult[];
}

interface RelatedResult {
  readonly id: string;
  readonly label: string;
  readonly status: ResultStatus;
}

interface ResultStatusView {
  readonly icon: string;
  readonly titleKey: string;
  readonly bodyKey: string;
}

@Component({
  selector: 'tch-public-result-detail-page',
  imports: [RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="result-detail">
        <nav class="result-detail__breadcrumb" aria-label="Breadcrumb">
          <a routerLink="/public/results">{{ 'public.results.latest_title' | translate }}</a>
          <span aria-hidden="true">/</span>
          <span>{{ result().gameName }}</span>
        </nav>

        <section class="result-detail__hero" aria-labelledby="result-detail-title">
          <div class="result-detail__copy">
            <p class="result-detail__eyebrow">{{ 'public.results.detail_eyebrow' | translate }}</p>
            <h1 id="result-detail-title">{{ result().gameName }}</h1>
            <p>{{ 'public.results.subtitle' | translate }}</p>
          </div>
          <a class="result-detail__primary-action" routerLink="/public/check-ticket">
            <span class="material-symbols-outlined">qr_code_scanner</span>
            <span>{{ 'public.results.verify_ticket' | translate }}</span>
          </a>
        </section>

        <section
          class="result-detail__status"
          [attr.data-status]="result().status"
          aria-labelledby="result-status-title"
        >
          <div class="result-detail__status-icon">
            <span class="material-symbols-outlined">{{ statusView().icon }}</span>
          </div>
          <div>
            <p class="result-detail__status-label">{{ 'public.results.status_label' | translate }}</p>
            <h2 id="result-status-title">{{ statusView().titleKey | translate }}</h2>
            <p>{{ statusView().bodyKey | translate }}</p>
          </div>
        </section>

        <div class="result-detail__layout">
          <article class="result-detail__card" aria-labelledby="result-numbers-title">
            <div class="result-detail__card-header">
              <div>
                <p class="result-detail__meta-label">{{ 'public.results.draw_datetime' | translate }}</p>
                <h2 id="result-numbers-title">{{ result().drawDateKey | translate }} · {{ result().drawTime }}</h2>
              </div>
              <span class="result-detail__chip" [attr.data-status]="result().status">
                {{ statusLabel(result().status) | translate }}
              </span>
            </div>

            <div class="result-detail__numbers" aria-label="Result numbers">
              @for (number of result().numbers; track $index) {
                <span class="result-detail__number">{{ number }}</span>
              }
            </div>

            <dl class="result-detail__meta">
              <div class="result-detail__meta-row">
                <dt>{{ 'public.results.supported_source' | translate }}</dt>
                <dd>{{ result().sourceLabelKey | translate }}</dd>
              </div>
              <div class="result-detail__meta-row">
                <dt>{{ 'public.results.last_update' | translate }}</dt>
                <dd>{{ result().updatedAtKey | translate }}</dd>
              </div>
              <div class="result-detail__meta-row">
                <dt>{{ 'public.results.public_reference' | translate }}</dt>
                <dd>{{ result().id }}</dd>
              </div>
            </dl>
          </article>

          <aside class="result-detail__receipt" aria-labelledby="result-receipt-title">
            <div class="result-detail__receipt-edge" aria-hidden="true"></div>
            <p class="result-detail__receipt-brand">TCHALANET</p>
            <h2 id="result-receipt-title">{{ 'public.results.receipt_title' | translate }}</h2>
            <div class="result-detail__receipt-code">{{ result().id }}</div>
            <div class="result-detail__receipt-lines">
              <div>
                <span>{{ 'public.results.game' | translate }}</span>
                <strong>{{ result().gameName }}</strong>
              </div>
              <div>
                <span>{{ 'public.results.status_label' | translate }}</span>
                <strong>{{ statusLabel(result().status) | translate }}</strong>
              </div>
              <div>
                <span>{{ 'public.results.last_update' | translate }}</span>
                <strong>{{ result().updatedAtKey | translate }}</strong>
              </div>
            </div>
            <p class="result-detail__receipt-note">{{ 'public.results.receipt_note' | translate }}</p>
          </aside>
        </div>

        @if (result().related.length) {
          <section class="result-detail__related" aria-labelledby="related-results-title">
            <div class="result-detail__section-heading">
              <h2 id="related-results-title">{{ 'public.results.related_title' | translate }}</h2>
              <a routerLink="/public/results">{{ 'public.results.all' | translate }}</a>
            </div>
            <div class="result-detail__related-grid">
              @for (item of result().related; track item.id) {
                <a class="result-detail__related-card" [routerLink]="['/public/results', item.id]">
                  <span>{{ item.label }}</span>
                  <strong>{{ statusLabel(item.status) | translate }}</strong>
                </a>
              }
            </div>
          </section>
        }
    </div>
  `,
  styles: [
    `
      .result-detail {
        display: grid;
        gap: 1rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1160px);
        margin: 0 auto;
        padding: 1.25rem 0 calc(5rem + var(--tch-page-gutter, 16px));
      }

      .result-detail .material-symbols-outlined {
        display: inline-block;
        overflow: hidden;
        max-width: 1em;
        line-height: 1;
        vertical-align: middle;
        white-space: nowrap;
      }

      .result-detail__breadcrumb {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .result-detail__breadcrumb a,
      .result-detail__section-heading a,
      .result-detail__related-card {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        text-decoration: none;
      }

      .result-detail__hero {
        display: grid;
        gap: 1rem;
      }

      .result-detail__copy {
        display: grid;
        gap: 0.5rem;
      }

      .result-detail__eyebrow,
      .result-detail__copy h1,
      .result-detail__copy p,
      .result-detail__status h2,
      .result-detail__status p,
      .result-detail__card h2,
      .result-detail__card p,
      .result-detail__meta,
      .result-detail__receipt h2,
      .result-detail__receipt p,
      .result-detail__section-heading h2 {
        margin: 0;
      }

      .result-detail__eyebrow,
      .result-detail__meta-label,
      .result-detail__status-label {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .result-detail__copy h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .result-detail__copy p,
      .result-detail__status p,
      .result-detail__meta-row dt,
      .result-detail__receipt-note {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .result-detail__primary-action {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-weight: 800;
        text-decoration: none;
      }

      .result-detail__status,
      .result-detail__card,
      .result-detail__receipt,
      .result-detail__related-card {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .result-detail__status {
        display: flex;
        gap: 1rem;
        padding: 1rem;
        border-left: 0.25rem solid var(--tch-color-status-missing, var(--mat-sys-outline));
        border-radius: var(--tch-radius-lg, 12px);
      }

      .result-detail__status[data-status='CONFIRMED'] {
        border-left-color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .result-detail__status[data-status='PENDING'] {
        border-left-color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .result-detail__status-icon {
        display: inline-grid;
        flex: 0 0 auto;
        place-items: center;
        width: 3rem;
        height: 3rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .result-detail__layout {
        display: grid;
        gap: 1rem;
      }

      .result-detail__card {
        display: grid;
        gap: 1.25rem;
        padding: 1rem;
        border-radius: var(--tch-radius-xl, 24px);
      }

      .result-detail__card-header {
        display: grid;
        gap: 0.75rem;
      }

      .result-detail__card h2,
      .result-detail__status h2,
      .result-detail__section-heading h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        line-height: var(--tch-line-height-title-md, 1.5rem);
      }

      .result-detail__chip {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.375rem 0.625rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-status-missing, var(--mat-sys-outline));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .result-detail__chip[data-status='CONFIRMED'] {
        color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .result-detail__chip[data-status='PENDING'] {
        color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .result-detail__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.625rem;
      }

      .result-detail__number {
        display: grid;
        place-items: center;
        width: 3.25rem;
        height: 3.25rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.25rem;
        font-weight: 800;
      }

      .result-detail__meta {
        display: grid;
        gap: 0.75rem;
      }

      .result-detail__meta-row {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        padding-top: 0.75rem;
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .result-detail__meta-row dt,
      .result-detail__meta-row dd {
        margin: 0;
      }

      .result-detail__meta-row dd {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-weight: 800;
        text-align: right;
      }

      .result-detail__receipt {
        position: relative;
        display: grid;
        gap: 1rem;
        overflow: hidden;
        padding: 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        background:
          radial-gradient(var(--tch-color-outline-variant, var(--mat-sys-outline-variant)) 0.5px, transparent 0.5px),
          var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        background-size: 0.5rem 0.5rem;
        font-family: var(--tch-font-family-mono, monospace);
      }

      .result-detail__receipt-edge {
        position: absolute;
        inset: 0 0 auto;
        height: 0.25rem;
        background: linear-gradient(
          90deg,
          var(--tch-color-primary, var(--mat-sys-primary)),
          var(--tch-color-accent, var(--mat-sys-tertiary)),
          var(--tch-color-primary, var(--mat-sys-primary))
        );
      }

      .result-detail__receipt-brand {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: 1.125rem;
        font-weight: 900;
        letter-spacing: 0.08em;
        text-align: center;
      }

      .result-detail__receipt h2,
      .result-detail__receipt-note {
        text-align: center;
      }

      .result-detail__receipt-code {
        border-block: 1px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-weight: 800;
        letter-spacing: 0.12em;
        padding: 0.75rem 0;
        text-align: center;
      }

      .result-detail__receipt-lines {
        display: grid;
        gap: 0.625rem;
      }

      .result-detail__receipt-lines div {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
      }

      .result-detail__receipt-lines span {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .result-detail__receipt-lines strong {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        text-align: right;
      }

      .result-detail__related {
        display: grid;
        gap: 1rem;
      }

      .result-detail__section-heading {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
      }

      .result-detail__related-grid {
        display: grid;
        gap: 0.75rem;
      }

      .result-detail__related-card {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
      }

      @media (min-width: 760px) {
        .result-detail {
          gap: 1.5rem;
          padding: 3rem 0;
        }

        .result-detail__hero {
          grid-template-columns: minmax(0, 1fr) auto;
          align-items: end;
        }

        .result-detail__copy h1 {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }

        .result-detail__primary-action {
          padding: 0 1.25rem;
        }

        .result-detail__layout {
          grid-template-columns: minmax(0, 1fr) minmax(19rem, 24rem);
          align-items: start;
        }

        .result-detail__card,
        .result-detail__receipt {
          padding: 1.5rem;
        }

        .result-detail__card-header {
          grid-template-columns: minmax(0, 1fr) auto;
          align-items: start;
        }

        .result-detail__related-grid {
          grid-template-columns: repeat(3, minmax(0, 1fr));
        }
      }
    `,
  ],
})
export class PublicResultDetailPage {
  private readonly route = inject(ActivatedRoute);

  readonly result = computed(() => publicResultFallback(this.route.snapshot.paramMap.get('id')));
  readonly statusView = computed(() => resultStatusView(this.result().status));

  statusLabel(status: ResultStatus): string {
    return `public.results.status.${status}`;
  }
}

export function publicResultFallback(id: string | null): PublicResultDetail {
  const publicId = id?.trim() || 'ny-afternoon';
  return {
    id: publicId,
    gameName: 'New York Afternoon',
    drawDateKey: 'public.results.fallback.draw_date',
    drawTime: '14:30',
    status: 'CONFIRMED',
    numbers: ['84', '12', '99', '24'],
    sourceLabelKey: 'public.results.fallback.source',
    updatedAtKey: 'public.results.fallback.updated_at',
    related: [
      { id: 'florida-evening', label: 'Florida Evening', status: 'PENDING' },
      { id: 'georgia-midday', label: 'Georgia Midday', status: 'CONFIRMED' },
      { id: 'ny-evening', label: 'New York Evening', status: 'UNAVAILABLE' },
    ],
  };
}

export function resultStatusView(status: ResultStatus): ResultStatusView {
  const views: Record<ResultStatus, ResultStatusView> = {
    CONFIRMED: {
      icon: 'verified',
      titleKey: 'public.results.detail_status.CONFIRMED.title',
      bodyKey: 'public.results.detail_status.CONFIRMED.body',
    },
    PENDING: {
      icon: 'schedule',
      titleKey: 'public.results.detail_status.PENDING.title',
      bodyKey: 'public.results.detail_status.PENDING.body',
    },
    UNAVAILABLE: {
      icon: 'cloud_off',
      titleKey: 'public.results.detail_status.UNAVAILABLE.title',
      bodyKey: 'public.results.detail_status.UNAVAILABLE.body',
    },
  };

  return views[status];
}
