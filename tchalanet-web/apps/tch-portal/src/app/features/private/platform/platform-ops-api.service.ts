import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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
  tenantCodes?: string[];
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

export interface OpsJobLaunchItem {
  tenant_id: string | null;
  execution_id: number | null;
  status: string;
  error: string | null;
}

export interface OpsLaunchResponse {
  job_key: string;
  requested: number;
  started: number;
  failed: number;
  launches: OpsJobLaunchItem[];
  message: string;
}

// ── Draw Results ─────────────────────────────────────────────────────────────

/** POST /platform/ops/draw-results/fetch */
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

export interface OverrideDrawResultRequest {
  slotKey: string;
  drawDate: string;
  lot1: string;
  lot2: string;
  lot3: string;
  reason: string;
  force?: boolean;
}

export interface RecordManualDrawResultRequest {
  drawDate: string;
  slotKey: string;
  recordedBy: string;
  notes?: string;
  lot1: string;
  lot2: string;
  lot3: string;
  force?: boolean;
  reason: string;
}

export interface HaitiLots {
  lot1?: string | null;
  lot2?: string | null;
  lot3?: string | null;
  lot4?: string | null;
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
  haitiResult?: HaitiLots | unknown;
  rawPayload?: unknown;
  overrideReason?: string;
}

export type OpsDrawResultQuality = 'COMPLETE' | 'SUSPECT' | 'INVALID';

export interface TchPage<T> {
  items: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Draw Lifecycle (admin/draws) ─────────────────────────────────────────────

/** Matches DrawSummaryResponse Java record (nested channel + slot objects). */
export interface DrawView {
  id: string;
  tenantId: string;
  channel: { id: string; code: string; name: string };
  slot: { id: string; key: string; label: string | null; timezone: string | null; drawTime: string | null };
  drawDate: string;
  scheduledAt: string;
  cutoffAt: string;
  status: string;
  active: boolean;
  lastResult: {
    id: string;
    occurredAt: string;
    status: string;
    lot1: string | null;
    lot2: string | null;
    lot3: string | null;
    lot4: string | null;
  } | null;
}

/** @deprecated Use DrawView — field names were incorrect */
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

export interface CancelDrawRequest {
  drawIds?: string[];
  reasonCode: string;
  reasonLabel?: string;
  force?: boolean;
}

export interface CancelDrawsRequest extends CancelDrawRequest {
  drawIds: string[];
}

export interface LifecycleDrawsRequest {
  drawIds: string[];
  reason?: string;
  force?: boolean;
}

export interface CorrectDrawResultRequest {
  correctedDrawResultId: string;
  reason: string;
  idempotencyKey: string;
  force?: boolean;
}

// ── Batch Executions ─────────────────────────────────────────────────────────

export interface ExecutionResponse {
  execution_id: number;
  job_key: string;
  status: string;
  started_at: string;
  ended_at?: string | null;
  context?: string | null;
  exit_code?: string | null;
  exit_message?: string | null;
}

// ── Cache ────────────────────────────────────────────────────────────────────

export interface CacheView {
  cacheName: string;
  size: number;
  hitRate?: number;
  lastClearedAt?: string;
}

export interface CacheGroupClearResult {
  group: string;
  cleared: string[];
  missing: string[];
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

  listExecutions(jobKey: string, limit = 20): Observable<ExecutionResponse[]> {
    return this.backend.get<ExecutionResponse[]>(
      `/platform/ops/batch/executions?job_key=${encodeURIComponent(jobKey)}&limit=${limit}`,
    );
  }

  getExecution(executionId: number): Observable<ExecutionResponse> {
    return this.backend.get<ExecutionResponse>(`/platform/ops/batch/executions/${executionId}`);
  }

  restartExecution(executionId: number): Observable<StartJobResponse> {
    return this.backend.post<StartJobResponse>(`/platform/ops/batch/executions/${executionId}:restart`, {});
  }

  // Batch Gates
  listGates(jobKeys?: string[], suppressShellFeedback = false): Observable<Record<string, boolean>> {
    const q = jobKeys?.length ? `?job_keys=${jobKeys.join(',')}` : '';
    return this.backend.get<Record<string, boolean>>(
      `/platform/ops/batch/gates/effective${q}`,
      suppressShellFeedback ? { suppressShellFeedback: true } : undefined,
    );
  }

  updateGate(jobKey: string, req: GateUpdateRequest): Observable<void> {
    return this.backend.put<void>(`/platform/ops/batch/gates/${jobKey}`, req);
  }

  // Draw Calendar
  generateDraws(req: GenerateDrawsRequest): Observable<OpsLaunchResponse> {
    return this.backend.post<OpsLaunchResponse>('/platform/ops/draws/generate', req);
  }

  openTodayDraws(req: OpenTodayDrawsRequest): Observable<OpsLaunchResponse> {
    return this.backend.post<OpsLaunchResponse>('/platform/ops/draws/open-today', req);
  }

  closeDueDraws(req: CloseDueDrawsRequest): Observable<OpsLaunchResponse> {
    return this.backend.post<OpsLaunchResponse>('/platform/ops/draws/close-due', req);
  }

  applyDrawResults(req: ApplyExternalResultsRequest): Observable<OpsLaunchResponse> {
    return this.backend.post<OpsLaunchResponse>('/platform/ops/draws/apply', req);
  }

  // Draw Results
  listDrawResults(params?: {
    slotKey?: string;
    status?: string;
    quality?: OpsDrawResultQuality | '';
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

  fetchDrawResults(req: FetchExternalResultsRequest): Observable<OpsLaunchResponse> {
    return this.backend.post<OpsLaunchResponse>('/platform/ops/draw-results/fetch', req);
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
  listDraws(
    params: {
      status?: string;
      from?: string;
      to?: string;
      resultSlotKey?: string;
      page?: number;
      size?: number;
      deletedVisibility?: 'active' | 'deleted' | 'all';
      suppressShellFeedback?: boolean;
    },
    tenantId?: string | null,
  ): Observable<TchPage<DrawView>> {
    const { deletedVisibility, suppressShellFeedback, ...queryParams } = params;
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(queryParams)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPage<DrawView>>(
      `/admin/draws${q ? '?' + q : ''}`,
      drawListOptions(tenantId, deletedVisibility, suppressShellFeedback),
    );
  }

  /** @deprecated Use listDraws */
  listDrawsForLifecycle(params: { status?: string; page?: number; size?: number }): Observable<TchPage<DrawView>> {
    return this.listDraws(params);
  }

  cancelDraw(drawId: string, req: CancelDrawRequest, tenantId?: string | null): Observable<DrawView> {
    return this.cancelDraws({ ...req, drawIds: [drawId] }, tenantId).pipe(map(rows => rows[0]));
  }

  cancelDraws(req: CancelDrawsRequest, tenantId?: string | null): Observable<DrawView[]> {
    return this.backend.post<DrawView[]>('/admin/draws/lifecycle/cancel', req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: cancel draws'));
  }

  lockDraw(drawId: string, reason?: string, tenantId?: string | null): Observable<DrawView> {
    return this.lockDraws({ drawIds: [drawId], reason }, tenantId).pipe(map(rows => rows[0]));
  }

  lockDraws(req: LifecycleDrawsRequest, tenantId?: string | null): Observable<DrawView[]> {
    return this.backend.post<DrawView[]>('/admin/draws/lifecycle/lock', req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: lock draws'));
  }

  unlockDraw(drawId: string, reason?: string, tenantId?: string | null): Observable<DrawView> {
    return this.unlockDraws({ drawIds: [drawId], reason }, tenantId).pipe(map(rows => rows[0]));
  }

  unlockDraws(req: LifecycleDrawsRequest, tenantId?: string | null): Observable<DrawView[]> {
    return this.backend.post<DrawView[]>('/admin/draws/lifecycle/unlock', req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: unlock draws'));
  }

  settleDraw(drawId: string, reason?: string, tenantId?: string | null): Observable<DrawView> {
    return this.settleDraws({ drawIds: [drawId], reason }, tenantId).pipe(map(rows => rows[0]));
  }

  settleDraws(req: LifecycleDrawsRequest, tenantId?: string | null): Observable<DrawView[]> {
    return this.backend.post<DrawView[]>('/admin/draws/lifecycle/settle', req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: settle draws'));
  }

  archiveDraw(drawId: string, reason?: string, force?: boolean, tenantId?: string | null): Observable<DrawView> {
    return this.archiveDraws({ drawIds: [drawId], reason, force }, tenantId).pipe(map(rows => rows[0]));
  }

  archiveDraws(req: LifecycleDrawsRequest, tenantId?: string | null): Observable<DrawView[]> {
    return this.backend.post<DrawView[]>('/admin/draws/lifecycle/archive', req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: archive draws'));
  }

  rescheduleDraw(drawId: string, scheduledAt: string, cutoffAt: string, reason: string, force?: boolean, tenantId?: string | null): Observable<DrawView> {
    return this.backend.post<DrawView>(`/admin/draws/${drawId}/reschedule`, { scheduledAt, cutoffAt, reason, force }, tenantAdminOptions(tenantId, 'SUPER_ADMIN: reschedule draw'));
  }

  correctDrawResult(drawId: string, req: CorrectDrawResultRequest, tenantId?: string | null): Observable<DrawView> {
    return this.backend.post<DrawView>(`/admin/draws/${drawId}/results/correct`, req, tenantAdminOptions(tenantId, 'SUPER_ADMIN: correct draw result'));
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

  clearCacheGroup(group: string, reason: string): Observable<CacheGroupClearResult> {
    return this.backend.delete<CacheGroupClearResult>(`/platform/ops/cache/groups/${group}`, { params: { reason } });
  }
}

function tenantAdminOptions(tenantId: string | null | undefined, reason: string): TchRequestOptions | undefined {
  return tenantId ? { asTenantAdmin: { tenantId, reason } } : undefined;
}

function drawListOptions(
  tenantId: string | null | undefined,
  deletedVisibility: 'active' | 'deleted' | 'all' | null | undefined,
  suppressShellFeedback = false,
): TchRequestOptions | undefined {
  const options = tenantAdminOptions(tenantId, 'SUPER_ADMIN: list draws');
  if (!deletedVisibility || deletedVisibility === 'active') {
    return suppressShellFeedback ? { ...(options ?? {}), suppressShellFeedback: true } : options;
  }
  return {
    ...(options ?? {}),
    headers: { 'X-Deleted-Visibility': deletedVisibility },
    ...(suppressShellFeedback ? { suppressShellFeedback: true } : {}),
  };
}
