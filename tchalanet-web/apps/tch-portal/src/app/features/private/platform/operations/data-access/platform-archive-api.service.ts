import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface ArchiveRunView {
  id: string;
  status: string;
  strategy: string;
  triggerType: string;
  idempotencyKey: string | null;
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface TriggerArchiveRunRequest {
  strategy: string;
  periodStart: string;
  periodEnd: string;
  reason: string;
}

export interface ArchiveOpsSummary {
  failedRuns: number;
  startedRuns: number;
  completedRuns: number;
  invalidObjects: number;
  verifiedObjects: number;
  pendingObjects: number;
}

export interface RestoreAuditLogRequest {
  tenantId?: string;
  entityType: string;
  entityId: string;
  from: string;
  to: string;
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformArchiveApi {
  private readonly backend = inject(TchBackendClient);

  listRuns(limit = 50): Observable<ArchiveRunView[]> {
    return this.backend.get<ArchiveRunView[]>(`/platform/archive/runs?limit=${limit}`);
  }

  triggerRun(req: TriggerArchiveRunRequest): Observable<ArchiveRunView> {
    return this.backend.post<ArchiveRunView>('/platform/archive/runs', req);
  }

  listFailedRuns(limit = 20): Observable<Record<string, unknown>[]> {
    return this.backend.get<Record<string, unknown>[]>(`/platform/archive/runs/failed?limit=${limit}`);
  }

  listInvalidObjects(limit = 20): Observable<Record<string, unknown>[]> {
    return this.backend.get<Record<string, unknown>[]>(`/platform/archive/objects/invalid?limit=${limit}`);
  }

  getOpsSummary(): Observable<ArchiveOpsSummary> {
    return this.backend.get<ArchiveOpsSummary>('/platform/archive/ops-summary');
  }

  restoreAuditLog(req: RestoreAuditLogRequest): Observable<string> {
    return this.backend.post<string>('/platform/archive/restore/audit-log', req);
  }
}
