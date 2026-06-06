export const API_PATHS = {
    i18n: {
        public: '/api/v1/public/i18n',
    },
    settings: {
        public: '/api/v1/public/settings',
        tenantResolve: '/api/v1/tenant/settings/resolve',
    },
} as const;

export const PORTAL_I18N_CONFIG = {
    fallbackLang: 'fr',
    defaultLang: 'fr',
    assetsPrefix: '/assets/i18n/',
    assetsSuffix: '.json',
    backendPath: API_PATHS.i18n.public,
    surfaces: ['PUBLIC_RESULTS',
        'PUBLIC_TICKET_CHECK',
        'COMMON_PUBLIC_ERROR',
        'PUBLIC_HOME'],
} as const;

export const AUTH_CONFIG = {
    realm: 'tchalanet',
    clientId: 'tchalanet-web',
    localUrl: 'https://auth.localtest.me',
    lanUrl: 'https://auth.tchalanet.lan',
} as const;

export const APPLICATION_API_URL_PATTERN =
    /^(\/api\/|https?:\/\/(localhost|127\.0\.0\.1):8083\/api\/|https?:\/\/api\.(localtest\.me|tchalanet\.lan)\/api\/)/i;

export function keycloakUrlForHostname(hostname: string): string {
    return hostname.endsWith('tchalanet.lan') ? AUTH_CONFIG.lanUrl : AUTH_CONFIG.localUrl;
}
