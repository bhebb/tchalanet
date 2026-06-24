import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const locales = ['fr', 'en', 'ht'];
const bundles = [
  'common',
  'domain',
  'component',
  'surface-public',
  'surface-admin',
  'surface-platform',
  'surface-seller-terminal',
  'feature-auth',
  'feature-public',
  'feature-admin',
  'feature-platform',
  'feature-seller-terminal',
];

const i18nRoot = path.join(root, 'apps/tch-portal/public/assets/i18n');
const scanRoots = [
  path.join(root, 'apps/tch-portal/src/app'),
  path.join(root, 'apps/tch-portal/public/assets/config'),
];
const scanExtensions = new Set(['.ts', '.html', '.json']);
const ignoredReferencedPrefixes = [
  'http.',
  'https.',
  'tch.',
  'web.',
  'entitlement.',
  'pos.',
];

const checkMode = process.argv.includes('--check');
const allowedTopLevelByBundle = {
  common: new Set(['common']),
  domain: new Set(['catalog', 'domain', 'draw_channel', 'ticket']),
  component: new Set(['app', 'component', 'error', 'layout', 'quickaction', 'readiness', 'shell', 'widget']),
  'surface-public': new Set(['brand', 'footer', 'legal', 'nav', 'public']),
  'surface-admin': new Set(['nav', 'surface']),
  'surface-platform': new Set(['nav', 'platform', 'surface']),
  'surface-seller-terminal': new Set(['nav', 'surface']),
  'feature-auth': new Set(['account', 'auth', 'profile']),
  'feature-public': new Set(['cta', 'dashboard', 'home', 'public', 'theme']),
  'feature-admin': new Set(['admin', 'dashboard', 'feature']),
  'feature-platform': new Set(['platform']),
  'feature-seller-terminal': new Set(['cashier', 'sellerTerminal']),
};

function isPlainObject(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function deepMerge(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (isPlainObject(target[key]) && isPlainObject(value)) {
      deepMerge(target[key], value);
    } else {
      target[key] = value;
    }
  }
  return target;
}

function flatten(value, prefix = '', result = new Set()) {
  if (typeof value === 'string') {
    result.add(prefix);
    return result;
  }
  if (!isPlainObject(value)) {
    return result;
  }
  for (const [key, child] of Object.entries(value)) {
    flatten(child, prefix ? `${prefix}.${key}` : key, result);
  }
  return result;
}

async function readJson(file) {
  return JSON.parse(await readFile(file, 'utf8'));
}

async function localeKeys(locale) {
  const merged = {};
  for (const bundle of bundles) {
    const file = path.join(i18nRoot, locale, `${bundle}.json`);
    deepMerge(merged, await readJson(file));
  }
  return flatten(merged);
}

async function localeBundleDiagnostics(locale) {
  const seen = new Map();
  const duplicates = [];
  const invalidTopLevels = [];

  for (const bundle of bundles) {
    const file = path.join(i18nRoot, locale, `${bundle}.json`);
    const data = await readJson(file);
    const allowedTopLevels = allowedTopLevelByBundle[bundle] ?? new Set();

    for (const topLevel of Object.keys(data)) {
      if (!allowedTopLevels.has(topLevel)) {
        invalidTopLevels.push(`${bundle}.json:${topLevel}`);
      }
    }

    for (const key of flatten(data)) {
      const existing = seen.get(key);
      if (existing) {
        duplicates.push(`${key} (${existing}, ${bundle}.json)`);
      } else {
        seen.set(key, `${bundle}.json`);
      }
    }
  }

  return { duplicates, invalidTopLevels };
}

async function walk(dir, files = []) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      await walk(fullPath, files);
    } else if (scanExtensions.has(path.extname(entry.name)) && !entry.name.endsWith('.spec.ts')) {
      files.push(fullPath);
    }
  }
  return files;
}

function collectReferences(source) {
  const references = new Set();
  const translatePipe = /['"`]([a-z][a-zA-Z0-9_-]*(?:\.[a-zA-Z0-9_-]+){1,})['"`]\s*\|\s*translate/g;
  const translateInstant = /\.instant\(\s*['"`]([a-z][a-zA-Z0-9_-]*(?:\.[a-zA-Z0-9_-]+){1,})['"`]/g;
  const routeKey = /\b(?:titleKey|descriptionKey|labelKey|messageKey|bodyKey|eyebrowKey|fileKey)\s*:\s*['"`]([^'"`]+)['"`]/g;
  for (const pattern of [translatePipe, translateInstant, routeKey]) {
    for (const match of source.matchAll(pattern)) {
      const key = match[1];
      if (!key.includes('$') && !ignoredReferencedPrefixes.some(prefix => key.startsWith(prefix))) {
        references.add(key);
      }
    }
  }
  return references;
}

async function referencedKeys() {
  const references = new Set();
  for (const scanRoot of scanRoots) {
    for (const file of await walk(scanRoot)) {
      const source = await readFile(file, 'utf8');
      for (const key of collectReferences(source)) {
        references.add(key);
      }
    }
  }
  return references;
}

function difference(left, right) {
  return [...left].filter(value => !right.has(value)).sort();
}

const declaredByLocale = new Map();
const diagnosticsByLocale = new Map();
for (const locale of locales) {
  declaredByLocale.set(locale, await localeKeys(locale));
  diagnosticsByLocale.set(locale, await localeBundleDiagnostics(locale));
}

const canonical = declaredByLocale.get('fr');
const references = await referencedKeys();
const missingByLocale = new Map();
for (const locale of locales) {
  missingByLocale.set(locale, difference(canonical, declaredByLocale.get(locale)));
}

const missingReferences = difference(references, canonical);
const unused = difference(canonical, references);

console.log(`i18n inventory (${canonical.size} declared fr keys, ${references.size} referenced keys)`);
for (const locale of locales) {
  const missing = missingByLocale.get(locale);
  const diagnostics = diagnosticsByLocale.get(locale);
  console.log(
    `- ${locale}: ${declaredByLocale.get(locale).size} keys, ${missing.length} missing vs fr, ` +
      `${diagnostics.duplicates.length} duplicate declarations, ` +
      `${diagnostics.invalidTopLevels.length} invalid bundle roots`,
  );
}
console.log(`- referenced but missing: ${missingReferences.length}`);
console.log(`- declared but not referenced: ${unused.length}`);

if (missingReferences.length > 0) {
  console.log('\nReferenced but missing:');
  for (const key of missingReferences.slice(0, 100)) {
    console.log(`  ${key}`);
  }
}

for (const [locale, diagnostics] of diagnosticsByLocale.entries()) {
  if (diagnostics.duplicates.length > 0) {
    console.log(`\nDuplicate declarations (${locale}):`);
    for (const duplicate of diagnostics.duplicates.slice(0, 100)) {
      console.log(`  ${duplicate}`);
    }
  }

  if (diagnostics.invalidTopLevels.length > 0) {
    console.log(`\nInvalid bundle roots (${locale}):`);
    for (const invalid of diagnostics.invalidTopLevels.slice(0, 100)) {
      console.log(`  ${invalid}`);
    }
  }
}

if (
  checkMode &&
  (missingReferences.length > 0 ||
    [...missingByLocale.values()].some(keys => keys.length > 0) ||
    [...diagnosticsByLocale.values()].some(
      diagnostics => diagnostics.duplicates.length > 0 || diagnostics.invalidTopLevels.length > 0,
    ))
) {
  process.exitCode = 1;
}
