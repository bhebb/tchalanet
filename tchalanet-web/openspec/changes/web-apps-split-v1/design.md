# Design

## Apps Cibles

```text
apps/
  public-portal/
  admin-portal/
  platform-portal/
```

`apps/pos-portal` reste une extraction future si la vente POS grossit.

Chaque app doit pouvoir être build/deploy seule :

```text
public-portal   sans admin-portal ni platform-portal
admin-portal    sans public-portal ni platform-portal
platform-portal sans public-portal ni admin-portal
```

## Libs Cibles

```text
  libs/
    api/
      contracts/                 # folder, not a separate Nx lib in V0
      http/                      # folder, not a separate Nx lib in V0
      backend-client/            # folder, not a separate Nx lib in V0

  core/
    auth/
    i18n/

  ui/
    theme/
    styles/
    components/

    web/
      errors/
      shell/

  page-model/
  shared-config/
```

Ne pas créer `libs/shell`; les shells partagés appartiennent à `libs/web/shell`.
Ne pas créer de sous-libs Nx `libs/api/contracts`, `libs/api/http`, `libs/api/backend-client` en V0 :
ce sont des dossiers internes de `libs/api`.
Ne pas créer `libs/web/navigation` en V0; la navigation web réutilisable reste dans `libs/web/shell`
tant qu'elle appartient au shell.
Ne pas déplacer les libs déjà bien placées (`ui/theme`, `ui/styles`, `ui/components`, `page-model`,
`shared-config`) sauf besoin concret.

## `libs/api`

`libs/api` est une lib technique HTTP + contrats transverses. Elle ne devient pas un registre de
clients métier.

### `libs/api/src/lib/contracts`

Contient notamment :

```text
ApiResponse<T>
ProblemDetail
TchPage<T>
ApiNotice
ServiceStatus
ActionItem
NavigationDestination
```

### `libs/api/src/lib/http`

Contient notamment :

```text
interceptors auth / request id / error mapping
constantes headers
helpers query params / pagination
normalisation ProblemDetail
```

### `libs/api/src/lib/backend-client`

Contient notamment :

```text
BackendClient, wrapper technique autour de HttpClient
unwrap automatique ApiResponse<T>
mapping central des erreurs
support pagination
support headers spécifiques via options
```

Ne pas créer `libs/api/clients`.

## `libs/core`

### `libs/core/auth`

Contient :

```text
page login partagée
session store
guards
permissions
login/logout
logique d'état d'authentification
```

Ne contient pas :

```text
clients métier admin
clients métier platform
clients métier POS
registre central d'endpoints backend
```

### `libs/core/i18n`

Contient :

```text
loader i18n runtime
locale state
bundle configuration
merge local fallback + backend/bootstrap overrides
```

## `libs/ui`

### `libs/ui/theme`

Contient :

```text
runtime theme
ThemeStore
ThemeDomApplier
preset registry
mapping backend tokens vers --tch-*
```

### `libs/ui/styles`

Contient :

```text
SCSS primitives
breakpoints
mixins
typography
Material overrides globaux
```

### `libs/ui/components`

Contient :

```text
composants UI réutilisables
composants stateless
composants sans appel HTTP direct
```

## `libs/web`

### `libs/web/shell`

Contient notamment :

```text
public shell
private/admin shell
platform shell
layout shell utilities
navigation web réutilisable liée au shell
```

### `libs/web/errors`

Contient notamment :

```text
page error
section error
API error presenter
mapping UI depuis ProblemDetail
```

## `libs/page-model`

Contient :

```text
contrats runtime PageModel
renderer
widget host abstrait
helpers de rendu
```

Ne contient pas :

```text
shell
thème runtime
client métier
```

## `libs/shared-config`

Contient :

```text
runtime config
feature flags
config proxy/app metadata si nécessaire
```

## Structure Standard Des Features

Les features sont organisées par surface puis feature :

```text
features/
  admin/
    seller-terminals/
  platform/
    tenants/
  public/
    home/
  pos/
    sale/
```

Chaque feature est organisée par page/flow :

```text
features/admin/seller-terminals/
  list/
  new/
  edit/
  components/
  data-access/
```

Règles :

- Chaque page routée vit dans son propre dossier.
- Chaque page a ses fichiers séparés :
  - `*.page.ts`
  - `*.page.html`
  - `*.page.scss`
  - `*.store.ts` si la page a de l'état.
- Les composants utilisés seulement par une page vivent dans `<page-folder>/components/`.
- Les composants partagés par plusieurs pages de la même feature vivent dans `<feature>/components/`.
- Les composants vraiment réutilisables globalement vivent dans `libs/ui/components/`.
- Le client API métier vit dans `<feature>/data-access/`.
- Les types/mappers/query builders liés à l'API de la feature vivent dans `<feature>/data-access/`.
- Les stores page-specific vivent avec leur page, pas dans `data-access/`.
- Ne pas créer de dossier `shared` dans une feature sans usage réel.

## Exemple Cible — Seller Terminals

```text
features/admin/seller-terminals/
  list/
    seller-terminal-list.page.ts
    seller-terminal-list.page.html
    seller-terminal-list.page.scss
    seller-terminal-list.store.ts
    components/
      seller-terminal-table.ts
      seller-terminal-table.html
      seller-terminal-table.scss
      seller-terminal-filter-bar.ts
      seller-terminal-filter-bar.html
      seller-terminal-filter-bar.scss
      seller-terminal-empty-state.ts
      seller-terminal-empty-state.html
      seller-terminal-empty-state.scss
  new/
    seller-terminal-new.page.ts
    seller-terminal-new.page.html
    seller-terminal-new.page.scss
    seller-terminal-new.store.ts
    components/
      seller-terminal-form.ts
      seller-terminal-form.html
      seller-terminal-form.scss
  edit/
    seller-terminal-edit.page.ts
    seller-terminal-edit.page.html
    seller-terminal-edit.page.scss
    seller-terminal-edit.store.ts
    components/
      seller-terminal-edit-form.ts
      seller-terminal-edit-form.html
      seller-terminal-edit-form.scss
  components/
    seller-terminal-status-badge.ts
    seller-terminal-status-badge.html
    seller-terminal-status-badge.scss
    seller-terminal-pin-reset-dialog.ts
    seller-terminal-pin-reset-dialog.html
    seller-terminal-pin-reset-dialog.scss
  data-access/
    seller-terminals-api.service.ts
    seller-terminals.types.ts
    seller-terminals.mappers.ts
    seller-terminals-query-params.ts
```

## Extraction Des Composants

Une page peut commencer simple. Si `*.page.ts`, `*.page.html` ou `*.page.scss` dépasse environ
100 lignes, extraire un composant.

Extraire en priorité :

```text
table
filter bar
form
summary card
dialog
empty state
error state
toolbar locale
section complexe
```

Promouvoir progressivement :

```text
page-only        -> <page-folder>/components/
feature-shared   -> <feature>/components/
global reusable  -> libs/ui/components/
```

Ne pas promouvoir trop tôt vers du shared/global.

## Placement Des Clients API

```text
libs/api
  = contrats + HTTP technique + BackendClient générique

features/*/*/data-access
  = clients API métier + types API locaux + mappers + query builders
```

Règles :

- Ne pas mettre les clients métier dans `libs/api`.
- Ne pas créer de registre central `libs/api/clients`.
- Chaque slice possède son propre `XxxApiService` dans `data-access/`.
- Un client métier ne sort de sa slice que lorsqu'il a au moins deux consommateurs réels.
- Les composants UI n'injectent jamais `HttpClient`.
- Les composants UI n'injectent pas directement les clients API métier.
- Les pages/stores orchestrent les appels API via `data-access`.

## Portals

### Admin Portal

- Utilise le login partagé depuis `libs/core/auth`.
- Contient les routes tenant admin.
- Ajoute une route lazy pour la vente POS admin.
- Garde la vente POS lazy-loaded dans `admin-portal` en V0.
- Masque/désactive la vente pour les admins qui ne veulent pas cette fonction.
- Garde les features POS dans une zone séparée :
  - `features/pos/home`
  - `features/pos/sale`
- Prépare l'extraction future vers `pos-portal` sans refonte majeure.

### Platform Portal

- Utilise le login partagé depuis `libs/core/auth`.
- Contient les routes superadmin/platform.
- Garde les features platform sous `features/platform`.
- Ne mélange pas les écrans platform avec les écrans tenant admin.
- Garde les opérations platform séparées des features admin tenant.

### Public Portal

- Contient les routes publiques.
- Garde les features publiques sous `features/public`.
- Branche `public-portal` sur `libs/page-model` si la page est rendue par PageModel.
- Ne met pas le shell public dans PageModel.
- Ne met pas le thème runtime dans PageModel.
- Priorise SSR/SSG.

## Proxy / Sous-routes Locales

Sous-routes locales :

```text
/public/**
/admin/**
/platform/**
/pos/**      # futur
/api/v1/**
```

Règles :

- Utiliser des URLs API relatives : `/api/v1/...`.
- Ne pas hardcoder les hosts backend dans les features.
- Garder la config proxy compatible avec déploiement indépendant des apps.

## Angular Moderne

Règles pour nouveau code et migrations ciblées :

- standalone components;
- `bootstrapApplication`;
- lazy routes par surface/feature/page;
- Signals pour état local;
- stores explicites pour pages complexes;
- ne pas introduire NgRx par défaut;
- `@defer` pour les zones lourdes, notamment vente POS;
- control flow moderne Angular;
- composants UI purs : inputs/outputs, pas d'appel HTTP direct;
- appels HTTP dans les API services/stores.

Les composants doivent viser `ChangeDetectionStrategy.OnPush`. Si une migration Angular ajoute
`ChangeDetectionStrategy.Eager`, le résultat final doit être ramené à OnPush lorsque compatible.

Les nouveaux formulaires doivent utiliser signal forms. Les formulaires existants migrent slice par
slice.

Les lectures HTTP réactives doivent préférer `httpResource`/`resource` quand le modèle convient. Les
commandes/mutations restent explicites dans les stores/services propriétaires.

## SSR / Performance

- `admin-portal` reste CSR optimisé en V0.
- `admin-portal` doit rester SSR/hydration-ready.
- Éviter les accès directs non protégés à `window`.
- Éviter les accès directs non protégés à `document`.
- Garder le shell compatible SSR.
- Garder la runtime config compatible SSR.
- Garder les routes lazy propres.
- Préparer une stratégie hybride par route.
- Prioriser SSR/SSG pour `public-portal`.

## Style / UI

- Utiliser les tokens `--tch-*`.
- Utiliser les variables locales `--comp-*` dans les composants réutilisables.
- Ne pas hardcoder les couleurs de marque dans les features.
- Respecter BEM-like naming.
- Utiliser les fichiers `.scss` séparés pour pages/composants.
- Garder les Material overrides globaux dans `libs/ui/styles` ou `libs/ui/theme`.
- Ne pas utiliser `::ng-deep` sauf exception temporaire documentée.
- Garder les composants globaux stateless et réutilisables.
