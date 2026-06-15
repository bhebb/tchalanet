// ─── API Base URL ──────────────────────────────────────────────────────────
// Local options (pass via --dart-define=API_BASE_URL=...):
//   Android emulator (default) : https://api.localtest.me:8443/api/v1
//   macOS / Chrome             : https://api.localtest.me/api/v1
//
// Requires: adb reverse tcp:8443 tcp:8443 for local backend access.
const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'https://api.localtest.me:8443/api/v1',
);

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
