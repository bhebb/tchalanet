import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { selectTenantId } from '@tchl/data-access/session';
import { Store } from '@ngrx/store';
import { isKeycloakUrl } from '@tchl/api';
import { AuthService } from '@tchl/shared/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/assets/')) return next(req);
  // Skip Keycloak
  if (isKeycloakUrl(req.url)) return next(req);

  // Ne touche pas aux URLs absolues déjà complètes
  // if (isAbsoluteUrl(req.url)) return next(req);
  const store = inject(Store);
  const auth = inject(AuthService);
  const token = auth.accessToken();

  if (!token) return next(req);
  const tenantId = store.selectSignal(selectTenantId)();
  let headers = req.headers;
  if (token) headers = headers.set('Authorization', `Bearer ${token}`);
  if (tenantId && !headers.has('X-Tenant-Id')) headers = headers.set('X-Tenant-Id', tenantId);
  return next(req.clone({ headers }));
};
