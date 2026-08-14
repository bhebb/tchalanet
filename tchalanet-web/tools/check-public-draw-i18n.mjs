import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const repoRoot = dirname(webRoot);
const migrationPath = resolve(
  repoRoot,
  'tchalanet-server/tchalanet-app/src/main/resources/db/migration/V204__seed_core_game_draw.sql',
);
const localeDir = resolve(webRoot, 'libs/shared-assets/public/assets/i18n');
const locales = ['fr', 'en', 'ht'];

const migration = readFileSync(migrationPath, 'utf8');
const slotRegex = /\(\s*'([A-Z]{2}_[A-Z0-9_]+)'\s*,\s*'([A-Z]{2})'\s*,/g;
const slots = [...migration.matchAll(slotRegex)].map(match => match[1]);
const keys = [...new Set(slots.map(slotToDrawChannelKey))].sort();

const missing = [];
for (const locale of locales) {
  const domain = JSON.parse(readFileSync(resolve(localeDir, locale, 'domain.json'), 'utf8'));
  for (const key of keys) {
    const value = getPath(domain, key);
    if (!isPublicLabel(value, key)) {
      missing.push(`${locale}: ${key}`);
    }
  }
}

if (missing.length) {
  console.error('Missing or invalid public draw-channel labels:');
  for (const entry of missing) {
    console.error(`- ${entry}`);
  }
  process.exit(1);
}

console.log(`public draw i18n ok (${keys.length} keys x ${locales.length} locales)`);

function slotToDrawChannelKey(slotKey) {
  const normalized = slotKey.toLowerCase();
  const separator = normalized.indexOf('_');
  const provider = normalized.slice(0, separator);
  const period = normalized.slice(separator + 1);
  return `draw_channel.${provider}.${period}.label`;
}

function getPath(object, path) {
  return path.split('.').reduce((current, part) => current?.[part], object);
}

function isPublicLabel(value, key) {
  if (typeof value !== 'string') return false;
  const clean = value.trim();
  if (!clean || clean === key) return false;
  return !['label', 'null', 'undefined'].includes(clean.toLowerCase());
}
