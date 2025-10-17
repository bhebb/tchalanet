export const OIDC_BASE = 'http://localhost:8080/realms/tchalanet'; // ou depuis env
export const API_BASE = 'http://localhost:8082'; // ton BFF en local
export const PROXY_OIDC_PREFIX = '/keycloak'; // si tu utilises un proxy dev

export function isKeycloakUrl(url: string): boolean {
  // appels direct KC (découverte, auth, token, userinfo, jwks, logout)
  if (url.startsWith(`${OIDC_BASE}/`)) return true;
  // appels via proxy dev: /keycloak/realms/tchalanet/...
  if (url.startsWith(`${PROXY_OIDC_PREFIX}/realms/tchalanet/`)) return true;
  // garde-fou: si l’URL contient /realms/tchalanet/protocol/openid-connect
  return url.includes('/realms/tchalanet/protocol/openid-connect');
}

export function isAbsoluteUrl(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

export function isApiUrl(url: string): boolean {
  // selon ton design : toutes les routes non KC destinées au BFF
  return url.startsWith(API_BASE) || (!isAbsoluteUrl(url) && !url.startsWith(PROXY_OIDC_PREFIX));
}
