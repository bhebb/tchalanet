import { TCH_I18N_ASSETS } from '@tch/shared-assets';

export const API_PATHS = {
  i18n: {
    public: '/api/v1/public/i18n',
  },
  settings: {
    public: '/api/v1/public/settings',
    tenantResolve: '/api/v1/tenant/settings/resolve',
  },
} as const;

// Backend translations are delivered inside the runtime bootstrap response and overlaid via
// TranslateService.setTranslation; the loader only serves local fallback bundles.
export const PORTAL_I18N_CONFIG = {
  fallbackLang: 'fr',
  defaultLang: 'fr',
  assetsPrefix: `${TCH_I18N_ASSETS.basePath}/`,
  assetsSuffix: '.json',
  bundles: [
    'common',
    'errors',
    'domain',
    'component',
    'surface-public',
    'surface-admin',
    'surface-platform',
    'surface-seller-terminal',
    'feature-auth',
    'feature-public',
    'feature-admin',
    'feature-platform',
    'feature-seller-terminal',
  ],
} as const;

export const AUTH_CONFIG = {
  realm: 'tchalanet',
  clientId: 'tchalanet-web',
  // Must match KC_HOSTNAME exactly (dev includes the external port :8443) —
  // keycloak-js validates the iss callback parameter against this URL.
  localUrl: 'https://auth.localtest.me:8443',
  lanUrl: 'https://auth.tchalanet.lan',
} as const;

// Bearer token is attached only to non-public API calls: /api/v1/public/** must stay
// anonymous even when a Keycloak session exists (public pages + token = no Authorization).
export const APPLICATION_API_URL_PATTERN =
  /^(?:https?:\/\/(?:(?:localhost|127\.0\.0\.1):8083|api\.(?:localtest\.me|tchalanet\.lan)))?\/api\/(?!v1\/public\/)/i;

export function keycloakUrlForHostname(hostname: string): string {
  if (hostname.endsWith('tchalanet.lan')) return AUTH_CONFIG.lanUrl;
  // Static/preview deployments (.vercel.app): point KC at the app origin.
  // The Vercel routing config intercepts /realms/* before the SPA rewrite and
  // returns 404 → KC init rejects immediately, no iframe loop, no timeout.
  if (hostname.endsWith('.vercel.app')) return `https://${hostname}`;
  return AUTH_CONFIG.localUrl;
}
