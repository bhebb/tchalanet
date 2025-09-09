import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageModel } from '@tchl/types';
import { AuthService } from '@tchl/shared/auth';

@Injectable({ providedIn: 'root' })
export class PageApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  getPublic() {
    return this.http.get<PageModel>('/v1/pages/home-public');
  }

  getPrivate(opts?: { tenantId?: string; xTenantIdHeader?: string }) {
    const params = opts?.tenantId ? { tenantId: opts.tenantId } : undefined;
    const headers = opts?.xTenantIdHeader ? { 'X-Tenant-Id': opts.xTenantIdHeader } : undefined;
    return this.http.get<PageModel>('/v1/pages/private', { params, headers });
  }

  getPage(context: any, tenantId: any) {
    if (this.auth.isAuthenticated()) {
      return this.getPrivate({ tenantId });
    } else {
      return this.getPublic();
    }
  }
}
