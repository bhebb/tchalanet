import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import { TCH_TOKEN_MANIFEST } from '../registry/token-manifest.generated';
import { BACKEND_TOKEN_TO_CSS_VAR } from './theme-token-map';

/**
 * Token contract — keeps the four definitions of the `--tch-*` vocabulary in sync:
 *   1. runtime-vars.scss   (mat-sys → --tch-* bridge)
 *   2. runtime-root.scss   (static tokens + first-paint colour fallback)
 *   3. theme-token-map.ts  (tenant override targets)
 *   4. token-manifest.generated.ts (canonical list the docs reference)
 *
 * The manifest is generated from the SCSS by tools/generate-token-manifest.mjs. These tests fail
 * when any source drifts from the others, so the contract can never silently rot.
 */

const scssDir = resolve(dirname(fileURLToPath(import.meta.url)), '../scss');
const DECL = /(--tch-[a-z0-9-]+)\s*:/g;

function declaredTokens(file: string): Set<string> {
  const css = readFileSync(resolve(scssDir, file), 'utf8');
  const set = new Set<string>();
  let m: RegExpExecArray | null;
  while ((m = DECL.exec(css)) !== null) set.add(m[1]);
  return set;
}

const root = declaredTokens('runtime-root.scss');
const vars = declaredTokens('runtime-vars.scss');
const emitted = new Set<string>([...root, ...vars]);

describe('theme token contract', () => {
  it('manifest matches the tokens emitted by the SCSS sources (run `npm run tokens:generate`)', () => {
    expect([...TCH_TOKEN_MANIFEST].sort()).toEqual([...emitted].sort());
  });

  it('every colour token in the mat-sys bridge has a first-paint fallback in :root', () => {
    const bridgeColors = [...vars].filter((t) => t.startsWith('--tch-color-'));
    const missingFallback = bridgeColors.filter((t) => !root.has(t));
    expect(missingFallback).toEqual([]);
  });

  it('every tenant override target resolves to an emitted token', () => {
    const unknownTargets = Object.values(BACKEND_TOKEN_TO_CSS_VAR).filter((t) => !emitted.has(t));
    expect(unknownTargets).toEqual([]);
  });

  it('override targets are valid --tch-* custom properties, never dotted/raw keys', () => {
    const invalid = Object.values(BACKEND_TOKEN_TO_CSS_VAR).filter((t) => !/^--tch-[a-z0-9-]+$/.test(t));
    expect(invalid).toEqual([]);
  });

  it('emits the prescribed stacking and scrim tokens', () => {
    for (const token of [
      '--tch-z-header',
      '--tch-z-drawer',
      '--tch-z-overlay',
      '--tch-z-toast',
      '--tch-color-scrim',
    ]) {
      expect(emitted.has(token), `${token} must be emitted`).toBe(true);
    }
  });
});
