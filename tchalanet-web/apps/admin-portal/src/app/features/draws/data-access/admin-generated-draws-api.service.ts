import { Injectable, ResourceRef, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { TCH_DEFAULT_PAGE_SIZE, TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { ConsoleDrawLifecycleApi, consoleDrawIdentity } from '@tch/web/console';
import {
  DatePreset,
  DrawStatusFilter,
  GeneratedDrawView,
  GeneratedDrawsSummary,
  GeneratedDrawSalesStatus,
  GeneratedDrawResultStatus,
  DrawLifecycleAction,
  GeneratedDrawsQuery,
  SaveDrawResultRequest,
  TENANT_TIMEZONE_FALLBACK,
  shiftIsoDate,
  tenantTodayIsoDate,
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

export interface GeneratedDrawsSummaryQuery {
  readonly datePreset?: DatePreset;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly today: string;
}

// ── Mapping helpers ──────────────────────────────────────────────────────────

/**
 * Presets resolve against the tenant's calendar, not the browser's: `from`/`to` bound the
 * channel-local `drawDate`, so a UTC-derived "today" would query the wrong day every evening
 * in Haiti (see {@link tenantTodayIsoDate}).
 */
function datePresetToRange(preset: DatePreset, timezone: string): { from: string; to: string } {
  const today = tenantTodayIsoDate(timezone);
  switch (preset) {
    case 'LAST_48H':
      return { from: shiftIsoDate(today, -1), to: today };
    case 'TODAY':
      return { from: today, to: today };
    case 'TOMORROW': {
      const tomorrow = shiftIsoDate(today, 1);
      return { from: tomorrow, to: tomorrow };
    }
    case 'THIS_WEEK':
      return { from: today, to: shiftIsoDate(today, 6) };
  }
}

function mapSalesStatus(status: string): GeneratedDrawSalesStatus {
  switch (status) {
    case 'OPEN':
      return 'OPEN';
    case 'LOCKED':
    case 'CLOSED':
    case 'RESULTED':
    case 'SETTLED':
      return 'CLOSED';
    case 'CANCELLED':
    case 'ARCHIVED':
      return 'CANCELLED';
    default:
      return 'UPCOMING';
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
    case 'OVERRIDDEN':
      return 'CONFIRMED';
    case 'PROVISIONAL':
      return 'PROVISIONAL';
    default:
      return 'MISSING';
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
    ? [d.lastResult.lot1, d.lastResult.lot2, d.lastResult.lot3, d.lastResult.lot4].filter(
        (x): x is string => x != null && x.trim().length > 0,
      )
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
    publicationStatus:
      resultStatus === 'CONFIRMED' || resultStatus === 'PROVISIONAL'
        ? 'PUBLISHED'
        : 'NOT_PUBLISHED',
    numbers: numbers?.length ? numbers : null,
    fetchedAt: d.lastResult?.fetchedAt ?? null,
    sourceError: null,
    lifecycleStatus: d.status,
  };
}

/**
 * `/admin/draws` (`DrawQueryAdminController.listDraws`, backed by `DrawSearchRequest`) only
 * accepts `resultSlotId`/`status`/`from`/`to` — it has no text-search field, so a `q` query
 * param is silently dropped server-side. Filter client-side instead, over whatever page is
 * already loaded — same tradeoff the status filter already accepts (see
 * {@link applyStatusFilter}): a match on another page won't surface without changing the date
 * range, but that's the existing, understood limitation of the client-side filters here.
 */
/**
 * Strips separators/case so "GA-", "ga_late" and "GA Late" all match a channel code stored as
 * "GA_LATE" — the admin types what the card *displays* (e.g. the identity title "GA-Late"), not
 * necessarily the raw backend code, which mixes `_`/`-` inconsistently across providers.
 */
function normalizeForSearch(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]/g, '');
}

export function applyQueryFilter(
  draws: GeneratedDrawView[],
  query: string | null | undefined,
): GeneratedDrawView[] {
  const needle = query?.trim() ? normalizeForSearch(query) : '';
  if (!needle) return draws;
  return draws.filter(d =>
    [d.label, d.drawChannelCode, d.slotKey, d.slotLabel, d.providerCode, d.providerLabel].some(
      field => field && normalizeForSearch(field).includes(needle),
    ),
  );
}

/**
 * Lifecycle statuses the backend can filter on (`GET /admin/draws?status=`). Anything not listed
 * here is derived from the result state and stays a client-side filter.
 *
 * Note the spelling: the backend enum is `CANCELED` (single L), the UI filter key is `CANCELLED`.
 */
const SERVER_STATUS_PARAM: Readonly<Record<string, string>> = {
  SCHEDULED: 'SCHEDULED',
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
  RESULTED: 'RESULTED',
  SETTLED: 'SETTLED',
  CANCELLED: 'CANCELED',
  ARCHIVED: 'ARCHIVED',
};

/** Backend `status` param for a UI filter key, or null when the filter is client-side only. */
export function serverStatusParam(status: string | null | undefined): string | null {
  if (!status || status === 'all') return null;
  return SERVER_STATUS_PARAM[status] ?? null;
}

/**
 * True when the backend fully answers this filter, so no client-side narrowing is needed.
 * PAST is served by `scheduledBefore=<now>` rather than by `status`.
 */
function isServerFiltered(status: string | null | undefined): boolean {
  return status === 'PAST' || serverStatusParam(status) !== null;
}

function applyStatusFilter(
  draws: GeneratedDrawView[],
  status: string | null | undefined,
): GeneratedDrawView[] {
  // Lifecycle statuses (OPEN, CLOSED, …) and PAST are already narrowed by the backend —
  // re-filtering here would only desync the rows from the server's totalElements/pagination.
  if (isServerFiltered(status)) return draws;
  if (!status || status === 'all') return draws;
  return draws.filter(d => {
    switch (status) {
      case 'LOCKED':
        return d.lifecycleStatus === status;
      case 'EXPECTED_OR_MISSING':
        return d.resultStatus === 'EXPECTED' || d.resultStatus === 'MISSING';
      case 'EXPECTED':
        return d.resultStatus === 'EXPECTED';
      case 'MISSING':
        return d.resultStatus === 'MISSING';
      case 'PROVISIONAL':
        return d.resultStatus === 'PROVISIONAL';
      case 'CONFIRMED':
        return d.resultStatus === 'CONFIRMED';
      case 'SOURCE_ERROR':
        return d.resultStatus === 'SOURCE_ERROR';
      case 'NOT_DUE':
        return d.resultStatus === 'NOT_DUE';
      default:
        return true;
    }
  });
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminGeneratedDrawsApiService {
  private readonly backend = inject(TchBackendClient);
  private readonly lifecycle = inject(ConsoleDrawLifecycleApi);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);

  /** Tenant calendar timezone — the one `drawDate` is expressed in. */
  tenantTimezone(): string {
    const value = this.runtimeSettings.settings().values['app.timezone'];
    return typeof value === 'string' && value.trim() ? value : TENANT_TIMEZONE_FALLBACK;
  }

  /**
   * Resource de lecture des tirages générés (créée par TchBackendClient).
   * Le filtre de statut n'est PAS envoyé au backend : il est appliqué côté client
   * par {@link projectDraws} — donc ne pas l'inclure dans `query` évite un refetch inutile.
   */
  generatedDrawsResource(
    query: () => GeneratedDrawsQuery,
  ): ResourceRef<TchPage<GeneratedDrawView> | undefined> {
    // mapping DTO -> vue au niveau du client (project) ; le resource yield des vues métier
    return this.backend.getPageResource<GeneratedDrawView, DrawView>(() => {
      const q = query();
      const presetRange = datePresetToRange(q.datePreset ?? 'LAST_48H', this.tenantTimezone());
      const from = q.from || presetRange.from;
      const to = q.to || presetRange.to;
      const status = serverStatusParam(q.status);
      return {
        path: '/admin/draws',
        options: {
          suppressShellFeedback: true,
          params: {
            from,
            to,
            // Lifecycle statuses and PAST are filtered server-side so pagination and
            // totalElements stay accurate; result-derived filters (EXPECTED, CONFIRMED…)
            // have no backend equivalent and remain client-side.
            ...(status ? { status } : {}),
            // "Passés" is a time-of-day question, not a business-date one: a draw at 12:29
            // today is past at 14:00 while still sitting inside today's from/to window.
            ...(q.status === 'PAST' ? { scheduledBefore: new Date().toISOString() } : {}),
            // The 2-day window regularly holds 50-60+ draws — dumping them all onto one
            // unpaginated page defeats the "next page" control entirely (there was never a
            // second page to go to).
            size: String(q.size ?? TCH_DEFAULT_PAGE_SIZE),
            page: String(q.page ?? 0),
          },
        },
      };
    }, mapDrawView);
  }

  generatedDrawsSummaryResource(
    query: () => GeneratedDrawsSummaryQuery,
  ): ResourceRef<GeneratedDrawsSummary | undefined> {
    return this.backend.getResource<GeneratedDrawsSummary>(() => {
      const q = query();
      const presetRange = datePresetToRange(q.datePreset ?? 'LAST_48H', this.tenantTimezone());
      return {
        path: '/admin/draws/summary',
        options: {
          suppressShellFeedback: true,
          params: {
            from: q.from || presetRange.from,
            to: q.to || presetRange.to,
            today: q.today,
          },
        },
      };
    });
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

  /**
   * Filtre de recherche texte client (le backend n'a pas de champ `q`, voir
   * {@link applyQueryFilter}) — matché sur le libellé, le code de canal, le slot et le provider.
   */
  filterDrawsByQuery(
    draws: readonly GeneratedDrawView[],
    query?: string | null,
  ): GeneratedDrawView[] {
    return applyQueryFilter([...draws], query);
  }

  getDrawById(drawId: string, options?: TchRequestOptions): Observable<GeneratedDrawView> {
    return this.backend.get<DrawView>(`/admin/draws/${drawId}`, options).pipe(map(mapDrawView));
  }

  saveDrawResult(
    request: SaveDrawResultRequest,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    const [n1, n2, n3] = request.numbers;
    const pick3 = n1;
    const pick4 = `${n2}${n3}`;
    const observeTrustPolicy = request.mode !== 'confirmed';
    return this.backend
      .post<DrawView>(
        `/admin/draws/${request.drawId}/manual-result`,
        {
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
        },
        options,
      )
      .pipe(map(mapDrawView));
  }

  cancelDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('cancel', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  openDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('open', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  closeDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('close', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  lockDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('lock', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  unlockDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('unlock', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  archiveDraw(
    drawId: string,
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView> {
    return this.lifecycleDraws('archive', [drawId], reason, options).pipe(map(draws => draws[0]));
  }

  lifecycleDraws(
    action: DrawLifecycleAction,
    drawIds: readonly string[],
    reason?: string,
    options?: TchRequestOptions,
  ): Observable<GeneratedDrawView[]> {
    const payload =
      action === 'cancel'
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

    return this.lifecycle
      .execute<DrawView>(action, payload, options)
      .pipe(map(draws => draws.map(mapDrawView)));
  }
}
