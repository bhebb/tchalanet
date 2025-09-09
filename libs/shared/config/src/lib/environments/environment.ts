export const environment = {
  apiBase: import.meta.env.VITE_API_BASE ?? '',
  authUrl: import.meta.env.VITE_AUTH_URL ?? '',
  authClientId: import.meta.env.VITE_AUTH_CLIENT_ID ?? '',
  apiVersion: import.meta.env.VITE_API_VERSION ?? 'v1',
  appVersion: import.meta.env.VITE_APP_VERSION ?? 'dev',
  errorVersion: Number(import.meta.env.VITE_ERROR_VERSION ?? '1'),
  // Mode: 'direct' (InstantSearch → Meili) | 'proxy' (Http → /api/search)
  searchMode: 'direct' as 'direct' | 'proxy',

  // Meili (dev)
  meiliHost: 'http://localhost:7700',
  meiliSearchKey: 'f34ffde28a35b523143f340ec614180a5dbbdec7675ca6d05395029554f2e3bb',
  indexName: 'tch_content',

  // Proxy (prod ou dev sécurisé)
  apiBaseUrl: 'http://localhost:8081',

  umami: {
    host: 'http://localhost:3300',
    websiteId: '004d636a-8a5a-41ec-83c1-1af7259ce49b',
  },

  tenant: 'public',
  lang: 'fr',
};
