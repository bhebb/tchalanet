import { Injectable, inject } from '@angular/core';
import { EntityHistoryType, EntityRevisionItem, TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PlatformEntityHistoryApi {
  private readonly backend = inject(TchBackendClient);

  listRevisions(params: {
    entityType: EntityHistoryType;
    entityId: string;
    page?: number;
    size?: number;
  }): Observable<TchPage<EntityRevisionItem>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPage<EntityRevisionItem>>(
      `/platform/entity-history/revisions${q ? '?' + q : ''}`,
    );
  }
}
