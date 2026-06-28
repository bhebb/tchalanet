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

export interface PartitionCleanupPlan {
  partitionName: string;
  tableName: string;
  periodStart: string;
  periodEnd: string;
  hotRowCount: number;
  archivedRowCount: number;
  archiveVerified: boolean;
  eligible: boolean;
  ineligibleReason: string | null;
}

export interface RestoreAuditLogRequest {
  tenantId?: string;
  entityType: string;
  entityId: string;
  from: string;
  to: string;
  reason: string;
}

export type ArchivePurgeMode = 'DRY_RUN' | 'DELETE';
export type ArchiveDomainPurgeDataset = 'DRAW' | 'DRAW_RESULT' | 'ENTITY_REVISION';

export interface TicketPurgeRequest {
  tenantId?: string;
  periodStart: string;
  periodEnd: string;
  batchSize?: number;
  mode?: ArchivePurgeMode;
  reason: string;
}

export interface DomainPurgeRequest extends TicketPurgeRequest {
  dataset: ArchiveDomainPurgeDataset;
}

export interface ArchivePurgePlan {
  dataset?: string;
  tenantId: string | null;
  periodStart: string;
  periodEnd: string;
  hotRows?: number;
  archivedRows?: number;
  blockingRows?: number;
  hotTickets?: number;
  hotLines?: number;
  hotCharges?: number;
  archivedTickets?: number;
  archivedLines?: number;
  archivedCharges?: number;
  eligible: boolean;
  ineligibleReason: string | null;
}

export interface ArchivePurgeResult {
  mode: ArchivePurgeMode;
  plan: ArchivePurgePlan;
  deletedChildRows?: number;
  deletedRows?: number;
  deletedCharges?: number;
  deletedLines?: number;
  deletedTickets?: number;
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

  listActiveLegalHolds(limit = 50): Observable<Record<string, unknown>[]> {
    return this.backend.get<Record<string, unknown>[]>(`/platform/archive/legal-holds/active?limit=${limit}`);
  }

  getPartitionCleanupPlan(tableName: string, retentionCutoff: string): Observable<PartitionCleanupPlan[]> {
    return this.backend.get<PartitionCleanupPlan[]>(
      `/platform/archive/partition-cleanup/plan?tableName=${encodeURIComponent(tableName)}&retentionCutoff=${retentionCutoff}`,
    );
  }

  getOpsSummary(): Observable<ArchiveOpsSummary> {
    return this.backend.get<ArchiveOpsSummary>('/platform/archive/ops-summary');
  }

  restoreAuditLog(req: RestoreAuditLogRequest): Observable<string> {
    return this.backend.post<string>('/platform/archive/restore/audit-log', req);
  }

  purgeTickets(req: TicketPurgeRequest): Observable<ArchivePurgeResult> {
    return this.backend.post<ArchivePurgeResult>('/platform/archive/ticket-purge', req);
  }

  purgeDomain(req: DomainPurgeRequest): Observable<ArchivePurgeResult> {
    return this.backend.post<ArchivePurgeResult>('/platform/archive/domain-purge', req);
  }
}
