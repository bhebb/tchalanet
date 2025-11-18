import { environment } from '@tchl/config';

export const PROXY_OIDC_PREFIX = '/keycloak';

export function isKeycloakUrl(url: string): boolean {
  // Vérifier si l'URL commence par l'authUrl de l'environnement
  if (url.startsWith(environment.authUrl)) return true;

  // appels via proxy dev: /keycloak/realms/...
  if (url.startsWith(`${PROXY_OIDC_PREFIX}/realms/`)) return true;

  // garde-fou: si l'URL contient /realms/.../protocol/openid-connect
  if (url.includes('/realms/') && url.includes('/protocol/openid-connect')) return true;

  // Détection des domaines Keycloak connus
  if (url.includes('auth.localtest.me') || url.includes('auth.stg.tchalanet.com') || url.includes('auth.tchalanet.com')) return true;

  return false;
}

export function isAbsoluteUrl(url: string): boolean {
  return /^https?:\/\//i.test(url);
}
