/**
 * Breakpoint contract — garde-fou responsive.
 *
 * Deux règles, sur les surfaces listées dans SCOPE :
 *
 *  1. aucune valeur de breakpoint littérale dans un `@media (min-width:|max-width:)` —
 *     utiliser `ui.up()` / `ui.down()` / `ui.between()` de `libs/ui/styles/src/lib/_breakpoints.scss` ;
 *  2. aucun `100vh` — utiliser `100dvh` (barre d'adresse mobile).
 *
 * Voir `docs/conventions/style.md` §6 et §10, et
 * `openspec/changes/web-nav-mobile-hardening-v1/specs/web-responsive-baseline/spec.md`.
 *
 * Élargir SCOPE au fur et à mesure que le reste du workspace est migré.
 */
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();

/** Répertoires sous contrat. */
const SCOPE = [
  'libs/web/shell',
  'libs/ui/components',
  'libs/ui/console',
  'libs/ui/styles',
  'libs/core/auth',
];

/**
 * Exemptions, chacune justifiée :
 *  - `_breakpoints.scss` définit les bornes, c'est le seul endroit où elles ont le droit d'exister ;
 *  - le kit `admin-crud/` est déprécié (chantier C4) hors `admin-list-surface`, on ne le migre pas
 *    pour le supprimer ensuite.
 */
const EXEMPT = [
  'libs/ui/styles/src/lib/_breakpoints.scss',
  'libs/ui/components/src/lib/admin-crud/admin-data-table.ts',
  'libs/ui/components/src/lib/admin-crud/admin-mobile-card-list.ts',
  'libs/ui/components/src/lib/admin-crud/admin-form-shell.ts',
  'libs/ui/components/src/lib/admin-crud/admin-list-toolbar.ts',
];

const EXTENSIONS = new Set(['.scss', '.ts']);
const LITERAL_MEDIA = /@media[^;{]*\((?:min|max)-width\s*:/g;
const VIEWPORT_HEIGHT = /\b100vh\b/g;

async function* walk(dir) {
  let entries;
  try {
    entries = await readdir(dir, { withFileTypes: true });
  } catch {
    return;
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'node_modules' || entry.name === 'dist') continue;
      yield* walk(full);
    } else if (EXTENSIONS.has(path.extname(entry.name))) {
      yield full;
    }
  }
}

function findMatches(text, pattern) {
  const hits = [];
  for (const match of text.matchAll(pattern)) {
    const line = text.slice(0, match.index).split('\n').length;
    hits.push({ line, text: match[0].trim() });
  }
  return hits;
}

const literalBreakpoints = [];
const viewportHeights = [];
let scanned = 0;

for (const dir of SCOPE) {
  for await (const file of walk(path.join(root, dir))) {
    const rel = path.relative(root, file);
    if (EXEMPT.includes(rel)) continue;
    scanned += 1;
    const text = await readFile(file, 'utf8');
    for (const hit of findMatches(text, LITERAL_MEDIA)) {
      literalBreakpoints.push(`${rel}:${hit.line} — ${hit.text}`);
    }
    for (const hit of findMatches(text, VIEWPORT_HEIGHT)) {
      viewportHeights.push(`${rel}:${hit.line}`);
    }
  }
}

console.log(`breakpoint contract (${scanned} fichiers, ${SCOPE.length} répertoires sous contrat)`);
console.log(`- media queries à valeur littérale: ${literalBreakpoints.length}`);
console.log(`- occurrences de 100vh: ${viewportHeights.length}`);

if (literalBreakpoints.length) {
  console.error(
    `\nmedia queries à valeur littérale — utiliser ui.up()/ui.down()/ui.between():\n${literalBreakpoints
      .map(hit => `  - ${hit}`)
      .join('\n')}`,
  );
}
if (viewportHeights.length) {
  console.error(
    `\n100vh interdit sur les surfaces mobiles — utiliser 100dvh:\n${viewportHeights
      .map(hit => `  - ${hit}`)
      .join('\n')}`,
  );
}
if (literalBreakpoints.length || viewportHeights.length) {
  process.exitCode = 1;
}
