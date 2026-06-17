import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TenantConfigApiService {
  private readonly backend = inject(TchBackendClient);

  getTenantConfig(): Observable<unknown> {
    return this.backend.get<unknown>('/admin/tenant-config');
  }

  updateInternalSettings(req: unknown): Observable<void> {
    return this.backend.put<void>('/admin/tenant-config/internal-settings', req);
  }

  getCommunicationConfig(): Observable<unknown> {
    return this.backend.get<unknown>('/admin/tenant-config/communication');
  }

  getDocumentConfig(): Observable<unknown> {
    return this.backend.get<unknown>('/admin/tenant-config/document');
  }
}
