import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '@tchl/config';

import { isKeycloakUrl } from '../utils/http.utils';

export const apiBaseInterceptor: HttpInterceptorFn = (req, next) => {
  // Laisse les assets (ex: /assets/i18n/fr.json)
  if (req.url.includes('assets/')) return next(req);

  // Skip Keycloak URLs
  if (isKeycloakUrl(req.url)) return next(req);

  // Ne touche pas aux URLs absolues déjà complètes (http:// ou https://)
  if (/^https?:\/\//i.test(req.url)) return next(req);

  // Préfixe la base API pour les URLs relatives uniquement
  const url = environment.apiBase ? `${environment.apiBase}${req.url}` : req.url;
  return next(req.clone({ url }));
};
