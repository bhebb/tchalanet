import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface TenantRuntimeView {
  code: string;
  name: string;
  status: string;
  timezone: string;
  currency: string;
  defaultLanguage?: string | null;
  defaultLocale?: string | null;
  supportedLocales: string[];
}

@Injectable({ providedIn: 'root' })
export class RuntimeApiService {
  private readonly backend = inject(TchBackendClient);

  getTenantRuntime(): Observable<TenantRuntimeView> {
    return this.backend.get<TenantRuntimeView>('/tenant/runtime');
  }

  getPublicTenantRuntime(): Observable<TenantRuntimeView> {
    return this.backend.get<TenantRuntimeView>('/public/tenant/runtime');
  }
}
