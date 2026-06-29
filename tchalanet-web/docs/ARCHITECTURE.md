# Tchalanet Web — Architecture Frontend

> **Statut** : Architecture active — extraction progressive en cours
> **Apps** : `apps/tch-portal/`, `apps/public-portal/`, `apps/admin-portal/`, `apps/platform-portal/` — Angular / Nx
> **Objectif** : garder une architecture frontend lisible, slice-first, sans créer de libs vides ni transformer PageModel en usine à gaz.

---

## 1. Objectif

`tchalanet-web` doit permettre de construire :

- la page publique ;
- les dashboards privés ;
- les shells public / privé ;
- les widgets dynamiques ;
- les thèmes runtime ;
- les composants UI réutilisables ;
- plusieurs apps déployables indépendamment quand la surface le justifie ;

tout en gardant une règle simple :

```text
Le backend prépare une page prête à rendre.
Angular rend le shell, le layout et les widgets.
Angular ne résout pas les fileKey/jsonFile.
Angular ne connaît pas les bindings backend internes.
```

Le PageModel n’est pas un CMS complet.
C’est un **contrat de composition de page prêt-à-rendre**.

---

## 2. Structure active

```text
tchalanet-web/
├── apps/
│   ├── public-portal/     ← app publique, SSR/hydration-ready, surface publique canonique
│   ├── admin-portal/      ← app tenant admin, CSR V0, déployable seule
│   ├── platform-portal/   ← app plateforme/superadmin, CSR V0, déployable seule
│   ├── tch-portal/        ← portail historique privé pendant migration, plus de surface publique
│   ├── web-e2e/           ← Playwright e2e unique pour public/admin/platform
│   └── proxy.conf.cjs     ← proxy local partagé vers /api/v1
│
│   apps/*/src/app/
│   ├── app.config.ts
│   ├── app.routes.ts
│   └── features/           ← pages, flows et orchestration propres à l'app
└── libs/
    ├── api/
    │   └── src/lib/
    │       ├── contracts/      ← contrats backend/web transverses
    │       ├── http/           ← interceptors, headers, helpers API
    │       └── backend-client/ ← TchBackendClient générique
    ├── core/
    │   ├── auth/          ← login partagé, auth/session/guards quand extraits
    │   └── i18n/          ← runtime i18n partagé quand extrait
    ├── page-model/         ← contrats runtime, API, renderer et registre abstrait
    ├── shared-assets/      ← assets statiques partagés servis en /assets/**
    ├── shared-config/      ← settings runtime et feature flags
    ├── web/                ← façade historique web, à réduire progressivement
    │   ├── errors/         ← modèle/copy/routing UI des erreurs web
    │   └── shell/          ← primitives shell web partagées
    ├── widgets/            ← registre concret et widgets PageModel
    └── ui/
        ├── components/     ← composants UI réutilisables et stateless
        ├── styles/         ← primitives SCSS compile-time
        └── theme/          ← thème runtime, presets Material 3 et tokens
```

Cette structure reste volontairement petite. Chaque lib active porte une frontière réelle. Ne pas
créer de sous-lib Nx pour `api/contracts`, `api/http` ou `api/backend-client` : `libs/api` reste une
seule lib avec des dossiers internes.

`pos-portal` n'existe pas en V0. La vente POS reste lazy-loaded dans `admin-portal` jusqu'à ce que la
surface justifie une app séparée.

La surface publique appartient désormais à `apps/public-portal/src/app/features/public`. Les pages
publiques ne sont plus maintenues dans `apps/tch-portal`. Les assets partagés sont servis depuis
`libs/shared-assets/public` par chaque app.

Les tests end-to-end Angular/Web vivent dans un seul projet Playwright :

```text
apps/web-e2e/src/
  public-portal/
  admin-portal/
  platform-portal/
```

Le run standard utilise Chromium et démarre les apps sur des ports locaux stables :

```text
public-portal   http://localhost:4301
admin-portal    http://localhost:4302
platform-portal http://localhost:4303
```

---

## 3. Rôle des libs actives

### `libs/api`

Responsabilité :

- contrats backend/web ;
- types HTTP communs ;
- helpers API ;
- interceptors ;
- `TchBackendClient`, wrapper technique autour de `HttpClient`.

Exemples de contrats à centraliser ici :

```text
ApiResponse
ApiNotice
ProblemDetail model côté frontend
ActionItem
NavigationDestination
TchPage
ServiceStatus
```

Règle importante :

```text
ui/components peut consommer ActionItem.
ui/components ne possède pas ActionItem.
```

Les contrats HTTP génériques ciblent `libs/api/src/lib/contracts`. Les helpers/interceptors ciblent
`libs/api/src/lib/http`. Le client backend générique cible `libs/api/src/lib/backend-client`. Les
contrats runtime PageModel ciblent `libs/page-model`.

Les contrats actifs peuvent rester temporairement dans `apps/tch-portal/src/app/shared/types`, mais uniquement pendant migration.

---

### `libs/page-model`

Responsabilité :

- contrats `PageRuntimeResponse` et types associés ;
- client `PageModelApi` ;
- renderer rows/columns et `WidgetHostComponent` ;
- fallbacks de widgets contenus ;
- token injectable abstrait `WIDGET_REGISTRY` ;
- helpers communs aux widgets et `LabelPipe`.

Cette lib ne dépend jamais de `libs/widgets`.

---

### `libs/widgets`

Responsabilité :

- widgets PageModel concrets ;
- mapping direct type backend → composant Angular ;
- provider `provideWidgets()`, activé par l'app composition root.

Cette lib dépend de `libs/page-model`.

---

### `libs/core/auth`

Responsabilité :

- login partagé ;
- session store ;
- guards ;
- permissions ;
- login/logout ;
- état d'authentification.

Cette lib ne contient aucun client métier admin, platform ou POS.

---

### `libs/core/i18n`

Responsabilité :

- contrats et état de locale partagés ;
- bootstrap i18n réutilisable quand il n'est plus app-coupled ;
- helpers runtime i18n.

Les écrans métier d'administration i18n restent dans leur feature platform/admin.

---

### `libs/shared-assets`

Responsabilité :

- logos, icônes, fonts, images publiques et markdown partagés ;
- bundles JSON i18n locaux de fallback ;
- constantes TypeScript de chemins `/assets/**`.

Chaque app copie `libs/shared-assets/public` dans son browser output. `public-portal` SSR sert ces
fichiers depuis le dossier browser généré, pas depuis le workspace source.

Cette lib ne contient ni secrets runtime, ni store i18n, ni loader ngx-translate, ni shell.

---

### `libs/web`

Responsabilité historique :

- éléments de shell web réutilisables sans injection de services applicatifs ;
- footer public ;
- navigation publique basse.

La lib racine `@tch/web` reste une façade de compatibilité pendant la migration.

---

### `libs/web/errors`

Responsabilité :

- modèle d'erreur web consommable par les pages ;
- mapping copy/traduction depuis les codes backend stables ;
- helpers de routage shell/page/section/field ;
- helpers field error côté formulaire.

Elle ne possède pas `HttpClient` et ne promet pas de workflow support/ticket.

---

### `libs/web/shell`

Responsabilité :

- primitives shell web partagées ;
- feedback shell routable ;
- actions shell qui préservent la surface courante ;
- futures briques public/admin/platform shell quand elles ne dépendent plus d'une app concrète.

La navigation reste dans shell en V0 ; ne pas créer `libs/web/navigation` tant qu'il n'y a pas de
frontière réelle.

---

### `libs/shared-config`

Responsabilité :

- settings runtime ;
- feature flags ;
- configuration frontend runtime.

Cette lib ne doit pas contenir de composants UI.

---

### `libs/ui/components`

Responsabilité :

- composants UI réutilisables ;
- composants stateless ;
- composants basés sur `input()` / `output()` ;
- aucun appel HTTP ;
- aucune dépendance NgRx ;
- aucune logique métier applicative.

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

Ces composants consomment des contrats partagés, mais ne les possèdent pas.

---

### `libs/ui/styles`

Responsabilité :

- primitives SCSS compile-time ;
- breakpoints ;
- functions ;
- mixins ;
- typography helpers ;
- overlay helpers ;
- Material overrides globaux.

Cette lib ne décide jamais du thème courant.

Exemples :

```text
_breakpoints.scss
_functions.scss
_mixins.scss
_typography.scss
_overlay.scss
_material-overrides.scss
_index.scss
README.md
```

---

### `libs/ui/theme`

Responsabilité :

- thème runtime ;
- sélection light/dark ;
- ThemeDomApplier ;
- application des CSS variables ;
- synchronisation Angular Material OverlayContainer ;
- presets ;
- tokens ;
- future génération/build de thème ;
- future intégration tenant theme.

Cette lib porte le `README.md`/guide de thème.

Décisions à conserver :

```text
Theme runtime applique les tokens.
Shared styles consomme les tokens.
Components exposent des variables locales --comp-*.
```

> Fondation theme/styles : le vocabulaire `--tch-*` est généré dans
> `libs/ui/theme/src/registry/token-manifest.generated.ts` et gardé par `theme-token-contract.spec.ts`
> (cohérence pont SCSS ↔ fallback ↔ token-map ↔ docs). L'**application de ces conventions aux
> composants `ui/components` et aux features est un slice ultérieur** (ce socle ne les restyle pas).

---

## 4. Structure cible par extraction

```text
libs/
  api/             contrats backend/web, http technique, backend-client
  core/auth/       login partagé, OIDC/Keycloak, session et guards
  core/i18n/       traduction runtime et sélection de langue
  shared-assets/   assets statiques et constantes de chemins /assets/**
  shared-config/   feature flags, settings et configuration runtime
  ui/              components, styles et theme
  page-model/      actif : contrats, API, moteur de rendu et registre abstrait
  widgets/         actif : registre concret et widgets dynamiques
  web/errors/      actif : modèle/copy/routing d'erreur web
  web/shell/       actif : primitives shell web
  web/             façade historique à réduire
```

Les libs ci-dessus sont actives. Elles restent fines : les clients métier restent dans les features
tant qu'ils n'ont pas plusieurs consommateurs réels.

Une lib est créée seulement lorsqu’un change :

- déplace un slice cohérent ;
- définit ses exports publics ;
- valide ses dépendances Nx ;
- supprime ou réduit une dépendance depuis une app.

Graphe de dépendances actif :

```text
apps/*      -> core/auth, core/i18n, web/errors, web/shell, ui/*, api, page-model, widgets
widgets     -> page-model
web         -> page-model, ui/components
web/errors  -> api
web/shell   -> ui/components, page-model selon besoin
page-model  -X-> widgets
ui          -X-> api clients métier / features
```

---

## 5. PageModel : frontière

Le PageModel rend uniquement le contenu.

```text
PageModel = rows / columns / widgets
```

Il ne possède pas :

- PublicHeader ;
- PublicFooter ;
- PrivateShell ;
- SidebarNav ;
- TopAppBar ;
- theme runtime ;
- i18n bootstrap ;
- résolution fileKey/jsonFile.

Le shell rend le chrome de page.

```text
PublicShell = PublicHeader + main + PublicFooter
PrivateShell = PrivateTopAppBar + SidebarNav + main
```

---

## 6. Contrat runtime Page

Le runtime frontend doit recevoir une page prête à rendre.

### Public runtime

```json
{
  "meta": {},
  "theme": {},
  "shell": {
    "type": "public",
    "header": {
      "brand": {},
      "primary": [],
      "utilities": [],
      "actions": []
    },
    "footer": {
      "brand": {},
      "descriptionKey": "public.footer.description",
      "statusKey": "public.footer.status.operational",
      "copyrightKey": "app.footer.copyright",
      "columns": [],
      "social": []
    }
  },
  "content": {
    "layout": {
      "rows": []
    },
    "widgets": {}
  },
  "dynamic": {
    "widgets": {},
    "errors": []
  }
}
```

### Private runtime

```json
{
  "meta": {},
  "theme": {},
  "shell": {
    "type": "private",
    "topAppBar": {},
    "navigationDrawer": {
      "brand": {},
      "primary": [],
      "sections": [],
      "secondary": []
    }
  },
  "content": {
    "layout": {
      "rows": []
    },
    "widgets": {}
  },
  "dynamic": {
    "widgets": {},
    "errors": []
  }
}
```

Règle non négociable :

```text
Ne pas remettre PrivateShell dans shell.header.
La sidenav vient de shell.navigationDrawer.
```

---

## 7. Bindings backend vs runtime frontend

La DB peut contenir des bindings internes :

```json
{
  "binding": {
    "mode": "dynamic",
    "source": "jsonFile"
  },
  "props": {
    "fileKey": "public_footer_links"
  }
}
```

Mais l’API runtime frontend doit renvoyer le fragment résolu :

```json
{
  "footer": {
    "brand": {},
    "descriptionKey": "...",
    "columns": [],
    "social": []
  }
}
```

Règle :

```text
fileKey/jsonFile/binding sont des détails backend.
Ils ne doivent pas être nécessaires pour rendre côté Angular.
```

---

## 8. Contrat JSON

Les contrats backend/web utilisent **camelCase**.

Exemples :

```text
schemaVersion
logicalId
isDefault
tenantId
labelKey
titleKey
descriptionKey
activeMatch
reasonKey
fileKey
maxItems
showDates
includeHistory
```

Interdit comme cible durable :

```text
label_key + labelKey
schema_version + schemaVersion
file_key + fileKey
```

Les clés i18n peuvent garder leurs underscores dans les valeurs :

```text
public.nav.check_ticket
home.check_ticket.title
```

---

## 9. ActionItem : contrat unique action/navigation

`ActionItem` devient le contrat unique pour :

- header nav ;
- footer links ;
- sidebar ;
- overlay nav ;
- buttons ;
- CTA ;
- social links.

`TchLink` est legacy et doit disparaître progressivement.

Contrat cible :

```ts
export type ActionItemKind = 'button' | 'link' | 'externalLink' | string;

export interface ActionItem {
  readonly id: string;
  readonly kind?: ActionItemKind;
  readonly labelKey?: string;
  readonly label?: string | null;
  readonly destination?: NavigationDestination;
  readonly icon?: string | null;
  readonly image?: string | null;
  readonly activeMatch?: 'exact' | 'prefix' | string | null;
  readonly disabled?: boolean;
  readonly reasonKey?: string | null;
  readonly badge?: unknown;
  readonly children?: readonly ActionItem[];
}

export type NavigationDestinationKind = 'route' | 'url';

export interface NavigationDestination {
  readonly kind: NavigationDestinationKind;
  readonly value: string;
  readonly requiredRoles?: readonly string[];
}
```

Helpers communs :

```ts
export function actionText(item: ActionItem | undefined): string {
  return item?.labelKey ?? item?.label ?? '';
}

export function actionRoute(item: ActionItem | undefined): string {
  return item?.destination?.kind === 'route' ? item.destination.value : '';
}

export function actionHref(item: ActionItem | undefined): string {
  return item?.destination?.value ?? '#';
}

export function isExternalAction(item: ActionItem | undefined): boolean {
  return item?.destination?.kind === 'url';
}

export function isRouteAction(item: ActionItem | undefined): boolean {
  return item?.destination?.kind === 'route';
}
```

---

## 10. Surfaces applicatives

| Surface        | Route frontend cible | Rôle                                             |
| -------------- | -------------------: | ------------------------------------------------ |
| Public         |            `/public` | Résultats, vérification ticket, PageModel public |
| Cashier/POS    |      `/admin/pos` V0 | Vente, paiement, session caisse                  |
| Tenant Admin   |             `/admin` | Dashboard et configuration tenant                |
| Platform Admin |          `/platform` | Opérations plateforme                            |
| Auth           |  `/login`, callbacks | Authentification                                 |

Note importante :

```text
Les routes frontend ne sont pas forcément les scopes API backend.
Ex: /admin peut appeler /api/v1/admin/** ou /api/v1/tenant/** selon le cas.
```

---

## 11. Convention composants

```text
Route → Page → Container(s) → Component(s)
```

| Type      | Suffixe            | Règle                                                                                       |
| --------- | ------------------ | ------------------------------------------------------------------------------------------- |
| Page      | `*.page.ts`        | Obligatoire. Routée, layout principal, peut injecter services applicatifs/store/router      |
| Container | `*.container.ts`   | Recommandé. Jamais routé, orchestre une sous-zone                                           |
| Widget    | `*.widget.ts`      | Obligatoire. Rendu par PageModel, props/data uniquement                                     |
| Shell     | `*.shell.ts`       | Obligatoire. Structure globale d’une surface                                                |
| Component | nom court autorisé | Stateless/presentational, `input()`/`output()`. Suffixe `.component.ts` seulement si ambigu |

Exemples acceptés :

```text
loading.ts
error-panel.ts
brand.ts
nav.ts
overlay-nav.ts
sidebar-nav.ts
public-shell.shell.ts
public-home.page.ts
```

---

## 12. Règles non négociables

- Toutes les routes pointent vers une `Page`.
- Les composants UI ne font pas d’appel HTTP.
- Les composants UI ne dépendent pas de NgRx ni de services applicatifs.
- Les pages orchestrent des services applicatifs/state dédiés, sans appeler directement `HttpClient`.
- Les contrats HTTP génériques ciblent `libs/api/contracts`.
- Les contrats runtime PageModel ciblent `libs/page-model`.
- Les contrats actifs peuvent rester temporairement dans `apps/tch-portal/src/app/shared/types`.
- Pas de nouvelle lib sans frontière claire et stable.
- Pas de lib Nx vide créée uniquement pour correspondre au diagramme cible.
- PageModel ne gère pas le shell.
- Angular ne résout pas `fileKey/jsonFile` en runtime.
- Les styles consomment `--tch-*`.
- Les composants exposent des variables locales `--comp-*`.

---

## 13. Non-goals immédiats

Ne pas faire maintenant :

- un CMS complet ;
- un theme builder visuel ;
- une migration massive de tous les widgets ;
- un moteur de layout avancé ;
- des conditions complexes dans PageModel ;
- des appels frontend pour résoudre `fileKey` ;
- des hacks Material dans chaque composant ;
- une réorganisation Nx complète en une seule PR.
