# tch-portal — Architecture Frontend

> **Statut** : Target architecture — migration en cours
> **App** : `apps/tch-portal/` — Angular / Nx
> **Objectif** : garder une architecture frontend lisible, slice-first, sans créer de libs vides ni transformer PageModel en usine à gaz.

---

## 1. Objectif

`tch-portal` doit permettre de construire :

* la page publique ;
* les dashboards privés ;
* les shells public / privé ;
* les widgets dynamiques ;
* les thèmes runtime ;
* les composants UI réutilisables ;

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
│   └── tch-portal/
│       └── src/app/
│           ├── core/       ← auth, HTTP, i18n, runtime, settings, PageModel API
│           ├── features/   ← pages, shells, renderer PageModel et widgets actifs
│           └── shared/     ← contrats encore locaux à l'application
└── libs/
    ├── api/
    │   └── src/lib/
    │       ├── contracts/  ← contrats backend/web transverses
    │       └── http/       ← clients HTTP, interceptors, helpers API
    ├── shared-config/      ← settings runtime et feature flags
    └── ui/
        ├── components/     ← composants UI réutilisables et stateless
        ├── styles/         ← primitives SCSS compile-time
        └── theme/          ← thème runtime, presets Material 3 et tokens
```

Cette structure est volontairement petite. Chaque lib active doit porter une frontière déjà utilisée.

---

## 3. Rôle des libs actives

### `libs/api`

Responsabilité :

* contrats backend/web ;
* types HTTP communs ;
* helpers API ;
* interceptors ;
* futurs clients HTTP transverses.

Exemples de contrats à centraliser ici :

```text
ApiResponse
ApiNotice
ProblemDetail model côté frontend
ActionItem
NavigationDestination
PageModel runtime contracts
TchPage
ServiceStatus
```

Règle importante :

```text
ui/components peut consommer ActionItem.
ui/components ne possède pas ActionItem.
```

Les nouveaux contrats backend/frontend doivent cibler `libs/api/src/lib/contracts`.

Les contrats actifs peuvent rester temporairement dans `apps/tch-portal/src/app/shared/types`, mais uniquement pendant migration.

---

### `libs/shared-config`

Responsabilité :

* settings runtime ;
* feature flags ;
* configuration frontend runtime.

Cette lib ne doit pas contenir de composants UI.

---

### `libs/ui/components`

Responsabilité :

* composants UI réutilisables ;
* composants stateless ;
* composants basés sur `input()` / `output()` ;
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

Ces composants consomment des contrats partagés, mais ne les possèdent pas.

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

* thème runtime ;
* sélection light/dark ;
* ThemeDomApplier ;
* application des CSS variables ;
* synchronisation Angular Material OverlayContainer ;
* presets ;
* tokens ;
* future génération/build de thème ;
* future intégration tenant theme.

Cette lib porte le `README.md`/guide de thème.

Décisions à conserver :

```text
Theme runtime applique les tokens.
Shared styles consomme les tokens.
Components exposent des variables locales --comp-*.
```

---

## 4. Structure cible par extraction

```text
libs/
  api/             contrats backend/web, clients HTTP et interceptors
  shared-auth/     OIDC/Keycloak, session et guards
  shared-i18n/     traduction runtime et sélection de langue
  shared-config/   feature flags, settings et configuration runtime
  ui/              components, styles et theme
  page-model/      moteur de layout/rendu PageModel, sans widgets concrets
  widgets/         registry et widgets dynamiques
  web/             routes, pages, containers et shells par surface
```

Ces libs sont des **cibles de migration**, pas des dossiers à créer à vide.

Une lib est créée seulement lorsqu’un change :

* déplace un slice cohérent ;
* définit ses exports publics ;
* valide ses dépendances Nx ;
* supprime ou réduit une dépendance depuis `apps/tch-portal`.

Ordre recommandé :

1. `shared-auth` / `shared-i18n`
2. `page-model`
3. `widgets`
4. `web`

`page-model` doit précéder `widgets`.

```text
page-model = contrat runtime + renderer abstrait + WidgetHost abstrait
widgets = registry + widgets concrets
web = routes, pages, shells et containers par surface
```

---

## 5. PageModel : frontière

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
* theme runtime ;
* i18n bootstrap ;
* résolution fileKey/jsonFile.

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
  "data": {
    "widgets": {}
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
  "data": {
    "widgets": {}
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

* header nav ;
* footer links ;
* sidebar ;
* overlay nav ;
* buttons ;
* CTA ;
* social links.

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
| Cashier/POS    |       `/app/cashier` | Vente, paiement, session caisse                  |
| Tenant Admin   |         `/app/admin` | Dashboard et configuration tenant                |
| Platform Admin |      `/app/platform` | Opérations plateforme                            |
| Auth           |  `/login`, callbacks | Authentification                                 |

Note importante :

```text
Les routes frontend ne sont pas forcément les scopes API backend.
Ex: /app/admin peut appeler /api/v1/admin/** ou /api/v1/tenant/** selon le cas.
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

* Toutes les routes pointent vers une `Page`.
* Les composants UI ne font pas d’appel HTTP.
* Les composants UI ne dépendent pas de NgRx ni de services applicatifs.
* Les pages orchestrent des services applicatifs/state dédiés, sans appeler directement `HttpClient`.
* Les contrats backend/frontend ciblent `libs/api/contracts`.
* Les contrats actifs peuvent rester temporairement dans `apps/tch-portal/src/app/shared/types`.
* Pas de nouvelle lib sans frontière claire et stable.
* Pas de lib Nx vide créée uniquement pour correspondre au diagramme cible.
* PageModel ne gère pas le shell.
* Angular ne résout pas `fileKey/jsonFile` en runtime.
* Les styles consomment `--tch-*`.
* Les composants exposent des variables locales `--comp-*`.

---

## 13. Non-goals immédiats

Ne pas faire maintenant :

* un CMS complet ;
* un theme builder visuel ;
* une migration massive de tous les widgets ;
* un moteur de layout avancé ;
* des conditions complexes dans PageModel ;
* des appels frontend pour résoudre `fileKey` ;
* des hacks Material dans chaque composant ;
* une réorganisation Nx complète en une seule PR.
