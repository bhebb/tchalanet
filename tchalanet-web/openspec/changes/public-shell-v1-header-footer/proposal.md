# Public Shell V1 — Header & Footer

## Why

Le shell public existe mais n'est pas encore câblé correctement :
- `PublicShellComponent` utilise `<ng-content />` au lieu d'un vrai shell routé (`<router-outlet />`)
- Chaque page importe et wrappe le shell manuellement — la composition est inversée
- `TchLangSwitcher` affiche les langues en ligne (nav-like) au lieu d'un dropdown
- La liste des langues vient d'une constante frontend (`['fr', 'en', 'ht']`) et non du runtime backend
- Le bouton login est un `<button>` CSS custom au lieu de `TchActionButton`
- Le burger est un `<button>` custom CSS au lieu de `mat-icon-button` Angular Material
- Le hover/active nav est neutre Material — manque l'identité Tchalanet (accent gold)
- Plusieurs classes BEM ne sont pas alignées avec la convention `docs/conventions/style.md`

## What

### S4 — Shell routé (fondation)
- Migrer `PublicShellComponent` → `TchPublicShellComponent` avec `<router-outlet />`
- Regrouper toutes les routes `public/*` sous un parent `TchPublicShellComponent`
- Retirer l'import et le wrapper `<tch-page-shell>` de chaque page publique
- Corriger les `<main>` imbriqués dans les pages (→ `<div>`)
- Aligner BEM : `shell__skip` → `public-shell__skip`, `shell__body` → `public-shell__main`

### S1 — Contrat `langs` backend
- Ajouter `PublicLanguageOption` dans `libs/page-model`
- Enrichir `PageMeta` avec `currentLang` et `supportedLangs`
- Hydrater le store i18n depuis `PageRuntimeResponse.meta` dans `PublicShellService`
- Fallback défensif `['fr', 'en', 'ht']` si le backend ne fournit pas encore le champ

### S2 — `TchLangSwitcher` dropdown
- Refactorer `TchLangSwitcher` (lib) : boutons inline → `mat-menu` dropdown
- Bouton fermé : `🇫🇷 FR` (emoji + code court)
- Menu ouvert : `🇫🇷 Français` / `🇬🇧 English` / `🇭🇹 Kreyòl`
- Emoji Unicode V1 (pas de SVG assets)
- `LanguageSwitcherComponent` branché sur langs du store (déjà le cas, bénéficie de S1)

### S3 — Header : burger + hover
- Migrer burger → `mat-icon-button` avec icônes `menu` / `close`
- Aria-labels : `"Ouvrir le menu"` / `"Fermer le menu"` (i18n keys)
- Surcharge locale hover/active Tchalanet dans `PublicHeader` via `--comp-nav-*`
- Gold accent subtil : `color-mix(in srgb, var(--tch-color-accent) 14%, transparent)`
- `TchNav` reste neutre — surcharge locale seulement

### S5 — Footer BEM + colonnes
- Ajouter classes `public-footer__heading` sur `h2`, `public-footer__links` wrapper sur liens
- Grille desktop : 3 → 4-5 colonnes selon payload backend

### S6 — i18n & a11y
- Clés aria burger open/close dans `fr.json`, `en.json`, `ht.json`
- Clés lang switcher si manquantes

## Impact

Projet unique : `tchalanet-web`

Fichiers touchés principaux :
- `apps/tch-portal/src/app/app.routes.ts`
- `apps/tch-portal/src/app/features/public/shell/public-shell.component.ts`
- `apps/tch-portal/src/app/features/public/shell/public-header.ts`
- `apps/tch-portal/src/app/features/public/public-*.page.ts` (6 fichiers)
- `libs/page-model/src/lib/runtime/pagemodel.types.ts`
- `libs/ui/components/src/lib/lang-switcher/lang-switcher.ts`
- `apps/tch-portal/src/app/core/i18n/language-switcher.ts`
- `apps/tch-portal/public/assets/i18n/fr.json`, `en.json`, `ht.json`
- `libs/web/src/lib/public-shell/public-footer.ts`

## Non-goals

- Aucun changement sur le shell privé
- Pas de nouveau composant shell supplémentaire
- Pas de changement de couleur/thème (défini dans `docs/conventions/theme.md`)
- Pas de widgets publics dans ce change
- Pas de backend work (backend follow-up pour `supportedLangs` dans `PageRuntimeResponse`)
