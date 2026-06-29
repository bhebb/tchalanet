import { copyFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const profile = process.argv[2];
const allowedProfiles = new Set([
  'local-ide',
  'local-ide-emulator',
  'dev-docker',
  'dev-docker-emulator',
  'stg-vercel',
  'prod-vercel',
]);
const apps = ['public-portal', 'admin-portal', 'platform-portal'];

if (!allowedProfiles.has(profile)) {
  console.error(
    `Usage: node tools/select-runtime-profile.mjs ${Array.from(allowedProfiles).join('|')}`,
  );
  process.exit(1);
}

const root = dirname(fileURLToPath(import.meta.url));
const configDir = join(root, '..', 'libs', 'shared-assets', 'public', 'assets', 'config');

for (const app of apps) {
  const source = join(configDir, `runtime.${app}.${profile}.json`);
  const target = join(configDir, `runtime.${app}.json`);

  if (!existsSync(source)) {
    console.error(`Missing runtime profile: ${source}`);
    process.exit(1);
  }

  copyFileSync(source, target);
  console.log(`selected ${profile} for ${app}`);
}
