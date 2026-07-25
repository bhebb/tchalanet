import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TenantCommercialConfigApiService {
  private readonly backend = inject(TchBackendClient);

  getCommissionOverview(
    options?: TchRequestOptions,
  ): Observable<{ tenantDefaultRate: number | null }> {
    return this.backend.get<{ tenantDefaultRate: number | null }>(
      '/admin/commission/overview',
      options,
    );
  }

  updateDefaultCommissionRate(rate: number, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/commission/default-rate', { rate }, options);
  }
}
