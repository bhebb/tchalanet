#!/usr/bin/env node
/**
 * Build the self-hosted Material Symbols (Outlined) font used by the app.
 *
 * Downloads the Google Fonts instance and converts it to woff2 (keeping all glyphs + the icon
 * ligatures, which live under the `rlig` feature wrapped in extension lookups). Output:
 *   libs/shared-assets/public/assets/fonts/material-symbols-outlined.woff2  (~310 KB)
 *
 * We intentionally do NOT glyph-subset: Material Symbols ligature subsetting is fragile (it can
 * silently drop the `rlig` feature, leaving icons as raw ligature text), and the full instance is
 * already small once woff2-compressed. Icon names come from both the web source AND the backend
 * pagemodel fragments/templates, so a subset would also have to track those — not worth the risk.
 *
 * Requirements: python3 with `fonttools` + `brotli` (pip3 install fonttools brotli).
 * Run: `npm run fonts:material-symbols`
 */
import { execFileSync } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const OUT = 'libs/shared-assets/public/assets/fonts/material-symbols-outlined.woff2';
const CSS_URL = 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined&display=block';

// Resolve the current font URL from the Google CSS (the hash changes across versions).
const css = await (await fetch(CSS_URL, { headers: { 'User-Agent': 'Mozilla/5.0' } })).text();
const url = css.match(/src:\s*url\(([^)]+)\)/)?.[1];
if (!url) throw new Error('Could not resolve the Material Symbols font URL from Google CSS.');

const dir = mkdtempSync(join(tmpdir(), 'ms-font-'));
const raw = join(dir, 'full.ttf');
writeFileSync(raw, Buffer.from(await (await fetch(url)).arrayBuffer()));

// Convert to woff2, keeping everything (all glyphs + GSUB ligatures).
execFileSync(
  'python3',
  [
    '-c',
    `from fontTools.ttLib import TTFont
f = TTFont(${JSON.stringify(raw)})
f.flavor = 'woff2'
f.save(${JSON.stringify(OUT)})
print('wrote', ${JSON.stringify(OUT)})`,
  ],
  { stdio: 'inherit' },
);
