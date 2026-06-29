# Tchalanet Web — Quickstart & Frontend Development Guide

> **Status**: DRAFT — migration en cours
> **Apps**: `apps/public-portal/`, `apps/admin-portal/`, `apps/platform-portal/`
> **Stack**: Angular / Nx / SCSS / Playwright / Vitest
> **Related docs**:
>
> * `WEB_ARCHITECTURE.md`
> * `docs/conventions/web-naming.md`
> * `docs/conventions/web-nx-boundaries.md`
> * `docs/conventions/web-state-management.md`
> * `docs/conventions/web-feature-playbook.md`

---

## 1. Objectif

Ce document sert de guide de démarrage rapide pour développer dans `tchalanet-web`.

Il ne remplace pas le document d’architecture frontend.
La règle principale reste :

```text
Backend runtime prépare une page prête à rendre.
Angular rend shell + layout + widgets.
Angular ne résout pas fileKey/jsonFile en runtime.
Angular ne connaît pas les bindings backend internes.
```

---

## 2. Démarrage rapide

### Installer les dépendances

```bash
pnpm install
```

### Démarrer les applications

```bash
pnpm runtime:local-ide
pnpm nx run public-portal:serve --port=4301
pnpm nx run admin-portal:serve --port=4302
pnpm nx run platform-portal:serve --port=4303
```

Chaque app peut aussi être lancée seule via `pnpm serve:public-portal`,
`pnpm serve:admin-portal` ou `pnpm serve:platform-portal`.

### Lancer les tests unitaires

```bash
pnpm test
```

### Lancer les tests end-to-end

```bash
pnpm nx e2e web-e2e
```

`web-e2e` est le projet Playwright unique. Les tests sont rangés par surface sous
`apps/web-e2e/src/{public-portal,admin-portal,platform-portal}`.

---

## 3. Création initiale du workspace

À utiliser seulement si le workspace n’existe pas encore.

```bash
pnpm init
npx create-nx-workspace@latest .
pnpm install
```

Installer le plugin Angular Nx si nécessaire :

```bash
pnpm add -D @nx/angular
```

Générer une nouvelle app seulement si le workspace doit ajouter une surface déployable :

```bash
pnpm nx g @nx/angular:app <app-name> \
  --directory=apps/<app-name> \
  --routing \
  --style=scss \
  --prefix=tch \
  --standalone \
  --unitTestRunner=vitest \
  --e2eTestRunner=playwright \
  --tags=scope:<surface>,type:app
```

> Ne pas utiliser cette commande dans un workspace déjà initialisé sauf décision explicite.

---

## 4. Structure active

```text
tchalanet-web/
├── apps/
│   ├── public-portal/
│   ├── admin-portal/
│   ├── platform-portal/
│   └── web-e2e/
└── libs/
    ├── api/
    │   └── src/lib/
    │       ├── contracts/  ← contrats backend/web transverses
    │       └── http/       ← clients HTTP, interceptors, helpers API
    ├── core/
    │   ├── auth/           ← auth, login partagé, guards, access/entitlements
    │   └── i18n/           ← runtime i18n partagé
    ├── notifications/      ← notifications privées réutilisables
    ├── page-model/         ← contrats runtime, API, renderer et registre abstrait
    ├── shared-assets/      ← assets publics partagés, i18n fallback, runtime config JSON
    ├── shared-config/      ← settings runtime et feature flags
    ├── web/                ← errors, shell, sandbox
    ├── widgets/            ← registre concret et widgets PageModel
    └── ui/
        ├── components/     ← composants UI réutilisables et stateless
        ├── styles/         ← primitives SCSS compile-time
        └── theme/          ← thème runtime, presets Material 3 et tokens
```

---

## 5. Structure par libs

Les libs actives portent une frontière réelle. Ne pas créer de dossiers à vide.

```text
libs/
  api/             contrats backend/web, clients HTTP et interceptors
  core/auth/       OIDC/Keycloak, session, login, guards, access/entitlements
  core/i18n/       traduction runtime et sélection de langue
  notifications/   notifications privées réutilisables
  shared-assets/   assets publics partagés, i18n fallback, runtime config JSON
  shared-config/   feature flags, settings et configuration runtime
  ui/              components, styles et theme
  page-model/      actif : contrats, API, renderer et registre abstrait
  widgets/         actif : registre concret et widgets dynamiques
  web/errors/      présentation d'erreurs web
  web/shell/       shells web publics/privés/platform
  web/sandbox/     sandbox dev/theme
```

Une lib est créée seulement si un change :

* déplace un slice cohérent ;
* définit ses exports publics ;
* valide ses dépendances Nx ;
* évite une dépendance circulaire ou une confusion réelle.

---

## 6. Rôle des libs actives

### `libs/api`

Responsabilité :

* contrats backend/web ;
* `ApiResponse`, `ActionItem`, `NavigationDestination` et contrats HTTP génériques ;
* clients HTTP transverses ;
* interceptors ;
* helpers API.

Règle :

```text
ui/components peut consommer ActionItem.
ui/components ne possède pas ActionItem.
```

---

### `libs/page-model`

Responsabilité :

* contrats runtime PageModel ;
* `PageModelApi` ;
* renderer et `WidgetHostComponent` ;
* token abstrait `WIDGET_REGISTRY`.

---

### `libs/widgets`

Responsabilité :

* widgets PageModel concrets ;
* registre concret ;
* provider `provideWidgets()`.

---

### `libs/web`

Responsabilité active :

* présentation shell réutilisable sans services applicatifs ;
* footer public ;
* navigation publique basse.

---

### `libs/shared-config`

Responsabilité :

* settings runtime ;
* feature flags ;
* configuration runtime.

---

### `libs/ui/components`

Responsabilité :

* composants visuels réutilisables ;
* composants stateless ;
* `input()` / `output()` ;
* aucun appel HTTP ;
* aucune dépendance NgRx ;
* aucune logique métier applicative.

Exemples :

```text
loading
error-panel
page-error
field-error
brand
nav
overlay-nav
sidebar-nav
lang-switcher
lang-theme-group
```

---

### `libs/ui/styles`

Responsabilité :

* primitives SCSS compile-time ;
* breakpoints ;
* functions ;
* mixins ;
* typography helpers ;
* overlay helpers ;
* Material overrides globaux.

Cette lib ne décide pas du thème courant.

---

### `libs/ui/theme`

Responsabilité :

* thème runtime ;
* light/dark ;
* ThemeDomApplier ;
* application des CSS variables ;
* synchronisation OverlayContainer ;
* presets ;
* tokens ;
* future génération/build de thèmes ;
* future intégration tenant theme.

---

## 7. Convention Page / Container / Component / Widget

```text
Route → Page → Container(s) → Component(s)
```

### Page

```text
*.page.ts
```

Une page est routée. Elle peut injecter services applicatifs, router, stores ou APIs.

### Container

```text
*.container.ts
```

Un container orchestre une sous-zone logique d’une page. Il n’est pas routé.

### Component

Les composants UI simples peuvent garder un nom court :

```text
loading.ts
error-panel.ts
brand.ts
nav.ts
overlay-nav.ts
sidebar-nav.ts
```

Utiliser `*.component.ts` seulement si le nom est ambigu ou si le dossier contient plusieurs classes proches.

### Widget

```text
*.widget.ts
```

Un widget est rendu dynamiquement par PageModel.
Il reçoit des props/data et ne fait pas d’appel HTTP direct.

### Shell

```text
*.shell.ts
```

Un shell structure une surface :

```text
PublicShell = PublicHeader + main + PublicFooter
PrivateShell = PrivateTopAppBar + SidebarNav + main
```

---

## 8. PageModel : règle simple

Le PageModel rend uniquement le contenu.

```text
PageModel = rows / columns / widgets
```

Il ne possède pas :

* PublicHeader ;
* PublicFooter ;
* PrivateShell ;
* SidebarNav ;
* TopAppBar ;
* runtime theme ;
* i18n bootstrap ;
* résolution fileKey/jsonFile.

---

## 9. Checklist développement

Avant d’ajouter du code :

* [ ] Est-ce une page routée ? → `*.page.ts`
* [ ] Est-ce une sous-zone orchestrée ? → `*.container.ts`
* [ ] Est-ce un composant UI pur ? → `libs/ui/components`
* [ ] Est-ce un contrat backend/web ? → `libs/api/contracts`
* [ ] Est-ce un appel HTTP transverse ? → `libs/api/http`
* [ ] Est-ce du SCSS partagé ? → `libs/ui/styles`
* [ ] Est-ce du thème runtime ? → `libs/ui/theme`
* [ ] Est-ce un contrat/API/renderer/helper PageModel ? → `libs/page-model`
* [ ] Est-ce un widget rendu par PageModel ? → `libs/widgets`
* [ ] Est-ce une présentation shell réutilisable sans service app ? → `libs/web`
* [ ] Est-ce une page ou une orchestration de surface ? → feature applicative

---

## 10. Règles non négociables

* Ne pas créer de nouvelle lib sans frontière claire et stable.
* Ne pas créer une lib cible vide.
* Ne pas créer une lib par composant.
* Les composants UI ne font pas d’appel HTTP.
* Les composants UI ne dépendent pas de NgRx.
* Les nouveaux contrats backend/web ciblent `libs/api/contracts`.
* `TchLink` est legacy ; les nouvelles navigations/actions utilisent `ActionItem`.
* PageModel ne gère pas le shell.
* Angular runtime ne dépend pas de `fileKey/jsonFile` pour rendre une page.
* Les styles consomment les tokens `--tch-*`.
* Les composants exposent des variables locales `--comp-*`.

---

## 11. Tags Nx recommandés

Tags de type :

```text
type:app
type:api
type:ui
type:styles
type:theme
type:shared-config
type:page-model
type:widgets
type:web
```

Tags de scope :

```text
scope:public
scope:cashier
scope:admin
scope:platform
scope:shared
```

Exemples :

```text
type:ui,scope:shared
type:api,scope:shared
type:app,scope:web
```

---

## 12. Dépendances Nx cibles

Règles cibles :

```text
public-portal  → page-model, widgets, web, core/auth, core/i18n
admin-portal   → web/shell, web/errors, core/auth, core/i18n, ui/console
platform-portal→ web/shell, web/errors, core/auth, core/i18n, ui/console
web          → page-model, ui/components
widgets      → page-model
page-model   → api, ui/components; jamais widgets
ui/components→ api/contracts, ui/styles, ui/theme si nécessaire
ui/styles    → aucune lib applicative
ui/theme     → api/contracts si nécessaire, jamais widgets/web
api          → shared-* seulement si nécessaire
shared-*     → code générique uniquement
```

À court terme, certaines dépendances peuvent rester dans `apps/<portal>/src/app`.
Les corrections se font slice par slice, pas par refactor massif.

---

## 13. Docs complémentaires

* `WEB_ARCHITECTURE.md` — architecture frontend cible
* `docs/conventions/web-naming.md` — conventions de nommage
* `docs/conventions/web-nx-boundaries.md` — Nx tags et dépendances
* `docs/conventions/web-state-management.md` — placement du state
* `docs/conventions/web-feature-playbook.md` — workflow feature
* `docs/conventions/web-placement-guide.md` — où placer chaque concept
