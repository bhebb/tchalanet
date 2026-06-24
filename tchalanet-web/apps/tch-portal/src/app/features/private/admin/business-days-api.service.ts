import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type BusinessDayStatus = 'OPEN' | 'CLOSED';

export interface BusinessDayView {
  id: string;
  date: string;
  status: BusinessDayStatus;
  reason?: string;
  appliesToTenant: boolean;
}

export interface UpsertBusinessDayRequest {
  date: string;
  status: BusinessDayStatus;
  reason?: string;
  appliesToTenant?: boolean;
}

@Injectable({ providedIn: 'root' })
export class BusinessDaysApiService {
  private readonly backend = inject(TchBackendClient);

  listBusinessDays(params?: { from?: string; to?: string }): Observable<BusinessDayView[]> {
    if (!params || (!params.from && !params.to)) {
      return this.backend.get<BusinessDayView[]>('/admin/business-days');
    }
    const query = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][],
      ),
    ).toString();
    return this.backend.get<BusinessDayView[]>(`/admin/business-days?${query}`);
  }

  upsertBusinessDay(req: UpsertBusinessDayRequest): Observable<void> {
    return this.backend.put<void>('/admin/business-days', req);
  }

  deleteBusinessDay(id: string): Observable<void> {
    return this.backend.delete<void>(`/admin/business-days/${id}`);
  }
}
