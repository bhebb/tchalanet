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

/**
 * One item from GET /public/draw-results/latest.
 * All fields are optional so the widget tolerates partial or legacy payloads.
 */
interface DrawResultItem {
  // ── Spec fields (latest / history response) ──
  readonly drawResultId?: string;
  readonly slotKey?: string;
  readonly provider?: string;
  /**
   * Stable i18n key sent by the server (e.g. "draw_channel.ny.mid.label").
   * Preferred over drawChannelLabel for display — pipe through | tchLabel.
   */
  readonly drawChannelLabelKey?: string;
  /**
   * Raw label from server — in the home-draws legacy payload this is just
   * the slot code (e.g. "NY_MID"), not a human-readable string.
   * Use drawChannelLabelKey | tchLabel instead.
   */
  readonly drawChannelLabel?: string;
  readonly resultDate?: string;
  readonly drawTime?: string;
  readonly timezone?: string;
  readonly occurredAt?: string;
  readonly status?: string;
  readonly numbers?: readonly string[];
  readonly publishedAt?: string | null;
  /** ISO timestamp of the next expected result — used to compute the live countdown. */
  readonly nextResultAt?: string | null;
  /** Backend-provided path, e.g. "/public/results/<uuid>" */
  readonly detailPath?: string;
  // ── Legacy slot fields (home.draws payload) ──
  readonly label?: string;
  readonly next?: {
    readonly status?: string;
    readonly localDate?: string;
    readonly localTime?: string;
    readonly expectedAt?: string;
    /** Pre-computed countdown in seconds at response time — seed for the timer. */
    readonly countdownSeconds?: number;
  };
  readonly latest?: {
    readonly drawResultId?: string;   // UUID — backend must include for direct detail linking
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

interface DrawSlotView extends DrawResultItem {
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
          {{ moreLabelKey | tchLabel }}
        </a>
      </tch-section-header>
    </div>

    @if (slotsWithCountdown().length) {
      <ul class="latest-results-widget__list" role="list">
        @for (slot of slotsWithCountdown(); track slot.drawResultId ?? slot.slotKey ?? $index) {
          <li>
            <tch-card class="latest-results-widget__item">
              <div class="latest-results-widget__item-top">
                <div class="latest-results-widget__item-meta">
                  <h3 class="latest-results-widget__item-name">
                    {{ (slot.drawChannelLabelKey || slot.label || slot.slotKey || slot.provider) | tchLabel }}
                  </h3>
                  <p class="latest-results-widget__item-slot-key">{{ slot.slotKey }}</p>
                  <time class="latest-results-widget__item-date">
                    {{ slot.resultDate || slot.latest?.resultDate || slot.next?.localDate || '' }}{{ slotTime(slot) ? ' · ' + slotTime(slot) : '' }}
                  </time>
                </div>
                <tch-status-badge
                  [status]="slotBadgeStatus(slot)"
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

              @if (detailHref(slot); as href) {
                <a tch-action variant="tertiary" class="latest-results-widget__detail-link" [attr.href]="href">
                  {{ 'public.results.detail' | tchLabel }}
                </a>
              }
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

        /* On narrow screens, stack the badge below the meta block */
        @media (max-width: 400px) {
          flex-direction: column-reverse;
          gap: 0.5rem;
        }
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

      .latest-results-widget__item-slot-key {
        margin: 0;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-family: var(--tch-font-family-mono, monospace);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        opacity: 0.6;
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

  // Spec: global action always "Tous les résultats", regardless of server labelKey.
  readonly moreLabelKey = 'public.results.all';

  /**
   * Max slots to display on the home preview.
   * Reads `maxSlots` from widget config (server-declared); absolute cap is 9
   * (3 cols × 3 rows desktop). The full history is on /public/results.
   */
  private readonly maxSlots = computed<number>(() => {
    const raw = this.config()?.props?.['maxSlots'];
    const n = typeof raw === 'number' ? raw : Number(raw);
    return Number.isFinite(n) && n > 0 ? Math.min(n, 9) : 9;
  });

  private readonly slots = computed<readonly DrawResultItem[]>(() => {
    const data = this.dynamic();
    if (!isRecord(data)) return [];
    const raw: readonly DrawResultItem[] = Array.isArray(data['items'])
      ? (data['items'] as readonly DrawResultItem[])
      : Array.isArray(data['slots'])
        ? (data['slots'] as readonly DrawResultItem[])
        : [];

    return [...raw]
      // Keep only slots that have at least one number published
      .filter(slot => extractNumbers(slot).length > 0)
      // Most recent result first
      .sort((a, b) => occurredAtMs(b) - occurredAtMs(a))
      // Home preview: at most maxSlots cards
      .slice(0, this.maxSlots());
  });

  /** Server reference time from the latest response payload; used to anchor countdowns. */
  private readonly serverNowMs = computed<number>(() => {
    const data = this.dynamic();
    if (isRecord(data) && typeof data['serverNow'] === 'string') {
      const ms = Date.parse(data['serverNow'] as string);
      return isNaN(ms) ? Date.now() : ms;
    }
    return Date.now();
  });

  /** Client time at the moment serverNow was received (used to compute elapsed drift). */
  private readonly clientNowAtLoad = signal(Date.now());

  readonly slotsWithCountdown = computed<readonly DrawSlotView[]>(() => {
    const clientNow = this._now();
    const serverNowMs = this.serverNowMs();
    const clientNowAtLoad = this.clientNowAtLoad();
    // Adjust server reference time forward by elapsed client time since load
    const adjustedNow = serverNowMs + (clientNow - clientNowAtLoad);
    return this.slots().map(slot => ({
      ...slot,
      countdown: slotCountdown(slot, adjustedNow, clientNowAtLoad),
      isAwaiting: slotIsAwaiting(slot, adjustedNow, clientNowAtLoad),
    }));
  });

  /**
   * Status label rules (spec V1):
   * - No numbers            → "En attente des résultats"
   * - Numbers + CONFIRMED   → "Résultats confirmés"
   * - Numbers + other       → "En attente de confirmation"
   * Never show "Indisponible" when numbers are present.
   */
  slotStatusLabel(slot: DrawResultItem): string {
    if (!this.numbers(slot).length) return 'public.results.awaiting';
    const s = slot.status ?? slot.latest?.status;
    switch (s) {
      case 'CONFIRMED':  return 'public.results.status.CONFIRMED';
      case 'OVERRIDDEN': return 'public.results.status.OVERRIDDEN';
      case 'ERROR':      return 'public.results.status.ERROR';
      default:           return 'public.results.status.PROVISIONAL';
    }
  }

  slotBadgeStatus(slot: DrawResultItem): BadgeStatus {
    if (!this.numbers(slot).length) return 'pending';
    const s = slot.status ?? slot.latest?.status;
    if (s === 'CONFIRMED' || s === 'OVERRIDDEN') return 'ready';
    if (s === 'ERROR') return 'blocked';
    return 'pending';
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  slotTime(slot: DrawResultItem): string {
    const t = slot.drawTime ?? slot.next?.localTime ?? '';
    return t.length > 5 ? t.substring(0, 5) : t;
  }

  numbers(slot: DrawResultItem): readonly string[] {
    return extractNumbers(slot);
  }

  /**
   * Returns the detail href for a slot, or null when no stable ID is available.
   * Priority: spec detailPath → top-level drawResultId → latest.drawResultId.
   * Returns null for home-draws slots where the backend has not yet included a drawResultId.
   */
  detailHref(slot: DrawResultItem): string | null {
    if (slot.detailPath) return slot.detailPath;
    const id = slot.drawResultId ?? slot.latest?.drawResultId;
    return id ? `/public/results/${encodeURIComponent(id)}` : null;
  }
}

/**
 * Extracts the published numbers from a slot, normalising both the spec
 * format (top-level `numbers[]`) and the legacy home-draws format
 * (`latest.haiti.*` / `latest.source.*`).
 */
function extractNumbers(slot: DrawResultItem): readonly string[] {
  if (slot.numbers?.length) return slot.numbers;
  const haiti = slot.latest?.haiti;
  if (haiti) {
    const vals = Object.values(haiti).filter(v => !!v && v.trim().length > 0);
    if (vals.length) return vals;
  }
  return [
    ...(slot.latest?.source?.pick3?.main ?? []),
    ...(slot.latest?.source?.pick4?.main ?? []),
  ];
}

/** Returns the epoch (ms) of the most recent result for sorting purposes. */
function occurredAtMs(slot: DrawResultItem): number {
  const iso = slot.occurredAt ?? slot.latest?.occurredAt;
  if (iso) {
    const ms = Date.parse(iso);
    if (!isNaN(ms)) return ms;
  }
  return 0;
}

/**
 * Returns the absolute epoch (ms) of the next expected result for a slot.
 *
 * Priority:
 * 1. `nextResultAt` — spec ISO timestamp (new endpoints)
 * 2. `next.expectedAt` — legacy ISO timestamp (home-draws payload)
 * 3. `next.countdownSeconds` — pre-computed server countdown; converted to
 *    an absolute epoch using the client's load time as the origin
 * 4. `next.localDate + localTime` — older legacy format
 */
function parseNextResultMs(slot: DrawResultItem, clientLoadMs = Date.now()): number | null {
  if (slot.nextResultAt) {
    const ms = Date.parse(slot.nextResultAt);
    return isNaN(ms) ? null : ms;
  }
  if (slot.next?.expectedAt) {
    const ms = Date.parse(slot.next.expectedAt);
    return isNaN(ms) ? null : ms;
  }
  if (typeof slot.next?.countdownSeconds === 'number' && slot.next.countdownSeconds > 0) {
    return clientLoadMs + slot.next.countdownSeconds * 1000;
  }
  if (slot.next?.localDate && slot.next?.localTime) {
    const ms = Date.parse(`${slot.next.localDate}T${slot.next.localTime}`);
    return isNaN(ms) ? null : ms;
  }
  return null;
}

function slotCountdown(slot: DrawResultItem, now: number, clientLoadMs: number): string | null {
  const ts = parseNextResultMs(slot, clientLoadMs);
  if (ts === null || ts <= now) return null;
  const totalSecs = Math.floor((ts - now) / 1000);
  const h = Math.floor(totalSecs / 3600);
  const m = Math.floor((totalSecs % 3600) / 60);
  const s = totalSecs % 60;
  if (h >= 24) {
    const days = Math.floor(h / 24);
    const remH = h % 24;
    return `${days}j ${String(remH).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function slotIsAwaiting(slot: DrawResultItem, now: number, clientLoadMs: number): boolean {
  const ts = parseNextResultMs(slot, clientLoadMs);
  if (ts === null) return false;
  return ts <= now && slot.status !== 'CONFIRMED' && slot.latest?.status !== 'CONFIRMED';
}
