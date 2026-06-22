import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

// ── Batch Jobs ──────────────────────────────────────────────────────────────

export interface JobInfoResponse {
  job_key: string;
  display_name: string;
  scope: string;
  required_params: string[];
  optional_params: string[];
}

/** POST /platform/ops/batch/jobs/{jobKey}:start */
export interface StartJobRequest {
  params: Record<string, string>;
}

export interface StartJobResponse {
  job_key: string;
  execution_id: number;
  status: string;
  started_at: string;
}

// ── Batch Gates ─────────────────────────────────────────────────────────────

/** PUT /platform/ops/batch/gates/{jobKey} */
export interface GateUpdateRequest {
  scope: 'GLOBAL' | 'TENANT';
  tenant_id?: string | null;
  enabled: boolean;
  reason: string;
}

// ── Draw Calendar ───────────────────────────────────────────────────────────

export interface GenerateDrawsRequest {
  tenantCodes?: string[];
  from: string;
  to: string;
  dryRun?: boolean;
  force?: boolean;
  reason?: string;
}

export interface OpenTodayDrawsRequest {
  tenantCodes?: string[];
  now?: string;
  drawDate?: string;
  limit?: number;
  dryRun?: boolean;
}

export interface CloseDueDrawsRequest {
  tenantCodes?: string[];
  now?: string;
  limit?: number;
  dryRun?: boolean;
}

export interface ApplyExternalResultsRequest {
  baseDate?: string;
  daysBack?: number;
  slotKeys?: string[];
  force?: boolean;
  dryRun?: boolean;
  maxSlots?: number;
  reason?: string;
}

export interface TenantBatchOutcome<R> {
  tenantId: string;
  ok: boolean;
  result: R | null;
  error: string | null;
}

export interface TenantBatchResponse<R> {
  tenantsRequested: number;
  tenantsSucceeded: number;
  tenantsFailed: number;
  tenants: TenantBatchOutcome<R>[];
}

export interface GenerateDrawsForRangeResult {
  created: number;
  skipped: number;
  alreadyExists: number;
  conflicts: number;
  skippedProviderClosed: number;
}

export interface OpenDueDrawsResult {
  opened: number;
  skippedLocked: number;
  skippedTooLateOrCutoffPassed: number;
  canceledProviderClosed: number;
}

export interface CloseDueDrawsResult {
  closed: number;
  skippedLocked: number;
}

export interface ApplyExternalResultsWindowResult {
  inserted: number;
  updated: number;
  notFound: number;
  errors: number;
}

// ── Draw Results ─────────────────────────────────────────────────────────────

/** POST /platform/ops/draw-results/fetch  (also used for /refresh) */
export interface FetchExternalResultsRequest {
  baseDate?: string;
  daysBack?: number;
  slotKeys?: string[];
  force?: boolean;
  dryRun?: boolean;
  maxSlots?: number;
  reason?: string;
  includeRaw?: boolean;
}

export interface FetchExternalResultsWindowResult {
  inserted: number;
  updated: number;
  noop: number;
  skipped: number;
  notFound: number;
}

export interface RefreshExternalResultsWindowResult {
  fetched: number;
  projectedOk: number;
  projectedFail: number;
  upserted: number;
  applied: number;
  notFound: number;
}

export interface OverrideDrawResultRequest {
  tenantId: string;
  slotKey: string;
  drawDate: string;
  pick3?: string;
  pick4?: string;
  reason: string;
  force?: boolean;
}

export interface RecordManualDrawResultRequest {
  tenantId: string;
  drawDate: string;
  slotKey: string;
  recordedBy: string;
  notes?: string;
  pick3?: string;
  pick4?: string;
  force?: boolean;
  reason?: string;
}

export interface DrawResultOpsResponse {
  id: string;
  slotKey: string;
  occurredAt: string;
  status: string;
  source: string;
  quality: string;
  sourceHash?: string;
  fetchedAt?: string;
  sourceResult?: unknown;
  haitiResult?: unknown;
  rawPayload?: unknown;
  overrideReason?: string;
}

export interface TchPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Draw Lifecycle (admin/draws) ─────────────────────────────────────────────

export interface DrawSummaryResponse {
  drawId: string;
  channelCode: string;
  channelName: string;
  status: string;
  scheduledAt: string;
  openedAt?: string | null;
  closedAt?: string | null;
  settledAt?: string | null;
}

// ── Cache ────────────────────────────────────────────────────────────────────

export interface CacheView {
  cacheName: string;
  size: number;
  hitRate?: number;
  lastClearedAt?: string;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PlatformOpsApi {
  private readonly backend = inject(TchBackendClient);

  // Batch Jobs
  listJobs(): Observable<JobInfoResponse[]> {
    return this.backend.get<JobInfoResponse[]>('/platform/ops/batch/jobs');
  }

  startJob(jobKey: string, req: StartJobRequest): Observable<StartJobResponse> {
    return this.backend.post<StartJobResponse>(`/platform/ops/batch/jobs/${jobKey}:start`, req);
  }

  // Batch Gates
  listGates(jobKeys?: string[]): Observable<Record<string, boolean>> {
    const q = jobKeys?.length ? `?job_keys=${jobKeys.join(',')}` : '';
    return this.backend.get<Record<string, boolean>>(`/platform/ops/batch/gates:effective${q}`);
  }

  updateGate(jobKey: string, req: GateUpdateRequest): Observable<void> {
    return this.backend.put<void>(`/platform/ops/batch/gates/${jobKey}`, req);
  }

  // Draw Calendar
  generateDraws(req: GenerateDrawsRequest): Observable<TenantBatchResponse<GenerateDrawsForRangeResult>> {
    return this.backend.post<TenantBatchResponse<GenerateDrawsForRangeResult>>('/platform/ops/draws/generate', req);
  }

  openTodayDraws(req: OpenTodayDrawsRequest): Observable<TenantBatchResponse<OpenDueDrawsResult>> {
    return this.backend.post<TenantBatchResponse<OpenDueDrawsResult>>('/platform/ops/draws/open-today', req);
  }

  closeDueDraws(req: CloseDueDrawsRequest): Observable<TenantBatchResponse<CloseDueDrawsResult>> {
    return this.backend.post<TenantBatchResponse<CloseDueDrawsResult>>('/platform/ops/draws/close-due', req);
  }

  applyDrawResults(req: ApplyExternalResultsRequest): Observable<ApplyExternalResultsWindowResult> {
    return this.backend.post<ApplyExternalResultsWindowResult>('/platform/ops/draws/apply', req);
  }

  // Draw Results
  listDrawResults(params?: {
    slotKey?: string;
    status?: string;
    quality?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<TchPage<DrawResultOpsResponse>> {
    const q = params
      ? new URLSearchParams(
          Object.fromEntries(
            Object.entries(params)
              .filter(([, v]) => v !== undefined && v !== '')
              .map(([k, v]) => [k, String(v)]),
          ),
        ).toString()
      : '';
    return this.backend.get<TchPage<DrawResultOpsResponse>>(
      `/platform/ops/draw-results${q ? '?' + q : ''}`,
    );
  }

  fetchDrawResults(req: FetchExternalResultsRequest): Observable<FetchExternalResultsWindowResult> {
    return this.backend.post<FetchExternalResultsWindowResult>('/platform/ops/draw-results/fetch', req);
  }

  refreshDrawResults(req: FetchExternalResultsRequest): Observable<RefreshExternalResultsWindowResult> {
    return this.backend.post<RefreshExternalResultsWindowResult>('/platform/ops/draw-results/refresh', req);
  }

  overrideDrawResult(req: OverrideDrawResultRequest): Observable<unknown> {
    return this.backend.post<unknown>('/platform/ops/draw-results/override', req);
  }

  manualDrawResult(req: RecordManualDrawResultRequest): Observable<unknown> {
    return this.backend.post<unknown>('/platform/ops/draw-results/manual', req);
  }

  confirmDrawResult(drawResultId: string): Observable<unknown> {
    return this.backend.post<unknown>(`/platform/ops/draw-results/${drawResultId}/confirm`, {});
  }

  // Draw Lifecycle (per-draw admin actions)
  listDrawsForLifecycle(params: { status?: string; page?: number; size?: number }): Observable<TchPage<DrawSummaryResponse>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPage<DrawSummaryResponse>>(`/admin/draws${q ? '?' + q : ''}`);
  }

  cancelDraw(drawId: string, reason: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/cancel`, { reason });
  }

  lockDraw(drawId: string, reason?: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/lock`, { reason });
  }

  unlockDraw(drawId: string, reason?: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/unlock`, { reason });
  }

  settleDraw(drawId: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/settle`, {});
  }

  archiveDraw(drawId: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/archive`, {});
  }

  rescheduleDraw(drawId: string, newScheduledAt: string): Observable<DrawSummaryResponse> {
    return this.backend.post<DrawSummaryResponse>(`/admin/draws/${drawId}/reschedule`, { newScheduledAt });
  }

  // Cache
  listCaches(): Observable<CacheView[]> {
    return this.backend.get<CacheView[]>('/platform/ops/cache');
  }

  clearCache(cacheName: string): Observable<void> {
    return this.backend.delete<void>(`/platform/ops/cache/${cacheName}`);
  }

  clearAllCaches(reason: string): Observable<void> {
    return this.backend.delete<void>('/platform/ops/cache', { params: { reason } });
  }
}
