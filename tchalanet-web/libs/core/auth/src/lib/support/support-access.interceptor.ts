import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { SupportAccessStore } from './support-access.store';

export const supportAccessInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(SupportAccessStore);
  const session = store.session();

  if (!session || req.headers.has('X-Tch-Tenant-Override') || !isTenantAdminApi(req.url)) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: {
      'X-Tch-Tenant-Override': session.tenantId,
      'X-Tch-Act-As': 'TENANT_ADMIN',
      'X-Tch-Override-Reason': `SUPER_ADMIN support session ${session.sessionId}`,
    },
  }));
};

function isTenantAdminApi(url: string): boolean {
  return /\/admin(\/|$)/.test(url);
}
