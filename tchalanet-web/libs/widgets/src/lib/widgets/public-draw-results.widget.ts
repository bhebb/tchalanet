import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, isRecord, ResultStatus, stringProp } from '@tch/page-model';
import { BadgeStatus, TchActionButton, TchCard, TchSectionHeader, TchStatusBadge } from '@tch/ui/components';

interface DrawSlot {
  readonly slotKey?: string;
  readonly label?: string;
  readonly provider?: string;
  readonly next?: {
    readonly status?: string;
    readonly localDate?: string;
    readonly localTime?: string;
    readonly expectedAt?: string;
  };
  readonly latest?: {
    readonly resultDate?: string;
    readonly occurredAt?: string;
    readonly status?: string;
    readonly quality?: string;
    readonly haiti?: Readonly<Record<string, string>>;
    readonly source?: {
      readonly pick3?: { readonly main?: readonly string[] };
      readonly pick4?: { readonly main?: readonly string[] };
    };
  };
}

interface DrawSlotView extends DrawSlot {
  readonly countdown: string | null;
  readonly isAwaiting: boolean;
}

@Component({
  selector: 'tch-public-draw-results-widget',
  imports: [LabelPipe, TchSectionHeader, TchStatusBadge, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="latest-results-widget__header">
      <tch-section-header
        [title]="titleKey() | tchLabel"
        [subtitle]="'public.results.subtitle' | tchLabel"
      >
        <a tch-action variant="tertiary" slot="action" [attr.href]="moreHref()">
          {{ moreLabelKey() | tchLabel }}
        </a>
      </tch-section-header>
    </div>

    @if (slotsWithCountdown().length) {
      <ul class="latest-results-widget__list" role="list">
        @for (slot of slotsWithCountdown(); track slot.slotKey ?? $index) {
          <li>
            <tch-card class="latest-results-widget__item">
              <div class="latest-results-widget__item-top">
                <div class="latest-results-widget__item-meta">
                  <h3 class="latest-results-widget__item-name">
                    {{ slot.label || slot.slotKey || slot.provider }}
                  </h3>
                  <time class="latest-results-widget__item-date">
                    {{ slot.latest?.resultDate || slot.next?.localDate || '' }}
                  </time>
                </div>
                <tch-status-badge
                  [status]="badgeStatus(slotStatus(slot))"
                  [label]="slotStatusLabel(slot) | tchLabel"
                />
              </div>

              @if (numbers(slot).length) {
                <div class="latest-results-widget__numbers" aria-label="Numéros tirés">
                  @for (n of numbers(slot); track $index) {
                    <span class="latest-results-widget__number">{{ n }}</span>
                  }
                </div>
              } @else {
                <p class="latest-results-widget__no-numbers">
                  {{ 'public.results.numbers_pending' | tchLabel }}
                </p>
              }

              @if (slot.isAwaiting) {
                <p class="latest-results-widget__awaiting">
                  {{ 'public.results.awaiting' | tchLabel }}
                </p>
              } @else if (slot.countdown) {
                <div class="latest-results-widget__next-draw">
                  <span class="latest-results-widget__next-label">
                    {{ 'public.results.next_in' | tchLabel }}
                  </span>
                  <time class="latest-results-widget__countdown">{{ slot.countdown }}</time>
                </div>
              }

              <a tch-action variant="tertiary" class="latest-results-widget__detail-link" [attr.href]="detailHref(slot)">
                {{ 'public.results.detail' | tchLabel }}
              </a>
            </tch-card>
          </li>
        }
      </ul>
    } @else {
      <p class="latest-results-widget__empty">{{ 'public.results.empty' | tchLabel }}</p>
    }
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      :host {
        display: grid;
        gap: 1.5rem;
      }

      /* ── Header ── */

      .latest-results-widget__header {
        display: contents;
      }

      a[tch-action][data-variant='tertiary'] {
        white-space: nowrap;
      }

      /* ── List ── */

      .latest-results-widget__list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 1rem;
        grid-template-columns: 1fr;

        @include bp.up(medium) {
          grid-template-columns: repeat(2, 1fr);
        }

        @include bp.up(expanded) {
          grid-template-columns: repeat(3, 1fr);
        }
      }

      /* ── Card (overrides TchCard defaults) ── */

      tch-card.latest-results-widget__item {
        display: grid;
        gap: 1rem;
        padding: 1.25rem;
        height: 100%;
        box-sizing: border-box;

        @include bp.up(expanded) {
          padding: 1.5rem;
        }
      }

      /* ── Card internals ── */

      .latest-results-widget__item-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.75rem;
      }

      .latest-results-widget__item-meta {
        display: grid;
        gap: 0.25rem;
      }

      .latest-results-widget__item-name {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .latest-results-widget__item-date {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-variant-numeric: tabular-nums;
      }

      /* ── Numbers ── */

      .latest-results-widget__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .latest-results-widget__number {
        width: 2.75rem;
        height: 2.75rem;
        display: grid;
        place-items: center;
        border-radius: 50%;
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.875rem;
        font-weight: 800;
        flex-shrink: 0;
      }

      .latest-results-widget__no-numbers {
        margin: 0;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
      }

      /* ── Awaiting ── */

      .latest-results-widget__awaiting {
        margin: 0;
        display: flex;
        align-items: center;
        gap: 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-status-warning, var(--mat-sys-secondary));

        &::before {
          content: '';
          display: inline-block;
          width: 0.5rem;
          height: 0.5rem;
          border-radius: 50%;
          background: currentColor;
          animation: pulse-dot 1.4s ease-in-out infinite;
          flex-shrink: 0;
        }
      }

      /* ── Next draw countdown ── */

      .latest-results-widget__next-draw {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
      }

      .latest-results-widget__next-label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .latest-results-widget__countdown {
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        font-variant-numeric: tabular-nums;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        letter-spacing: 0.04em;
      }

      /* ── Detail link ── */

      .latest-results-widget__detail-link {
        justify-self: start;
        min-height: unset;
        padding: 0;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        background: transparent;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        border: none;
      }

      /* ── Empty ── */

      .latest-results-widget__empty {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
      }

      @keyframes pulse-dot {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.3; }
      }
    `,
  ],
})
export class PublicDrawResultsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  private readonly _now = signal(Date.now());

  constructor() {
    const id = setInterval(() => this._now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
  }

  readonly titleKey = computed(
    () => stringProp(this.config(), 'titleKey') ?? 'home.draws.title',
  );

  readonly moreHref = computed(() => {
    const dest = actionFrom(this.config()?.props?.['moreDestination']);
    return dest ? destinationHref(dest.destination) : '/public/results';
  });

  readonly moreLabelKey = computed(() => {
    const dest = actionFrom(this.config()?.props?.['moreDestination']);
    return dest?.labelKey ?? 'public.results.all';
  });

  private readonly slots = computed<readonly DrawSlot[]>(() => {
    const data = this.dynamic();
    return isRecord(data) && Array.isArray(data['slots'])
      ? (data['slots'] as readonly DrawSlot[])
      : [];
  });

  readonly slotsWithCountdown = computed<readonly DrawSlotView[]>(() => {
    const now = this._now();
    return this.slots().map(slot => ({
      ...slot,
      countdown: slotCountdown(slot, now),
      isAwaiting: slotIsAwaiting(slot, now),
    }));
  });

  slotStatus(slot: DrawSlot): ResultStatus {
    const s = slot.latest?.status;
    if (s === 'CONFIRMED') return 'CONFIRMED';
    if (s === 'PROVISIONAL' || slot.next?.status === 'WAITING') return 'PENDING';
    if (slot.latest?.quality === 'COMPLETE') return 'PENDING';
    return 'UNAVAILABLE';
  }

  slotStatusLabel(slot: DrawSlot): string {
    return `public.results.status.${this.slotStatus(slot)}`;
  }

  badgeStatus(s: ResultStatus): BadgeStatus {
    if (s === 'CONFIRMED') return 'ready';
    if (s === 'PENDING') return 'pending';
    return 'missing';
  }

  numbers(slot: DrawSlot): readonly string[] {
    const haiti = slot.latest?.haiti;
    if (haiti) {
      const vals = Object.values(haiti).filter(v => v && v.trim().length > 0);
      if (vals.length) return vals;
    }
    return [
      ...(slot.latest?.source?.pick3?.main ?? []),
      ...(slot.latest?.source?.pick4?.main ?? []),
    ];
  }

  detailHref(slot: DrawSlot): string {
    const key = (slot.slotKey ?? slot.provider ?? '').toLowerCase().replace(/_/g, '-');
    return key ? `/public/results/${encodeURIComponent(key)}` : '/public/results';
  }
}

function parseExpectedMs(slot: DrawSlot): number | null {
  if (slot.next?.expectedAt) {
    const ms = Date.parse(slot.next.expectedAt);
    return isNaN(ms) ? null : ms;
  }
  if (slot.next?.localDate && slot.next?.localTime) {
    const ms = Date.parse(`${slot.next.localDate}T${slot.next.localTime}`);
    return isNaN(ms) ? null : ms;
  }
  return null;
}

function slotCountdown(slot: DrawSlot, now: number): string | null {
  const ts = parseExpectedMs(slot);
  if (ts === null || ts <= now) return null;
  const totalSecs = Math.floor((ts - now) / 1000);
  const h = Math.floor(totalSecs / 3600);
  const m = Math.floor((totalSecs % 3600) / 60);
  const s = totalSecs % 60;
  if (h >= 24) {
    const days = Math.floor(h / 24);
    const remH = h % 24;
    return `${days}j ${String(remH).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function slotIsAwaiting(slot: DrawSlot, now: number): boolean {
  const ts = parseExpectedMs(slot);
  if (ts === null) return false;
  return ts <= now && slot.latest?.status !== 'CONFIRMED';
}
