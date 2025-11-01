export const environment = {
  apiBase: 'https://api.tchalanet.com/api',
  authUrl: 'https://sso.tchalanet.com/realms/tchalanet',
  authClientId: 'tchalanet-web',
  apiVersion: 'v1',
  appVersion: '1',
  errorVersion: '1',
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
  feature: {
    kind: 'unleash',
    url: 'http://localhost:3063/api/frontend',
    clientKey: 'default:development.97e1e48d98f7d191e1dc530152eb146bce6f8182a6d26762f688eeec',
    appName: 'tchalanet-web',
    environment: 'development',
    refresh: 10,
    defaultValue: false,
  },
  tenant: 'public',
  lang: 'fr',
};
