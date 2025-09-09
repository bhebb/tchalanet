import { HttpHeaders, HttpInterceptorFn } from '@angular/common/http';
import { environment } from '@tchl/config';
import { isAbsoluteUrl, isKeycloakUrl } from '../utils/http.utils';

export const metaHeadersInterceptor: HttpInterceptorFn = (req, next) => {
  // Laisse assets
  if (req.url.startsWith('/assets/')) return next(req);
  // Skip Keycloak
  if (isKeycloakUrl(req.url)) return next(req);

  // Ne touche pas aux URLs absolues déjà complètes
  // if (isAbsoluteUrl(req.url)) return next(req);
  const headers = (req.headers || new HttpHeaders())
    .set('X-Api-Version', `${environment.apiVersion}`)
    .set('X-App-Version', `${environment.appVersion}`)
    .set('X-Error-Version', `${environment.errorVersion}`);

  return next(req.clone({ headers }));
};
