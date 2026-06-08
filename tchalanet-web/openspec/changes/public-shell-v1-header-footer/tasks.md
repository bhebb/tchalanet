# Tasks — Public Shell V1 Header & Footer

## S4 — Shell routé (fondation)

- [x] Renommer `PublicShellComponent` → `TchPublicShellComponent`, sélecteur `tch-public-shell`
- [x] Remplacer `<ng-content />` par `<router-outlet />` dans le shell
- [x] Aligner BEM : `shell__skip` → `public-shell__skip`, `shell__body` → `public-shell__main`
- [x] Regrouper les routes `public/*` sous un parent `TchPublicShellComponent` dans `app.routes.ts`
- [x] Retirer `PublicShellComponent` import + `<tch-page-shell>` de `public-home.page.ts`
- [x] Retirer shell wrapper + `<main>` → `<div>` dans `public-results.page.ts`
- [x] Retirer shell wrapper + `<main>` → `<div>` dans `public-check-ticket.page.ts`
- [x] Retirer shell wrapper + `<main>` → `<div>` dans `public-result-detail.page.ts`
- [x] Retirer shell wrapper + `<main>` → `<div>` dans `public-rules.page.ts`
- [x] Retirer shell wrapper de `public-info.page.ts`

## S1 — Contrat `langs` backend

- [x] Ajouter `PublicLanguageOption` dans `libs/page-model/src/lib/runtime/pagemodel.types.ts`
- [x] Enrichir `PageMeta` avec `currentLang?: string` et `supportedLangs?: readonly PublicLanguageOption[]`
- [x] Ajouter `I18nActions.setLanguages` + reducer handler dans le store i18n
- [x] Exposer `I18nFacade.setLanguages()` pour mise à jour depuis le backend
- [x] Mettre à jour `PublicShellService` pour hydrater le store i18n depuis `page().meta` (avec `tap`)
- [x] Fallback défensif : hydratation ignorée si `supportedLangs` est absent du backend

## S2 — `TchLangSwitcher` dropdown

- [x] Refactorer `TchLangSwitcher` : remplacer boutons inline par `mat-menu` Angular Material
- [x] Bouton trigger : emoji flag + code court (`🇫🇷 FR`) via `mat-icon-button`
- [x] Items menu : emoji flag + label complet (`🇫🇷 Français`)
- [x] Item actif marqué avec `mat-icon` check + couleur primary
- [x] Focus, clavier et a11y natifs du `mat-menu`
- [x] Mettre à jour `LanguageSwitcherComponent` : passer `shortLabel` + `flag` depuis constante locale `LANG_META`

## S3 — Header : burger + hover

- [x] Migrer `public-header__burger` → `mat-icon-button` avec `mat-icon`
- [x] Icône `menu` (fermé) / `close` (ouvert)
- [x] Aria-labels i18n : `public.nav.menu_open` / `public.nav.menu_close`
- [x] Surcharger `--comp-nav-hover-bg` dans `.public-header__nav` pour hover gold Tchalanet
- [x] Surcharger `--comp-nav-active` et `--comp-nav-active-indicator`
- [x] Exposer `--comp-nav-hover-bg` et `--comp-nav-active-indicator` dans `TchNav`
- [x] Vérifier `aria-label` nav : `"Navigation publique"` → clé i18n `public.nav.main`
- [x] Migrer bouton login → `button[tch-action]` (TchActionButton, gold accent par défaut)
- [x] DOM réordonné : burger avant brand (mobile-first)

## S5 — Footer BEM

- [x] Ajouter class `public-footer__heading` sur chaque `h2` de colonne footer
- [x] Ajouter wrapper `public-footer__links` autour des `<a>` dans chaque colonne
- [x] Ajuster grille desktop footer : `repeat(3, minmax(0, 1fr))` → `repeat(auto-fill, minmax(10rem, 1fr))`

## S6 — i18n & a11y

- [x] Ajouter `public.nav.menu_open` / `public.nav.menu_close` dans `fr.json`, `en.json`, `ht.json`
- [x] Ajouter `public.nav.main` dans les 3 fichiers i18n (aria-label nav desktop)
- [x] Clés triées alphabétiquement dans le bloc nav pour chaque locale

## Backend follow-up

- [ ] Backend : ajouter `meta.currentLang` et `meta.supportedLangs` dans `PageRuntimeResponse`
- [ ] Backend : payload `supportedLangs` avec `code`, `label`, `shortLabel`, `flag` pour chaque langue
