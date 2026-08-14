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
import { actionFrom, destinationHref, isRecord, stringProp } from '@tch/page-model';
import { BadgeStatus, TchActionButton, TchCard, TchSectionHeader, TchStatusBadge } from '@tch/ui/components';
import { TranslateService } from '@ngx-translate/core';

interface DrawResultItem {
  readonly drawResultId?: string;
  readonly slotKey?: string;
  readonly provider?: string;
  readonly drawChannelLabelKey?: string;
  readonly drawChannelLabel?: string;
  readonly resultDate?: string;
  readonly drawTime?: string;
  readonly timezone?: string;
  readonly occurredAt?: string;
  readonly status?: string;
  readonly numbers?: readonly string[];
  readonly publishedAt?: string | null;
  readonly nextResultAt?: string | null;
  readonly detailPath?: string;
  readonly label?: string;
  readonly next?: {
    readonly status?: string;
    readonly localDate?: string;
    readonly localTime?: string;
    readonly expectedAt?: string;
    readonly countdownSeconds?: number;
  };
  readonly latest?: {
    readonly drawResultId?: string;
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
  templateUrl: './draw-results.widget.html',
  styleUrl: './draw-results.widget.scss',
})
export class PublicDrawResultsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
  private readonly translate = inject(TranslateService);

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

  readonly moreLabelKey = 'public.results.all';

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
      .filter(slot => extractNumbers(slot).length > 0)
      .sort((a, b) => occurredAtMs(b) - occurredAtMs(a))
      .slice(0, this.maxSlots());
  });

  private readonly serverNowMs = computed<number>(() => {
    const data = this.dynamic();
    if (isRecord(data) && typeof data['serverNow'] === 'string') {
      const ms = Date.parse(data['serverNow'] as string);
      return isNaN(ms) ? Date.now() : ms;
    }
    return Date.now();
  });

  private readonly clientNowAtLoad = signal(Date.now());

  readonly slotsWithCountdown = computed<readonly DrawSlotView[]>(() => {
    const clientNow = this._now();
    const serverNowMs = this.serverNowMs();
    const clientNowAtLoad = this.clientNowAtLoad();
    const adjustedNow = serverNowMs + (clientNow - clientNowAtLoad);
    return this.slots().map(slot => ({
      ...slot,
      countdown: slotCountdown(slot, adjustedNow, clientNowAtLoad),
      isAwaiting: slotIsAwaiting(slot, adjustedNow, clientNowAtLoad),
    }));
  });

  slotStatusLabel(slot: DrawResultItem): string {
    if (!this.numbers(slot).length) return 'public.results.awaiting';
    const s = slot.status ?? slot.latest?.status;
    switch (s) {
      case 'CONFIRMED':  return 'domain.result.status.CONFIRMED';
      case 'OVERRIDDEN': return 'domain.result.status.OVERRIDDEN';
      case 'ERROR':      return 'domain.result.status.ERROR';
      default:           return 'domain.result.status.PROVISIONAL';
    }
  }

  slotBadgeStatus(slot: DrawResultItem): BadgeStatus {
    if (!this.numbers(slot).length) return 'pending';
    const s = slot.status ?? slot.latest?.status;
    if (s === 'CONFIRMED' || s === 'OVERRIDDEN') return 'ready';
    if (s === 'ERROR') return 'blocked';
    return 'pending';
  }

  formatDate(iso: string | undefined): string {
    return formatResultDate(iso);
  }

  slotTime(slot: DrawResultItem): string {
    const t = slot.drawTime ?? slot.next?.localTime ?? '';
    return t.length > 5 ? t.substring(0, 5) : t;
  }

  slotTitle(slot: DrawResultItem): string {
    const explicit = publicLabel(slot.drawChannelLabel) ?? publicLabel(slot.label);
    if (explicit) return explicit;

    const key = publicLabel(slot.drawChannelLabelKey);
    if (key) {
      const translated = this.translate.instant(key);
      if (typeof translated === 'string' && translated.trim() && translated !== key) {
        return translated;
      }
    }

    return humanSlotLabel(slot.slotKey) ?? publicLabel(slot.provider) ?? '';
  }

  numbers(slot: DrawResultItem): readonly string[] {
    return extractNumbers(slot);
  }

  shouldShowCaMiddayDaily4Note(slot: DrawResultItem): boolean {
    return isCaliforniaMidday(slot.slotKey) && this.numbers(slot).length > 0 && this.numbers(slot).length < 4;
  }

  detailHref(slot: DrawResultItem): string | null {
    if (slot.detailPath) return slot.detailPath;
    const id = slot.drawResultId ?? slot.latest?.drawResultId;
    return id ? `/public/results/${encodeURIComponent(id)}` : null;
  }
}

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

function isCaliforniaMidday(slotKey: string | undefined): boolean {
  return (slotKey ?? '').trim().toUpperCase() === 'CA_MID';
}

function publicLabel(value: string | undefined): string | null {
  const clean = value?.trim();
  if (!clean) return null;
  const normalized = clean.toLowerCase();
  if (normalized === 'label' || normalized === 'null' || normalized === 'undefined') return null;
  // Reject raw technical slot keys like MO_MID, PA_MID, FL_LATE — fall through to humanSlotLabel
  if (/^[A-Z]{2,3}_[A-Z0-9]+$/.test(clean)) return null;
  return clean;
}

/** Formats an ISO date string (YYYY-MM-DD) for display in French. */
export function formatResultDate(iso: string | null | undefined): string {
  if (!iso) return '';
  // Use local noon to avoid off-by-one day across timezones
  const d = new Date(`${iso}T12:00:00`);
  if (isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat('fr', { day: 'numeric', month: 'short', year: 'numeric' }).format(d);
}

function humanSlotLabel(slotKey: string | undefined): string | null {
  const clean = slotKey?.trim().toUpperCase();
  if (!clean) return null;
  const [provider, period] = clean.split('_', 2);
  if (!provider || !period) return clean.replace(/[_-]+/g, ' ');
  const providerLabel = PROVIDER_LABELS[provider] ?? provider;
  const periodLabel = PERIOD_LABELS[period] ?? period.replace(/[_-]+/g, ' ');
  return `${providerLabel} — ${periodLabel}`;
}

const PROVIDER_LABELS: Readonly<Record<string, string>> = {
  CA: 'California',
  FL: 'Florida',
  GA: 'Georgia',
  IL: 'Illinois',
  MI: 'Michigan',
  MN: 'Minnesota',
  MO: 'Missouri',
  NJ: 'New Jersey',
  NY: 'New York',
  OH: 'Ohio',
  PA: 'Pennsylvania',
  TN: 'Tennessee',
  TX: 'Texas',
};

const PERIOD_LABELS: Readonly<Record<string, string>> = {
  MID: 'Midi',
  EVE: 'Soir',
  LATE: 'Nuit',
  '1000': '10:00',
  '1227': '12:27',
  '1800': '18:00',
  '2212': '22:12',
};

function occurredAtMs(slot: DrawResultItem): number {
  const iso = slot.occurredAt ?? slot.latest?.occurredAt;
  if (iso) {
    const ms = Date.parse(iso);
    if (!isNaN(ms)) return ms;
  }
  return 0;
}

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
