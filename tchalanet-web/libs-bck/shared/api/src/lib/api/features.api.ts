import { map,Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface FeaturesPayload { tenant: string; role: string; features: string[]; }

@Injectable({ providedIn: 'root' })
export class FeaturesApi {
  private http = inject(HttpClient);
  get(tenant: string, role: string): Observable<string[]> {
    return this.http.get<FeaturesPayload>(`/api/v1/dashboards/tenant/features`, { params: { tenant, role } })
      .pipe(map(r => r.features ?? []));
  }
}
