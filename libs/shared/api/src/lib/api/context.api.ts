import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map, Observable } from 'rxjs';

export interface BootstrapDto {
  context: { tenant: string; role: string; locale: string; /* ... */ };
  features: string[];
  nav: { header: any[]; sidenav: any[]; version: string };
  kpis?: Record<string, unknown>;
  layout?: any;
}

@Injectable({ providedIn: 'root' })
export class ContextApi {
  private http = inject(HttpClient);
  private oidc = inject(OidcSecurityService);

  /** Complete the OIDC redirect by reading ?code and storing tokens. */
  checkAuth() {
    return this.oidc.checkAuth();
  }

  /** Ask backend for the UI context (tenant/role/features/nav/...) */
  bootstrap(): Observable<BootstrapDto> {
    // backend can infer tenant/role from token; or optionally accept overrides
    return this.http.get<BootstrapDto>('/api/v1/console/bootstrap');
  }
}
