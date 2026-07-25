import { Injectable, ResourceRef, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface TenantRuntimeView {
  tenantCode: string;
  displayName: string;
  statusPublic: string;
  timezone: string;
  currency: string;
  defaultLanguage?: string | null;
  defaultLocale?: string | null;
  supportedLocales: string[];
}

@Injectable({ providedIn: 'root' })
export class RuntimeApiService {
  private readonly backend = inject(TchBackendClient);

  getTenantRuntime(options?: TchRequestOptions): Observable<TenantRuntimeView> {
    return this.backend.get<TenantRuntimeView>('/tenant/runtime', options);
  }

  tenantRuntimeResource(): ResourceRef<TenantRuntimeView | undefined> {
    return this.backend.getResource<TenantRuntimeView>(() => ({
      path: '/tenant/runtime',
      options: { suppressShellFeedback: true },
    }));
  }

  getPublicTenantRuntime(options?: TchRequestOptions): Observable<TenantRuntimeView> {
    return this.backend.get<TenantRuntimeView>('/public/tenant/runtime', options);
  }
}
