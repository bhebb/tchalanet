# Tasks — web-nav-mobile-hardening-v1

Ordre : socle d'abord (breakpoints/tokens), puis shell public, puis shell privé,
puis tests. Cocher en temps réel (règle `openspec-workflow`).

## 1. Socle — breakpoints, tokens, garde-fou (`specs/web-responsive-baseline`)

- [x] Migrer les media queries des shells vers `ui.up()` / `ui.down()` :
      `private-shell-layout.component.scss` (720 → `down(expanded)`),
      `public-header.scss` (480 / 768 / 1024 → `up(medium)` / `up(expanded)` /
      `up(large)`), `public-footer.ts` (768 / 1024 → `up(medium)` / `up(large)`).
- [x] Migrer les briques UI de navigation/console : `section-header.ts`,
      `error-panel.ts`, `page-error.ts`, `pub-card-grid.ts`,
      `admin-list-surface.ts`, `admin-page-shell.component.ts`,
      `admin-dialog-shell.component.ts`, `admin-crud-shell.component.ts`,
      `admin-data-toolbar.component.ts`, `admin-detail-layout.component.scss`,
      `profile.page.scss`, `account-activation.page.scss`.
      (`nav.ts` et `overlay-nav.ts` n'avaient aucune media query. Le kit
      `admin-crud/` déprécié — chantier C4 — est exempté plutôt que migré.)
- [x] ~~Externaliser en `.scss` compagnon~~ — **inutile** : les 3 apps déclarent
      `inlineStyleLanguage: "scss"` et `includePaths: [libs/ui/styles/src/lib]`,
      donc `@use 'index' as ui;` fonctionne directement dans un bloc `styles: [...]`.
      Vérifié par un build `public-portal`.
- [x] Corriger `--tch-size-touch-target` → `--tch-touch-target` (4 occurrences,
      `admin-list-surface.ts`).
- [x] Remplacer `100vh` par `100dvh` : `private-shell-layout.component.scss`,
      les 3 `apps/*/src/app/app.scss`, `libs/core/auth/src/lib/login/tch-login.page.scss`,
      `libs/core/auth/src/lib/forgot-password/forgot-password.page.scss`.
- [x] Garde-fou : `tools/breakpoint-contract.mjs` + script `breakpoints:contract`
      (ajouté à `contracts:check`) + étape dans `.github/workflows/web-pr.yml`.
      Pas de stylelint dans le workspace ; le tooling suit le pattern existant
      `widget-registry-contract.mjs`. Refuse aussi `100vh`.

## 2. Shell public (`specs/web-shell-navigation`)

- [x] Supprimer `public-bottom-nav.ts`, son export dans `libs/web/shell/src/index.ts`
      et sa référence dans `public-shell.spec.ts` (0 usage réel ; le token fantôme
      `--tch-z-nav` disparaît avec).
- [x] `tch-overlay-nav` : `role="dialog"` + `aria-modal="true"` sur le panneau.
- [x] `tch-overlay-nav` : focus trap (`ConfigurableFocusTrapFactory`,
      `@angular/cdk/a11y`) + restitution du focus au déclencheur à la fermeture.
- [x] `tch-overlay-nav` : fermeture sur `Escape`.
- [x] `tch-overlay-nav` : `routerLinkActive` + `ariaCurrentWhenActive="page"`.
- [x] `tch-overlay-nav` : rendre les destinations externes (aujourd'hui ignorées
      silencieusement) et afficher l'`icon` déclarée dans le contrat.
- [x] `tch-overlay-nav` : cibles tactiles à `var(--tch-touch-target, 48px)`.
- [x] `tch-overlay-nav` : `closeLabel` traduisible pour le backdrop (l'aria-label
      était `"Close navigation"` en dur).
- [x] `public-header` : aligner la bascule burger ↔ nav inline sur `up(expanded)`,
      ce qui supprime le trou 768–839px.
- [x] `public-header` : refermer l'overlay quand on passe en `isWide()` — sinon un
      redimensionnement laisse un overlay ouvert sans burger pour le refermer.
- [x] `tch-nav` : `ariaCurrentWhenActive="page"`.

## 3. Shell privé (`specs/web-shell-navigation`)

- [x] `private-shell-layout` : focus trap dans le drawer en mode overlay +
      restitution du focus au burger.
- [x] `private-shell-layout` : `inert` sur `.content` pendant l'overlay.
- [x] `private-shell-layout` : `inert` sur le **drawer lui-même** quand il est
      replié — hors écran par `translateX(-100%)`, il restait tabulable derrière
      le contenu (défaut non listé au départ).
- [x] `private-shell-layout` : scroll-lock du document pendant l'overlay
      (classe `tch-overlay-open`, même mécanisme que `tch-overlay-nav`).
- [x] `private-shell-layout` : `role="dialog"` / `aria-modal` **uniquement** en
      mode overlay, piloté par `TchBreakpointService.isWide()`.
- [x] `tch-sidebar-nav` : `aria-current="page"` sur les liens actifs (racine et
      enfants), en réutilisant `isActionActive()`.
- [x] `tch-sidebar-nav` : entrée `density` (`comfortable` | `compact`) — cibles à
      `--tch-touch-target` en overlay, densité souris conservée en sidebar
      persistante. Convention alignée sur `tch-draw-label` (style.md §4.1).
- [x] `tch-sidebar-nav` : `aria-labelledby` sur les `<section>` via l'`id` du `<h2>`.
- [x] `safe-area-inset` sur la top-app-bar et le drawer ; scrim via
      `--tch-color-scrim` au lieu d'un `#000` en dur (style.md §15).
- [x] Réutilisation de la clé i18n orpheline `nav.private.ariaLabel` pour nommer
      le dialogue et la nav (le défaut du composant était `'Navigation principale'`
      en dur).

## 4. Tests

- [x] Vitest — focus trap, restitution du focus, `Escape`, `inert`, scroll-lock,
      sémantique dialogue vs panneau permanent (drawer privé + overlay public).
- [x] Vitest — `aria-current="page"` sur les liens actifs, `aria-labelledby` des
      sections, densité tactile, destinations externes rendues.
- [x] Réparer `private-shell-layout.component.spec.ts`, **rouge sur `main`**
      (`AUTH_CLIENT` et `ThemeStore` absents du TestBed — la cible `web-shell:test`
      n'étant pas en CI, personne ne le voyait).
- [x] Câbler les tests de libs en CI : `pnpm run test` ne couvrait que les 3 apps,
      donc aucun spec de `libs/**` ne tournait. Passé à
      `nx run-many -t test --all --exclude=web,tchalanet-web` (16 projets verts ;
      `web` est la façade morte du chantier C4, sans spec).
- [x] Placement des specs de nav : écrits dans `web-shell` et non dans
      `ui-components`. La cible `ui-components:test` tourne sur **Vitest brut**,
      sans compilateur Angular, et ne sait donc pas instancier un composant à
      `input()` signal (NG0303). L'aligner sur `@angular/build:unit-test` exige de
      réécrire `libs/ui/components/tsconfig.json` (il lui manque
      `moduleResolution: "bundler"`, `strict`, `strictTemplates`…) — hors périmètre.
      **Suivi à ouvrir.**
- [x] Playwright — projet `public-portal-mobile` (390×844, `isMobile`, `hasTouch`)
      dans `apps/web-e2e/playwright.config.ts` + `src/mobile/public-nav.spec.ts` :
      burger et nav inline masquée, absence de scroll horizontal, menu modal,
      `Escape` + restitution du focus, fermeture après navigation, bascule 839→840,
      fermeture de l'overlay au franchissement de la borne.
      `data-testid` ajoutés sur le burger et les navs inline.
- [ ] **Bloqué — non exécuté.** `playwright.config.ts` ne se charge pas :
      `ReferenceError: exports is not defined in ES module scope`. Vérifié
      identique sur l'arbre vierge (`git stash`), donc **antérieur à ce change** et
      cohérent avec l'absence de cible `web-e2e` en CI. Cause : le fichier est
      chargé par deux loaders aux attentes opposées — Playwright le transpile en
      CJS (`import.meta.url` déclenche la détection ESM et casse), tandis que le
      graphe Nx le charge en ESM par type-stripping (`__dirname` indisponible).
      Corriger l'un casse l'autre : à traiter dans un change dédié à l'outillage e2e.

## 4b. Vérification exécutée

- [x] `pnpm run test` — 16 projets verts (dont `web-shell`, 53 tests).
- [x] `pnpm nx run-many -t build -p public-portal,admin-portal,platform-portal` — vert.
- [x] `pnpm run lint` — vert. (Les cibles `lint` des libs remontent 11 erreurs de
      dépendance circulaire `ui-console ↔ ui-components ↔ core-auth` : compte
      identique sur l'arbre vierge, donc antérieures ; `pnpm run lint` ne couvre
      que les 3 apps et n'est pas élargi ici.)
- [x] `node tools/breakpoint-contract.mjs` — 0 breakpoint littéral, 0 `100vh`.
- [x] `openspec validate web-nav-mobile-hardening-v1 --strict` — valide.
- [x] Bundle public-portal : `menu-button{display:none}` et `nav--short{display:flex}`
      à **840px**, `nav--long` à **1200px** → le trou 768–839px a bien disparu.
- [x] Bundle platform-portal : `.drawer`/`.burger`/`.workspace` basculent à
      `max-width: 839.98px`, aligné sur `TchBreakpointService.isWide()`.
- [x] Navigateur, platform-portal à 390×844 : `min-height` résolue à 844px
      (`100dvh` = `innerHeight`), scroll horizontal = 0.

## 5. Documentation

- [x] `docs/conventions/style.md` §10.1 et §10.2 : frontière unique 840px pour la
      navigation, sémantique overlay vs permanent, garde-fou breakpoints, et le
      fait que `@use 'index' as ui;` marche dans un bloc `styles: [...]`.
- [x] `libs/web/shell/README.md` : table overlay vs permanent (role, focus trap,
      `inert`, scroll-lock, `Escape`, densité) et rappel du burger-only public.
