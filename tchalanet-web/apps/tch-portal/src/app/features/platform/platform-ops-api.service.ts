import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export const VALID_JOB_KEYS = [
  'draw:lifecycle:generate',
  'draw:lifecycle:open',
  'draw:lifecycle:close',
  'draw:lifecycle:settle',
  'results:external:refresh',
  'results:external:fetch',
  'results:external:apply',
  'results:external:manual',
  'results:external:override',
  'catalog:search:reindex',
] as const;

export type JobKey = (typeof VALID_JOB_KEYS)[number];

export interface BatchJobView {
  jobKey: JobKey;
  status: string;
  lastRunAt?: string;
  nextRunAt?: string;
}

export interface BatchGateView {
  jobKey: JobKey;
  enabled: boolean;
  reason?: string;
}

export interface BatchExecutionView {
  executionId: string;
  jobKey: JobKey;
  status: string;
  startedAt: string;
  endedAt?: string;
  tenantCode?: string;
  dryRun: boolean;
}

export interface StartJobRequest {
  jobKey: JobKey;
  tenantCode?: string;
  dryRun?: boolean;
  force?: boolean;
  reason?: string;
  parameters?: Record<string, unknown>;
}

export interface DrawOperationRequest {
  dryRun?: boolean;
  force?: boolean;
  reason?: string;
}

export interface DrawResultView {
  drawResultId: string;
  drawId: string;
  slotCode: string;
  status: string;
  fetchedAt?: string;
  confirmedAt?: string;
}

export interface ManualResultRequest {
  drawId: string;
  slotCode: string;
  numbers: number[];
  reason: string;
}

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

export interface TchPageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CacheView {
  cacheName: string;
  size: number;
  hitRate?: number;
  lastClearedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformOpsApi {
  private readonly backend = inject(TchBackendClient);

  listJobs(): Observable<BatchJobView[]> {
    return this.backend.get<BatchJobView[]>('/platform/ops/batch/jobs');
  }

  startJob(jobKey: JobKey, req: StartJobRequest): Observable<void> {
    return this.backend.post<void>(`/platform/ops/batch/jobs/${jobKey}:start`, req);
  }

  listGates(): Observable<BatchGateView[]> {
    return this.backend.get<BatchGateView[]>('/platform/ops/batch/gates:effective');
  }

  updateGate(jobKey: JobKey, enabled: boolean): Observable<void> {
    return this.backend.put<void>(`/platform/ops/batch/gates/${jobKey}`, { enabled });
  }

  listExecutions(params?: { jobKey?: string; limit?: number }): Observable<BatchExecutionView[]> {
    const query = params
      ? `?${new URLSearchParams(
          Object.fromEntries(
            Object.entries(params)
              .filter(([, v]) => v !== undefined)
              .map(([k, v]) => [k, String(v)]),
          ),
        ).toString()}`
      : '';
    return this.backend.get<BatchExecutionView[]>(`/platform/ops/batch/executions${query}`);
  }

  generateDraws(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draws/generate', req);
  }

  openTodayDraws(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draws/open-today', req);
  }

  closeDueDraws(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draws/close-due', req);
  }

  applyDrawResults(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draws/apply', req);
  }

  listDrawResults(): Observable<DrawResultView[]> {
    return this.backend.get<DrawResultView[]>('/platform/ops/draw-results');
  }

  fetchDrawResults(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draw-results/fetch', req);
  }

  refreshDrawResults(req: DrawOperationRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draw-results/refresh', req);
  }

  manualDrawResult(req: ManualResultRequest): Observable<void> {
    return this.backend.post<void>('/platform/ops/draw-results/manual', req);
  }

  confirmDrawResult(drawResultId: string, reason: string): Observable<void> {
    return this.backend.post<void>(`/platform/ops/draw-results/${drawResultId}/confirm`, {
      reason,
    });
  }

  listDrawsForLifecycle(params: { status?: string; page?: number; size?: number }): Observable<TchPageResult<DrawSummaryResponse>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPageResult<DrawSummaryResponse>>(`/admin/draws${q ? '?' + q : ''}`);
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
