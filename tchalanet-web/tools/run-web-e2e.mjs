#!/usr/bin/env node
import { spawn } from 'node:child_process';

const rawArgs = process.argv.slice(2);
const apiMode = rawArgs.includes('--api');
const externalMode = rawArgs.includes('--external');
const passThroughArgs = rawArgs.filter(arg => arg !== '--api' && arg !== '--external');

const env = { ...process.env };

if (apiMode) {
  env.WEB_E2E_API = '1';
} else if (externalMode) {
  env.WEB_E2E_EXTERNAL = '1';
} else {
  env.WEB_E2E_EMULATOR = env.WEB_E2E_EMULATOR ?? '1';
  env.TCH_FIREBASE_EMULATOR_HOST = env.TCH_FIREBASE_EMULATOR_HOST ?? '127.0.0.1:9099';
  env.TCH_E2E_SUPERADMIN_EMAIL = env.TCH_E2E_SUPERADMIN_EMAIL ?? 'super_admin@e2e.local';
  env.TCH_E2E_SUPERADMIN_PASSWORD = env.TCH_E2E_SUPERADMIN_PASSWORD ?? 'e2e-password-123';
  env.TCH_E2E_ADMIN_EMAIL = env.TCH_E2E_ADMIN_EMAIL ?? 'admin@e2e.local';
  env.TCH_E2E_ADMIN_PASSWORD = env.TCH_E2E_ADMIN_PASSWORD ?? 'e2e-password-123';
  env.TCH_E2E_TENANT_ID = env.TCH_E2E_TENANT_ID ?? 'stub-tenant-1';
  env.TCH_E2E_SELLER_TERMINAL_ID = env.TCH_E2E_SELLER_TERMINAL_ID ?? 'stub-terminal-1';
}

const args = ['exec', 'nx', 'e2e', 'web-e2e', ...passThroughArgs];
const child = spawn('pnpm', args, { stdio: 'inherit', env });

child.on('exit', code => process.exit(code ?? 1));
