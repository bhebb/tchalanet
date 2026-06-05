import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { isRecord, ResultStatus, stringProp } from '../widget.contract';

interface DrawSlot {
  readonly slotKey?: string;
  readonly label?: string;
  readonly provider?: string;
  readonly next?: { readonly status?: string; readonly localDate?: string; readonly localTime?: string };
  readonly latest?: {
    readonly resultDate?: string;
    readonly occurredAt?: string;
    readonly status?: string;
    readonly quality?: string;
    readonly haiti?: Readonly<Record<string, string>>;
    readonly source?: { readonly pick3?: { readonly main?: readonly string[] }; readonly pick4?: { readonly main?: readonly string[] } };
  };
}

@Component({
  selector: 'tch-public-draw-results-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="results">
      <div class="results__header">
        <div>
          <h2>{{ titleKey() | tchLabel }}</h2>
          <p>{{ 'public.results.subtitle' | tchLabel }}</p>
        </div>
        <a class="results__more" href="/public/results">{{ 'public.results.all' | tchLabel }}</a>
      </div>

      @if (slots().length) {
        <div class="results__grid">
          @for (slot of slots(); track slot.slotKey ?? $index) {
            <article class="result-card">
              <div class="result-card__top">
                <div>
                  <h3>{{ slot.label || slot.slotKey || slot.provider }}</h3>
                  <p>{{ slot.latest?.resultDate || slot.next?.localDate }}</p>
                </div>
                <span class="result-card__status" [attr.data-status]="status(slot)">
                  {{ statusLabel(status(slot)) | tchLabel }}
                </span>
              </div>
              <div class="result-card__numbers">
                @for (number of numbers(slot); track $index) {
                  <span>{{ number }}</span>
                }
              </div>
              <a class="result-card__detail" href="/public/results">
                {{ 'public.results.detail' | tchLabel }}
              </a>
            </article>
          }
        </div>
      } @else {
        <p class="results__empty">{{ 'public.results.empty' | tchLabel }}</p>
      }
    </section>
  `,
  styles: [
    `
      .results {
        display: grid;
        gap: 1rem;
      }
      .results__header {
        display: flex;
        justify-content: space-between;
        align-items: end;
        gap: 1rem;
      }
      .results h2,
      .results h3,
      .results p {
        margin: 0;
      }
      .results h2 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .results__header p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }
      .results__more,
      .result-card__detail {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 800;
        text-decoration: none;
      }
      .results__grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 1rem;
      }
      .result-card {
        display: grid;
        gap: 1rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }
      .result-card__top {
        display: flex;
        justify-content: space-between;
        gap: 0.75rem;
      }
      .result-card__status {
        align-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.5rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }
      .result-card__status[data-status='CONFIRMED'] {
        color: var(--tch-color-status-ready, #10b981);
      }
      .result-card__status[data-status='PENDING'] {
        color: var(--tch-color-status-warning, #f59e0b);
      }
      .result-card__status[data-status='UNAVAILABLE'] {
        color: var(--tch-color-status-missing, #64748b);
      }
      .result-card__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .result-card__numbers span {
        width: 2.5rem;
        height: 2.5rem;
        display: grid;
        place-items: center;
        border-radius: 50%;
        background: var(--tch-color-surface-container-highest, var(--mat-sys-surface-container-highest));
        font-family: var(--tch-font-family-mono, monospace);
        font-weight: 800;
      }
      @media (max-width: 720px) {
        .results__header {
          display: grid;
        }
      }
    `,
  ],
})
export class PublicDrawResultsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'title_key') ?? 'public.results.latest_title',
  );
  readonly slots = computed<readonly DrawSlot[]>(() => {
    const data = this.dynamic();
    return isRecord(data) && Array.isArray(data['slots']) ? (data['slots'] as readonly DrawSlot[]) : [];
  });

  status(slot: DrawSlot): ResultStatus {
    const latestStatus = slot.latest?.status;
    const quality = slot.latest?.quality;
    if (latestStatus === 'CONFIRMED') {
      return 'CONFIRMED';
    }
    if (latestStatus === 'PROVISIONAL' || slot.next?.status === 'WAITING') {
      return 'PENDING';
    }
    if (quality === 'COMPLETE') {
      return 'PENDING';
    }
    return 'UNAVAILABLE';
  }

  statusLabel(status: ResultStatus): string {
    return `public.results.status.${status}`;
  }

  numbers(slot: DrawSlot): readonly string[] {
    const haiti = slot.latest?.haiti;
    const values = haiti ? Object.values(haiti).filter(Boolean) : [];
    if (values.length) {
      return values;
    }
    return [
      ...(slot.latest?.source?.pick3?.main ?? []),
      ...(slot.latest?.source?.pick4?.main ?? []),
    ];
  }
}
