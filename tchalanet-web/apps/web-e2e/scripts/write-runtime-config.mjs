import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = join(__dirname, '..', '..', '..');
const configDir = join(workspaceRoot, 'libs', 'shared-assets', 'public', 'assets', 'config');

const apiBaseUrl = required('WEB_E2E_API_BASE_URL');
const firebaseAuthEmulatorUrl = optional('WEB_E2E_FIREBASE_AUTH_EMULATOR_URL');
const publicBaseUrl = optional('PUBLIC_BASE_URL') ?? 'http://localhost:4301';
const adminBaseUrl = optional('ADMIN_BASE_URL') ?? 'http://localhost:4302';
const platformBaseUrl = optional('PLATFORM_BASE_URL') ?? 'http://localhost:4303';

const baseConfig = {
  appId: '',
  production: false,
  apiBaseUrl,
  authBaseUrl: null,
  assetsBaseUrl: '/',
  firebaseAuthEmulatorUrl,
  enableSandbox: true,
  portalBaseUrls: {
    'public-portal': publicBaseUrl,
    'admin-portal': adminBaseUrl,
    'platform-portal': platformBaseUrl,
  },
};

writeRuntimeConfig('public-portal');
writeRuntimeConfig('admin-portal');
writeRuntimeConfig('platform-portal');

console.log(`Wrote E2E runtime config with API base URL ${apiBaseUrl}`);

function writeRuntimeConfig(appId) {
  mkdirSync(configDir, { recursive: true });
  const target = join(configDir, `runtime.${appId}.json`);
  writeFileSync(target, `${JSON.stringify({ ...baseConfig, appId }, null, 2)}\n`, 'utf8');
}

function required(name) {
  const value = optional(name);
  if (!value) {
    throw new Error(`${name} is required`);
  }
  return value;
}

function optional(name) {
  const value = process.env[name]?.trim();
  return value ? value : undefined;
}
