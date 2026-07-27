# OpenSpec Change — Web Nav & Mobile Hardening V1

## Status

Proposed — 2026-07-26

## Why

Les trois portails (`public-portal`, `admin-portal`, `platform-portal`) partagent
deux shells — `private-shell` (console) et `public-shell` — qui ont reçu leur
responsive **au coup par coup**, écran par écran. L'état sur `main` :

- **Les breakpoints ne suivent pas la convention.** `docs/conventions/style.md`
  §10 impose les window-size classes M3 (600 / 840 / 1200 / 1600) via `ui.up()` /
  `ui.down()` (`libs/ui/styles/src/lib/_breakpoints.scss`). Dans les faits le
  workspace contient **30+ valeurs littérales distinctes** (720, 760, 768, 800,
  840, 860, 900, 960, 980, 1024, 1100, 1120…) réparties sur 122 fichiers, et les
  mixins ne sont utilisés que dans ~10.
- **Trou de navigation 768–839px.** Le burger public disparaît à `≥768px`
  (`public-header.scss`) tandis que la bottom nav était dimensionnée pour
  disparaître à `≥840px` : deux frontières pour une seule bascule mobile→tablette.
- **`PublicBottomNav` est du code mort** — importé nulle part hors de son propre
  spec. La nav publique mobile repose donc entièrement sur le burger + overlay,
  qui n'a jamais été durci en conséquence.
- **`tch-overlay-nav` est un cul-de-sac clavier** : pas de focus trap, pas de
  restitution du focus, pas de fermeture à `Escape`, pas d'`aria-modal`. Le
  drawer privé a le même défaut (Escape existe, mais ni focus trap ni `inert`).
- **Aucun lien de navigation n'annonce la page courante.** `tch-nav` utilise
  `routerLinkActive` sans `ariaCurrentWhenActive` ; `tch-sidebar-nav` calcule un
  `.is-active` purement visuel. La logique d'activité existe, elle n'est pas
  exposée aux technologies d'assistance.
- **Tokens fantômes.** `--tch-z-nav` et `--tch-size-touch-target` (4 occurrences
  dans `admin-list-surface.ts`) n'existent pas dans le manifeste ; les vrais sont
  `--tch-z-*` et `--tch-touch-target`. Le fallback masque l'erreur, donc elle dérive.
- **`100vh` sur 12 surfaces** dont le shell privé et les pages d'auth, alors que
  style.md §6 et §20 l'interdisent explicitement pour les surfaces mobiles.

Le problème de fond n'est pas esthétique : il n'existe **aucune définition unique
de « mobile »** dans le web. Chaque composant a inventé la sienne, donc chaque
nouvel écran en invente une de plus.

## Decision (locked)

- **Une seule frontière mobile↔desktop : 840px**, soit `up(expanded)` en SCSS et
  `TchBreakpointService.isWide()` en TS (qui l'implémente déjà). Les autres
  paliers M3 (600 / 1200 / 1600) restent disponibles pour la densité, jamais pour
  la bascule de navigation.
- **La navigation mobile publique reste burger-only.** `PublicBottomNav` est
  **supprimé** ; l'effort va sur `tch-overlay-nav`.
- **Le modèle de navigation ne change pas.** `PLATFORM_NAVIGATION`,
  `TENANT_ADMIN_NAVIGATION`, `CASHIER_NAVIGATION` et les routes restent tels quels.
- **Périmètre de migration borné** : les 18 fichiers des shells et des briques UI
  de navigation/console. Le reste du workspace converge ensuite via un garde-fou
  automatisé, pas via un sweep manuel de 122 fichiers.

## What Changes

- Contrat de bascule unique et nommé, côté CSS et côté TS
  (`specs/web-responsive-baseline`).
- Durcissement navigation des deux shells : focus trap, restitution du focus,
  `Escape`, `inert`, `aria-modal`, `aria-current`, cibles tactiles
  (`specs/web-shell-navigation`).
- Suppression de `PublicBottomNav` et de sa référence de test.
- Correction des tokens fantômes et remplacement de `100vh` par `100dvh`.
- Garde-fou automatisé contre les breakpoints littéraux.
- Tests : Vitest sur les comportements de navigation, projet Playwright mobile
  (aujourd'hui les 3 projets sont Desktop Chrome uniquement).

## Impact

- `libs/web/shell` (private-shell, public-shell), `libs/ui/components`
  (overlay-nav, nav, sidebar-nav, admin-list-surface), `libs/ui/console`
  (page-shell et briques console), `libs/core/auth` (pages login — `100dvh` seul).
- `apps/*/src/app/app.scss` : `100vh` → `100dvh`.
- `apps/web-e2e` : nouveau projet Playwright mobile.
- Aucune modification du modèle de navigation, des routes, ni des contrats API.
- Une suppression publique : `PublicBottomNav` (0 usage réel).

## Non-goals

- Pas de sweep breakpoints sur les 122 fichiers du workspace.
- Pas de brique table responsive — chantier **C2** de `docs/ARCHITECTURE.md`.
- Pas de suppression du kit `admin-crud` déprécié — chantier **C4**.
- Pas de refonte visuelle ni de changement de hiérarchie de navigation.
- Pas de couverture mobile Flutter (runtime différent, autre change).
