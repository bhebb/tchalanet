// ─── API Base URL ──────────────────────────────────────────────────────────
// Local options (pass via --dart-define=API_BASE_URL=...):
//   Android emulator (default) : https://api.localtest.me:8443/api/v1
//   macOS / Chrome             : https://api.localtest.me/api/v1
//
// Requires: adb reverse tcp:8443 tcp:8443 (same tunnel as Keycloak)
const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'https://api.localtest.me:8443/api/v1',
);

// ─── Keycloak ──────────────────────────────────────────────────────────────
// AppAuth Android SDK enforces HTTPS — HTTP is rejected at the native level.
//
// Emulator dev workflow (run once per adb session, port 8443 is non-privileged):
//   adb reverse tcp:8443 tcp:8443
//   flutter run --dart-define=POS_DEVICE=true
//
// Options:
//   Android emulator (default) : https://auth.localtest.me:8443
//   macOS / Chrome             : https://auth.localtest.me
const kcBaseUrl = String.fromEnvironment(
  'KC_BASE_URL',
  defaultValue: 'https://auth.localtest.me:8443',
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

// ─── Device binding ──────────────────────────────────────────────────────────
// Sent as X-Device-Binding on authenticated API calls. The backend recomputes
// SHA256(tenantId|terminalId|<binding>) and compares it to the seeded
// terminal_binding.credential_hash — a match upgrades the operational-context
// trust from CLIENT_CLAIM (WEAK) to STRONG, which is required for selling.
//
// Dev default matches the e2e seed (V205/V211). In prod, pass an empty value
// (--dart-define=POS_DEVICE_BINDING=) until the real terminal-activation flow
// (T12) provides the per-device credential.
const posDeviceBinding = String.fromEnvironment(
  'POS_DEVICE_BINDING',
  defaultValue: 'e2e-cred-dev',
);
