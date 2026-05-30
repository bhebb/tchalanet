// libs/shared/data-access/kpis/kpis.api.ts
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface KpisDto { salesToday: number; tickets: number; payoutPending: number; margin: number; }

@Injectable({ providedIn: 'root' })
export class KpisApi {
  private http = inject(HttpClient);
  get(tenant: string, role: string): Observable<KpisDto> {
    return this.http.get<KpisDto>('/api/v1/kpis/console', { params: { tenant, role } });
  }
}
