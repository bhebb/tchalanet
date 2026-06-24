import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPage } from '../../platform-ops-api.service';

export type AuditActorType = 'USER' | 'TERMINAL' | 'SYSTEM';

export type AuditEntityType =
  | 'SYSTEM' | 'TENANT' | 'PLAN' | 'SUBSCRIPTION' | 'THEME'
  | 'USER' | 'USER_PREFERENCE' | 'GAME' | 'DRAW' | 'DRAW_RESULT'
  | 'DRAW_CHANNEL' | 'RESULT_SLOT' | 'ODDS' | 'LIMIT_POLICY'
  | 'OUTLET' | 'TERMINAL' | 'SELLER_TERMINAL' | 'TICKET' | 'TICKET_LINE'
  | 'PAYOUT' | 'PAYMENT' | 'FEATURE_FLAG' | 'BATCH_JOB' | 'CACHE' | 'PUBLIC_CONTENT';

export interface AuditEventView {
  id: string;
  tenantId: string | null;
  occurredAt: string;
  actorType: AuditActorType;
  actorId: string | null;
  entityType: AuditEntityType;
  entityId: string;
  action: string;
  details: string | null;
  ip: string | null;
  userAgent: string | null;
}

export interface PurgeAuditResult {
  deleted: number;
  retentionDays: number;
  threshold: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformAuditApi {
  private readonly backend = inject(TchBackendClient);

  listAuditEvents(params: {
    tenantId?: string;
    entityType?: AuditEntityType;
    entityId?: string;
    action?: string;
    actorId?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<TchPage<AuditEventView>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPage<AuditEventView>>(`/platform/audit/logs${q ? '?' + q : ''}`);
  }

  purge(): Observable<PurgeAuditResult> {
    return this.backend.post<PurgeAuditResult>('/platform/audit/purge', {});
  }
}
