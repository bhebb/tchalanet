import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '@tchl/config';
import { isAbsoluteUrl, isKeycloakUrl } from '../utils/http.utils';

export const apiBaseInterceptor: HttpInterceptorFn = (req, next) => {
  // Laisse les assets (ex: /assets/i18n/fr.json)
  if (req.url.startsWith('/assets/')) return next(req);
  // Skip Keycloak
  if (isKeycloakUrl(req.url)) return next(req);

  // Ne touche pas aux URLs absolues déjà complètes
  // if (isAbsoluteUrl(req.url)) return next(req);

  // Préfixe la base API absolue
  const url = `${environment.apiBase}` ? `${environment.apiBase}${req.url}` : req.url;
  return next(req.clone({ url }));
};
