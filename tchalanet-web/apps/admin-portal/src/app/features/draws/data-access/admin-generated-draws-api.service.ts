import { Injectable, ResourceRef, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import { ConsoleDrawLifecycleApi, consoleDrawIdentity } from '@tch/web/console';
import {
  DatePreset,
  DrawStatusFilter,
  GeneratedDrawView,
  GeneratedDrawSalesStatus,
  GeneratedDrawResultStatus,
  DrawLifecycleAction,
  GeneratedDrawsQuery,
  SaveDrawResultRequest,
  isGeneratedDrawSellableNow,
} from './admin-generated-draws.models';

export interface DrawView {
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
    case 'LAST_48H': {
      const d = new Date(today);
      d.setDate(d.getDate() - 1);
      return { from: fmt(d), to: fmt(today) };
    }
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

function mapDrawView(d: DrawView): GeneratedDrawView {
  const identity = consoleDrawIdentity({
    channelCode: d.channel.code,
    channelName: d.channel.name,
    slotKey: d.slot.key,
    slotLabel: d.slot.label,
    officialDateLabel: d.drawDate,
    officialTimeLabel: d.slot.drawTime,
    officialTimezoneLabel: d.slot.timezone,
    fallbackTitle: d.channel.code,
  });
  const providerCode = identity.providerCode ?? 'UNK';
  const salesStatus = mapSalesStatus(d.status);
  const resultStatus = mapResultStatus(d.status, d.lastResult);
  const numbers = d.lastResult
    ? [d.lastResult.lot1, d.lastResult.lot2, d.lastResult.lot3].filter((x): x is string => x != null)
    : null;

  return {
    drawId: d.id,
    drawChannelId: d.channel.id,
    drawChannelCode: d.channel.code,
    providerCode,
    providerLabel: identity.providerName ?? providerCode,
    slotKey: d.slot.key,
    slotLabel: identity.channelShortName ?? identity.slotLabel ?? d.slot.key,
    label: identity.channelName ?? d.channel.name,
    businessDate: d.drawDate,
    scheduledAt: d.scheduledAt,
    cutoffAt: d.cutoffAt,
    timezone: d.slot.timezone ?? 'America/New_York',
    salesStatus,
    resultStatus,
    resultMode: 'MANUAL',
    resultId: d.lastResult?.id ?? null,
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
      case 'OPEN':         return isGeneratedDrawSellableNow(d);
      case 'SCHEDULED':
      case 'LOCKED':
      case 'CLOSED':
      case 'RESULTED':
      case 'SETTLED':
      case 'CANCELLED':
      case 'ARCHIVED':     return d.lifecycleStatus === status;
      case 'PAST':         return (d.salesStatus === 'OPEN' && !isGeneratedDrawSellableNow(d))
        || d.salesStatus === 'CLOSED'
        || d.salesStatus === 'CANCELLED'
        || d.resultStatus === 'EXPECTED'
        || d.resultStatus === 'MISSING'
        || d.resultStatus === 'CONFIRMED';
      case 'EXPECTED_OR_MISSING':
        return d.resultStatus === 'EXPECTED' || d.resultStatus === 'MISSING';
      case 'EXPECTED':     return d.resultStatus === 'EXPECTED';
      case 'MISSING':      return d.resultStatus === 'MISSING';
      case 'PROVISIONAL':  return d.resultStatus === 'PROVISIONAL';
      case 'CONFIRMED':    return d.resultStatus === 'CONFIRMED';
      case 'SOURCE_ERROR': return d.resultStatus === 'SOURCE_ERROR';
      case 'NOT_DUE':      return d.resultStatus === 'NOT_DUE';
      default: return true;
    }
  });
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminGeneratedDrawsApiService {
  private readonly backend = inject(TchBackendClient);
  private readonly lifecycle = inject(ConsoleDrawLifecycleApi);

  /**
   * Resource de lecture des tirages générés (créée par TchBackendClient).
   * Le filtre de statut n'est PAS envoyé au backend : il est appliqué côté client
   * par {@link projectDraws} — donc ne pas l'inclure dans `query` évite un refetch inutile.
   */
  generatedDrawsResource(
    query: () => GeneratedDrawsQuery,
  ): ResourceRef<TchPage<GeneratedDrawView> | undefined> {
    // mapping DTO -> vue au niveau du client (project) ; le resource yield des vues métier
    return this.backend.getPageResource<GeneratedDrawView, DrawView>(
      () => {
        const q = query();
        const presetRange = datePresetToRange(q.datePreset ?? 'LAST_48H');
        const from = q.from || presetRange.from;
        const to = q.to || presetRange.to;
        return {
          path: '/admin/draws',
          options: {
            suppressShellFeedback: true,
            params: {
              from,
              to,
              // 20 rows per page by default: the 2-day window regularly holds 50-60+ draws,
              // and dumping them all onto one unpaginated page defeats the "next page" control
              // entirely (there was never a second page to go to).
              size: String(q.size ?? 20),
              page: String(q.page ?? 0),
              ...(q.q ? { q: q.q } : {}),
            },
          },
        };
      },
      mapDrawView,
    );
  }

  /**
   * Filtre de statut client (indépendant du fetch — ne pas l'envoyer au backend).
   * Le mapping DTO -> vue est fait par le resource ({@link generatedDrawsResource}).
   */
  filterDrawsByStatus(
    draws: readonly GeneratedDrawView[],
    status?: DrawStatusFilter | null,
  ): GeneratedDrawView[] {
    return applyStatusFilter([...draws], status && status !== 'all' ? status : null);
  }

  getDrawById(drawId: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend
      .get<DrawView>(`/admin/draws/${drawId}`, options)
      .pipe(map(mapDrawView));
  }

  saveDrawResult(request: SaveDrawResultRequest, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    const [n1, n2, n3] = request.numbers;
    const pick3 = n1;
    const pick4 = `${n2}${n3}`;
    const observeTrustPolicy = request.mode !== 'confirmed';
    return this.backend
      .post<DrawView>(`/admin/draws/${request.drawId}/manual-result`, {
        recordedBy: null,
        notes: request.note || null,
        pick3,
        pick4,
        force: request.force ?? false,
        reason: request.force
          ? 'Override manuel super admin'
          : request.mode === 'confirmed'
            ? 'Saisie manuelle confirmée'
            : 'Saisie provisoire',
        observeTrustPolicy,
      }, options)
      .pipe(map(mapDrawView));
  }

  cancelDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('cancel', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  openDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('open', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  closeDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('close', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  lockDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('lock', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  unlockDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('unlock', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  archiveDraw(drawId: string, reason?: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('archive', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  lifecycleDraws(
    action: DrawLifecycleAction,
    drawIds: readonly string[],
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView[]> {
    const payload = action === 'cancel'
      ? {
          drawIds: [...drawIds],
          reasonCode: 'ADMIN_REQUEST',
          reasonLabel: reason ?? null,
          force: false,
        }
      : {
          drawIds: [...drawIds],
          reason,
          force: false,
        };

    return this.lifecycle.execute<DrawView>(action, payload, options).pipe(
      map(draws => draws.map(mapDrawView)),
    );
  }
}
