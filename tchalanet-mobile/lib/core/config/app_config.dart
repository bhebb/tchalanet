// ─── API Base URL ──────────────────────────────────────────────────────────
// Local options (pass via --dart-define=API_BASE_URL=...):
//   Android emulator : http://10.0.2.2:8083/api/v1
//   macOS / Chrome   : http://localhost:8083/api/v1
//   Traefik (HTTPS)  : https://api.localtest.me/api/v1
//   LAN hostname     : http://api.tchalanet.lan/api/v1
const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8083/api/v1',
);

// ─── Keycloak ──────────────────────────────────────────────────────────────
// Local options:
//   Android emulator : http://10.0.2.2:8082
//   macOS / Chrome   : https://auth.localtest.me
const kcBaseUrl = String.fromEnvironment(
  'KC_BASE_URL',
  defaultValue: 'https://auth.localtest.me',
);

const kcRealm = String.fromEnvironment(
  'KC_REALM',
  defaultValue: 'tchalanet',
);

const kcClientId = String.fromEnvironment(
  'KC_CLIENT_ID',
  defaultValue: 'tchalanet-mobile-pos',
);

const kcRedirectUri = 'com.tchalanet.mobile:/oauth2redirect';
