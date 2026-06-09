import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EMPTY, Observable } from 'rxjs';

import { ResultStatus } from '@tch/page-model';
import { PublicDrawResultDetail, PublicDrawResultsService } from './public-draw-results.service';

interface PublicResultDetail {
  readonly drawResultId: string;
  readonly slotKey: string;
  /** Stable i18n key from the server (e.g. "draw_channel.ny.eve.label"). */
  readonly drawChannelLabelKey: string;
  /** Human-readable fallback label from the server. */
  readonly drawChannelLabel: string;
  readonly resultDate: string;
  readonly drawTime: string;
  readonly timezone: string;
  readonly status: ResultStatus;
  readonly numbers: readonly string[];
  readonly sourceLabel: string;
  readonly publishedAt: string | null;
  readonly related: readonly RelatedResult[];
}

interface RelatedResult {
  readonly drawResultId: string;
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

      @if (isLoading()) {
        <div class="result-detail__loading" aria-live="polite" aria-busy="true">
          <span class="result-detail__spinner" aria-hidden="true"></span>
          <span>{{ 'public.results.loading' | translate }}</span>
        </div>
      }

        <nav class="result-detail__breadcrumb" aria-label="Breadcrumb">
          <a routerLink="/public/results">{{ 'public.results.latest_title' | translate }}</a>
          <span aria-hidden="true">/</span>
          <span>{{ channelLabel() | translate }}</span>
        </nav>

        <section class="result-detail__hero" aria-labelledby="result-detail-title">
          <div class="result-detail__copy">
            <p class="result-detail__eyebrow">{{ 'public.results.detail_eyebrow' | translate }}</p>
            <h1 id="result-detail-title">{{ channelLabel() | translate }}</h1>
            <p class="result-detail__code">{{ result().slotKey }} · {{ result().resultDate }} · {{ hhmm(result().drawTime) }}</p>
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
                <h2 id="result-numbers-title">{{ result().resultDate }} · {{ hhmm(result().drawTime) }}</h2>
              </div>
              <span class="result-detail__chip" [attr.data-status]="result().status">
                {{ statusLabel(result().status) | translate }}
              </span>
            </div>

            @if (result().numbers.length) {
              <div class="result-detail__numbers" aria-label="Result numbers">
                @for (number of result().numbers; track $index) {
                  <span class="result-detail__number">{{ number }}</span>
                }
              </div>
            } @else {
              <p class="result-detail__no-numbers">{{ 'public.results.numbers_pending' | translate }}</p>
            }

            <dl class="result-detail__meta">
              <div class="result-detail__meta-row">
                <dt>{{ 'public.results.supported_source' | translate }}</dt>
                <dd>{{ result().sourceLabel }}</dd>
              </div>
              @if (result().publishedAt) {
                <div class="result-detail__meta-row">
                  <dt>{{ 'public.results.last_update' | translate }}</dt>
                  <dd>{{ fmtIso(result().publishedAt) }}</dd>
                </div>
              }
              <div class="result-detail__meta-row">
                <dt>{{ 'public.results.public_reference' | translate }}</dt>
                <dd class="result-detail__ref-id">
                  <span class="result-detail__ref-short" [title]="result().drawResultId">
                    {{ shortId(result().drawResultId) }}
                  </span>
                  <button
                    type="button"
                    class="result-detail__copy-btn"
                    [attr.aria-label]="'public.results.copy_ref' | translate"
                    (click)="copyRef(result().drawResultId)"
                  >
                    <span class="material-symbols-outlined" aria-hidden="true">
                      {{ copiedRef() ? 'check' : 'content_copy' }}
                    </span>
                  </button>
                </dd>
              </div>
            </dl>
          </article>

          <aside class="result-detail__receipt" aria-labelledby="result-receipt-title">
            <div class="result-detail__receipt-edge" aria-hidden="true"></div>
            <p class="result-detail__receipt-brand">TCHALANET</p>
            <h2 id="result-receipt-title">{{ 'public.results.receipt_title' | translate }}</h2>
            <div class="result-detail__receipt-code">{{ result().slotKey }}</div>
            <div class="result-detail__receipt-lines">
              <div>
                <span>{{ 'public.results.game' | translate }}</span>
                <strong>{{ channelLabel() | translate }}</strong>
              </div>
              <div>
                <span>{{ 'public.results.draw_datetime' | translate }}</span>
                <strong>{{ result().resultDate }} · {{ hhmm(result().drawTime) }}</strong>
              </div>
              <div>
                <span>{{ 'public.results.status_label' | translate }}</span>
                <strong>{{ statusLabel(result().status) | translate }}</strong>
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
              @for (item of result().related; track item.drawResultId) {
                <a class="result-detail__related-card" [routerLink]="['/public/results', item.drawResultId]">
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

      .result-detail__code,
      .result-detail__copy p,
      .result-detail__status p,
      .result-detail__meta-row dt,
      .result-detail__receipt-note {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .result-detail__code {
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
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

      .result-detail__status[data-status='CONFIRMED'],
      .result-detail__status[data-status='OVERRIDDEN'] {
        border-left-color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .result-detail__status[data-status='PROVISIONAL'] {
        border-left-color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .result-detail__status[data-status='ERROR'] {
        border-left-color: var(--tch-color-status-blocked, var(--mat-sys-error));
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

      .result-detail__chip[data-status='CONFIRMED'],
      .result-detail__chip[data-status='OVERRIDDEN'] {
        color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .result-detail__chip[data-status='PROVISIONAL'] {
        color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .result-detail__chip[data-status='ERROR'] {
        color: var(--tch-color-status-blocked, var(--mat-sys-error));
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

      .result-detail__no-numbers {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
        margin: 0;
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

      .result-detail__ref-id {
        display: flex;
        align-items: center;
        gap: 0.375rem;
      }

      .result-detail__ref-short {
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        cursor: default;
      }

      .result-detail__copy-btn {
        flex-shrink: 0;
        display: inline-grid;
        place-items: center;
        width: 1.75rem;
        height: 1.75rem;
        padding: 0;
        border: 0;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font-size: 0.875rem;
        transition: color 150ms, background 150ms;

        .material-symbols-outlined {
          font-size: 1rem;
          max-width: unset;
        }

        &:hover {
          background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
          color: var(--tch-color-primary, var(--mat-sys-primary));
        }
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

      .result-detail__loading {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }

      .result-detail__spinner {
        flex-shrink: 0;
        width: 1.25rem;
        height: 1.25rem;
        border: 2px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-top-color: var(--tch-color-primary, var(--mat-sys-primary));
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }

      @keyframes spin {
        to { transform: rotate(360deg); }
      }
    `,
  ],
})
export class PublicResultDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly svc = inject(PublicDrawResultsService);

  private readonly drawResultId =
    this.route.snapshot.paramMap.get('drawResultId') ?? null;

  readonly resource = rxResource({
    params: () => this.drawResultId,
    stream: ({ params: id }): Observable<PublicDrawResultDetail> =>
      id ? this.svc.detail(id) : EMPTY,
  });

  /** Resolved data: live from server if available, otherwise fallback mock */
  readonly result = computed(() => {
    const live = this.resource.value();
    if (live) {
      return {
        drawResultId: live.drawResultId,
        slotKey: live.slotKey,
        drawChannelLabelKey: live.drawChannelLabelKey,
        drawChannelLabel: live.drawChannelLabel,
        resultDate: live.resultDate,
        drawTime: live.drawTime,
        timezone: live.timezone,
        status: live.status as ResultStatus,
        numbers: live.numbers,
        sourceLabel: live.sourceLabel ?? '',
        publishedAt: live.publishedAt,
        related: [],
      } satisfies PublicResultDetail;
    }
    return publicResultFallback(this.drawResultId);
  });

  readonly isLoading = computed(() => this.resource.isLoading());
  readonly hasError = computed(() => !!this.resource.error() && !this.resource.value());

  /** Channel label: uses drawChannelLabelKey translated, fallback to drawChannelLabel. */
  readonly channelLabel = computed(() => {
    const r = this.result();
    return r.drawChannelLabelKey || r.drawChannelLabel;
  });

  readonly statusView = computed(() => resultStatusView(this.result().status));

  statusLabel(status: ResultStatus): string {
    return `public.results.status.${status}`;
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  hhmm(time: string | undefined): string {
    if (!time) return '';
    return time.length > 5 ? time.substring(0, 5) : time;
  }

  /**
   * Formats an ISO timestamp for public display.
   * "2026-06-09T02:55:02.192926Z" → "2026-06-09 · 02:55 UTC"
   */
  fmtIso(iso: string | null | undefined): string {
    if (!iso) return '';
    const ms = Date.parse(iso);
    if (isNaN(ms)) return iso;
    const d = new Date(ms);
    const date = d.toISOString().substring(0, 10);   // "2026-06-09"
    const time = d.toISOString().substring(11, 16);  // "02:55"
    return `${date} · ${time} UTC`;
  }

  /**
   * Truncates a UUID for mobile display.
   * "71f2ff4a-e52d-44f5-93f6-943db0e94ed9" → "71f2ff4a…"
   */
  shortId(id: string): string {
    return id.length > 12 ? `${id.substring(0, 8)}…` : id;
  }

  readonly copiedRef = signal(false);

  async copyRef(id: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(id);
      this.copiedRef.set(true);
      setTimeout(() => this.copiedRef.set(false), 2000);
    } catch {
      // Clipboard API unavailable (non-secure context) — silently ignore
    }
  }
}

export function publicResultFallback(drawResultId: string | null): PublicResultDetail {
  const id = drawResultId?.trim() || 'fallback';
  return {
    drawResultId: id,
    slotKey: 'NY_MID',
    drawChannelLabelKey: 'draw_channel.ny.mid.label',
    drawChannelLabel: 'New York — Midi',
    resultDate: '2026-06-08',
    drawTime: '12:30',
    timezone: 'America/New_York',
    status: 'CONFIRMED',
    numbers: ['84', '12', '99', '24'],
    sourceLabel: 'New York Lottery',
    publishedAt: null,
    related: [
      { drawResultId: 'fl-eve-fallback', label: 'Florida Evening', status: 'PROVISIONAL' },
      { drawResultId: 'ga-mid-fallback', label: 'Georgia Midday', status: 'CONFIRMED' },
      { drawResultId: 'ny-eve-fallback', label: 'New York Evening', status: 'PROVISIONAL' },
    ],
  };
}

export function resultStatusView(status: ResultStatus): ResultStatusView {
  const views: Record<ResultStatus, ResultStatusView> = {
    PROVISIONAL: {
      icon: 'schedule',
      titleKey: 'public.results.detail_status.PROVISIONAL.title',
      bodyKey: 'public.results.detail_status.PROVISIONAL.body',
    },
    CONFIRMED: {
      icon: 'verified',
      titleKey: 'public.results.detail_status.CONFIRMED.title',
      bodyKey: 'public.results.detail_status.CONFIRMED.body',
    },
    OVERRIDDEN: {
      icon: 'edit_note',
      titleKey: 'public.results.detail_status.OVERRIDDEN.title',
      bodyKey: 'public.results.detail_status.OVERRIDDEN.body',
    },
    ERROR: {
      icon: 'error_outline',
      titleKey: 'public.results.detail_status.ERROR.title',
      bodyKey: 'public.results.detail_status.ERROR.body',
    },
  };
  return views[status];
}
