import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import {
  DatePreset,
  GeneratedDrawView,
  GeneratedDrawSalesStatus,
  GeneratedDrawResultStatus,
  GeneratedDrawsQuery,
  PagedResult,
  SaveDrawResultRequest,
} from './admin-generated-draws.models';

interface DrawView {
  readonly id: string;
  readonly tenantId: string;
  readonly channel: { readonly id: string; readonly code: string; readonly name: string };
  readonly slot: {
    readonly id: string;
    readonly key: string;
    readonly label: string | null;
    readonly timezone: string | null;
    readonly drawTime: string | null;
  };
  readonly drawDate: string;
  readonly scheduledAt: string;
  readonly cutoffAt: string;
  readonly status: string;
  readonly active: boolean;
  readonly lastResult: {
    readonly id: string;
    readonly occurredAt: string;
    readonly fetchedAt?: string | null;
    readonly status: string;
    readonly lot1: string | null;
    readonly lot2: string | null;
    readonly lot3: string | null;
    readonly lot4: string | null;
  } | null;
}

// ── Mapping helpers ──────────────────────────────────────────────────────────

function datePresetToRange(preset: DatePreset): { from: string; to: string } {
  const today = new Date();
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  switch (preset) {
    case 'TODAY':
      return { from: fmt(today), to: fmt(today) };
    case 'TOMORROW': {
      const d = new Date(today);
      d.setDate(d.getDate() + 1);
      return { from: fmt(d), to: fmt(d) };
    }
    case 'THIS_WEEK': {
      const d = new Date(today);
      d.setDate(d.getDate() + 6);
      return { from: fmt(today), to: fmt(d) };
    }
  }
}

function mapSalesStatus(status: string): GeneratedDrawSalesStatus {
  switch (status) {
    case 'OPEN': return 'OPEN';
    case 'LOCKED':
    case 'CLOSED':
    case 'RESULTED':
    case 'SETTLED': return 'CLOSED';
    case 'CANCELLED':
    case 'ARCHIVED': return 'CANCELLED';
    default: return 'UPCOMING';
  }
}

function mapResultStatus(
  status: string,
  lastResult: DrawView['lastResult'],
): GeneratedDrawResultStatus {
  if (!lastResult) {
    if (status === 'SCHEDULED' || status === 'OPEN') return 'NOT_DUE';
    if (status === 'LOCKED' || status === 'CLOSED') return 'EXPECTED';
    return 'NOT_DUE';
  }
  switch (lastResult.status) {
    case 'CONFIRMED':
    case 'OVERRIDDEN': return 'CONFIRMED';
    case 'PROVISIONAL': return 'PROVISIONAL';
    default: return 'MISSING';
  }
}

function slotLabelFromKey(key: string): string {
  const part = key.split('_').pop() ?? key;
  const labels: Record<string, string> = {
    MID: 'Midday', MIDDAY: 'Midday', EVE: 'Evening', EVENING: 'Evening',
    DAY: 'Day', NIGHT: 'Night', MORNING: 'Morning', NOON: 'Noon',
  };
  return labels[part] ?? part;
}

function providerCodeFromSlotKey(key: string): string {
  return (key.trim().split(/[-_]/)[0] || 'UNK').toUpperCase();
}

function mapDrawView(d: DrawView): GeneratedDrawView {
  const providerCode = providerCodeFromSlotKey(d.slot.key);
  const salesStatus = mapSalesStatus(d.status);
  const resultStatus = mapResultStatus(d.status, d.lastResult);
  const numbers = d.lastResult
    ? [d.lastResult.lot1, d.lastResult.lot2, d.lastResult.lot3].filter((x): x is string => x != null)
    : null;

  return {
    drawId: d.id,
    drawChannelId: d.channel.id,
    providerCode,
    providerLabel: d.channel.name,
    slotKey: d.slot.key,
    slotLabel: slotLabelFromKey(d.slot.key),
    label: d.channel.name,
    businessDate: d.drawDate,
    scheduledAt: d.scheduledAt,
    timezone: d.slot.timezone ?? 'America/New_York',
    salesStatus,
    resultStatus,
    resultMode: 'MANUAL',
    publicationStatus: resultStatus === 'CONFIRMED' || resultStatus === 'PROVISIONAL'
      ? 'PUBLISHED'
      : 'NOT_PUBLISHED',
    numbers: numbers?.length ? numbers : null,
    fetchedAt: d.lastResult?.fetchedAt ?? null,
    sourceError: null,
    lifecycleStatus: d.status,
  };
}

function applyStatusFilter(draws: GeneratedDrawView[], status: string | null | undefined): GeneratedDrawView[] {
  if (!status || status === 'all') return draws;
  return draws.filter(d => {
    switch (status) {
      case 'OPEN':         return d.salesStatus === 'OPEN';
      case 'EXPECTED':     return d.resultStatus === 'EXPECTED';
      case 'MISSING':      return d.resultStatus === 'MISSING';
      case 'CONFIRMED':    return d.resultStatus === 'CONFIRMED';
      case 'SOURCE_ERROR': return d.resultStatus === 'SOURCE_ERROR';
      default: return true;
    }
  });
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminGeneratedDrawsApiService {
  private readonly backend = inject(TchBackendClient);

  getGeneratedDraws(
    params: GeneratedDrawsQuery = {},
    options?: TchRequestOptions,
  ): Observable<PagedResult<GeneratedDrawView>> {
    const { from, to } = datePresetToRange(params.datePreset ?? 'TODAY');
    const q = new URLSearchParams(
      Object.fromEntries(
        ([
          ['from', from],
          ['to', to],
          ['size', String(params.size ?? 100)],
          ['page', String(params.page ?? 0)],
          ...(params.q ? [['q', params.q]] : []),
        ] as [string, string][]).filter(([, v]) => v != null && v !== ''),
      ),
    ).toString();

    return this.backend.get<TchPage<DrawView>>(`/admin/draws?${q}`, options).pipe(
      map(page => {
        const mapped = page.items.map(mapDrawView);
        const filtered = applyStatusFilter(mapped, params.status);
        return {
          content: filtered,
          totalElements: filtered.length,
          page: page.page,
          size: page.size,
        };
      }),
    );
  }

  saveDrawResult(request: SaveDrawResultRequest, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    const pick3 = request.numbers.join('-');
    const observeTrustPolicy = request.mode !== 'confirmed';
    return this.backend
      .post<DrawView>(`/admin/draws/${request.drawId}/manual-result`, {
        recordedBy: null,
        notes: request.note || null,
        pick3,
        pick4: null,
        force: false,
        reason: request.mode === 'confirmed' ? 'Saisie manuelle confirmée' : 'Saisie provisoire',
        observeTrustPolicy,
      }, options)
      .pipe(map(mapDrawView));
  }

  cancelDraw(drawId: string, reasonCode: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend
      .post<DrawView>(`/admin/draws/${drawId}/cancel`, { reasonCode, force: false }, options)
      .pipe(map(mapDrawView));
  }

  lockDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend
      .post<DrawView>(`/admin/draws/${drawId}/lock`, { reason }, options)
      .pipe(map(mapDrawView));
  }

  unlockDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend
      .post<DrawView>(`/admin/draws/${drawId}/unlock`, { reason }, options)
      .pipe(map(mapDrawView));
  }

  archiveDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend
      .post<DrawView>(`/admin/draws/${drawId}/archive`, { reason, force: false }, options)
      .pipe(map(mapDrawView));
  }
}
