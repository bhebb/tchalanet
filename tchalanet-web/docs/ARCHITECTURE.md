# Tchalanet Web — Architecture Frontend

> **Statut** : Architecture active — extraction progressive en cours
> **Apps** : `apps/public-portal/`, `apps/admin-portal/`, `apps/platform-portal/` — Angular / Nx
> **Objectif** : garder une architecture frontend lisible, slice-first, sans créer de libs vides ni transformer PageModel en usine à gaz.
> **Dernier état des lieux** : 2026-07-02 (usages mesurés sur le code réel).

---

## 1. Objectif

`tchalanet-web` doit permettre de construire :

- la surface publique ;
- les consoles privées (tenant admin, plateforme, POS) ;
- les shells public / privé ;
- les widgets dynamiques ;
- les thèmes runtime ;
- les composants UI réutilisables ;
- plusieurs apps déployables indépendamment quand la surface le justifie ;

tout en gardant une règle simple :

```text
Le backend prépare une page prête à rendre (surface publique).
Angular rend le shell, le layout et les widgets.
Angular ne résout pas les fileKey/jsonFile.
Angular ne connaît pas les bindings backend internes.
```

Le PageModel n’est pas un CMS complet.
C’est un **contrat de composition de page prêt-à-rendre**.

---

## 2. Deux paradigmes de rendu — où on est

La vraie structure n’est pas « 3 apps », c’est **2 paradigmes de rendu** :

| Paradigme | Surfaces | Moteur | Qui décrit la page |
| --------- | -------- | ------ | ------------------ |
| **PageModel-driven** | `public-portal` | `@tch/page-model` + `@tch/widgets` | Le backend (PageRuntimeResponse) |
| **Console-driven** | `admin-portal` (50 pages + POS), `platform-portal` (40 pages) | `@tch/ui/console` + `@tch/ui/components` | Le frontend (pages Angular classiques) |

Preuve par la matrice d’imports (2026-07-02) :

```text
public-portal   : page-model 11× · ui/components 9× · api 8× — jamais ui/console
admin-portal    : ui/console 113× · api 103× · web/errors 73× · ui/components 57×
platform-portal : ui/console 139× · web/errors 63× · ui/components 58× · api 55×
```

Cette séparation est saine et voulue. Le socle console (`@tch/ui/console`) est le
design system de facto des surfaces privées ; il est documenté ici et opérationnalisé
dans [`conventions/feature-playbook.md`](./conventions/feature-playbook.md).

### Où on va — chantiers actifs

| # | Chantier | Motivation |
| - | -------- | ---------- |
| C1 | `ui/console` documenté et adopté comme DS console unique | 252 imports réels, 0 ligne de doc avant 2026-07 |
| C2 | Brique **table + pagination** dans `ui/console` | Chaque liste recode `mat-table` + prev/next ; refonte ergonomique = ~20 pages touchées |
| C3 | i18n consoles : `TranslatePipe` obligatoire sur toute nouvelle page, résorption des pages FR hardcodées | Décision 2026-07-02 ; l’archi i18n multi-bundles est le socle, minimiser la duplication de clés |
| C4 | Nettoyage : supprimer `libs/web` racine (0 usage), kit `admin-crud` mort dans `ui/components`, dialogs orphelins | Deux kits CRUD concurrents créent de l’ambiguïté |
| C5 | Foyer « domaine partagé inter-consoles » : rapatrier `apps/*/shared/{lottery,results}` (dupliqués **et divergés** entre admin et platform) | `libs/web/console` en est l’embryon, mal nommé |
| C6 | `pos-portal` : extraction quand la surface a son propre cycle de build/deploy | POS reste lazy dans `admin-portal` en V0 |

Principe d’ergonomie central :

```text
Une page console ne contient que de l’orchestration + des briques console.
Si changer l’apparence d’une liste demande de toucher plus que libs/ui/console,
c’est une brique manquante — on l’ajoute au DS, pas à la page.
```

---

## 3. Structure active

```text
tchalanet-web/
├── apps/
│   ├── public-portal/     ← app publique, SSR/hydration-ready, surface publique canonique
│   ├── admin-portal/      ← app tenant admin + POS lazy, CSR V0, déployable seule
│   ├── platform-portal/   ← app plateforme/superadmin, CSR V0, déployable seule
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
    │   ├── auth/          ← login partagé, auth/session/guards
    │   └── i18n/          ← runtime i18n partagé
    ├── notifications/      ← notifications privées réutilisables
    ├── page-model/         ← contrats runtime, API, renderer et registre abstrait
    ├── shared-assets/      ← assets statiques partagés servis en /assets/**
    ├── shared-config/      ← settings runtime et feature flags
    ├── web/                ← façade racine historique (0 usage) — suppression planifiée (C4)
    │   ├── console/        ← data-access métier inter-consoles (embryon C5, à renommer)
    │   ├── errors/         ← modèle/copy/routing UI des erreurs web
    │   ├── sandbox/        ← pages/outils de dev : theme sandbox, debug UI
    │   └── shell/          ← primitives shell web partagées
    ├── widgets/            ← registre concret et widgets PageModel
    └── ui/
        ├── components/     ← primitives UI transverses stateless
        ├── console/        ← DESIGN SYSTEM des consoles admin/platform
        ├── styles/         ← primitives SCSS compile-time
        └── theme/          ← thème runtime, presets Material 3 et tokens
```

Cette structure reste volontairement petite. Chaque lib active porte une frontière réelle. Ne pas
créer de sous-lib Nx pour `api/contracts`, `api/http` ou `api/backend-client` : `libs/api` reste une
seule lib avec des dossiers internes.

`pos-portal` n'existe pas en V0. La vente POS reste lazy-loaded dans `admin-portal` jusqu'à ce que la
surface justifie une app séparée (C6).

Les tests end-to-end Angular/Web vivent dans un seul projet Playwright :

```text
apps/web-e2e/src/
  public-portal/    http://localhost:4301
  admin-portal/     http://localhost:4302
  platform-portal/  http://localhost:4303
```

---

## 4. Carte des briques — rôle, statut, usage

Statuts : ✅ canonique · 🟡 actif à faire évoluer · ⚠️ legacy à réduire · 💀 mort à supprimer.

| Brique | Rôle réel | Statut | Usage mesuré (2026-07) |
| ------ | --------- | ------ | ---------------------- |
| `@tch/ui/console` | Design system console : page-shell, section-card, detail-layout (main+aside), crud-shell, empty-state, identity-card, routes account/profile | ✅ | 252 imports (admin 113, platform 139) |
| `@tch/ui/components` | Primitives transverses : loading, error-panel, status-badge, confirm-dialog, action-button, field-error, list-surface, notice… | ✅ (partie vivante) | 124 imports |
| `@tch/ui/components` → dossier `admin-crud` (data-table, form-shell, list-toolbar, mobile-card-list, form-actions) | Kit CRUD jamais adopté, supplanté par `ui/console` + `mat-table` | 💀 (C4) | 0 usage |
| `@tch/api` | Contrats génériques + `TchBackendClient` + ProblemDetail | ✅ | 166 imports |
| `@tch/web/errors` | Modèle d’erreur 3 niveaux + copy resolver | ✅ | 136 imports |
| `@tch/web/shell` | Shells public/privé réutilisables | ✅ | wired 1× par app |
| `@tch/page-model` | Moteur de rendu public piloté backend | ✅ | 14 imports (public surtout) |
| `@tch/widgets` | Widgets concrets PageModel | ✅ | registre synchrone |
| `@tch/ui/theme` / `@tch/ui/styles` | Thème runtime M3 / primitives SCSS | ✅ | transverse |
| `@tch/shared-config` / `@tch/shared-assets` | Settings runtime, flags / assets statiques | ✅ | transverse |
| `@tch/core/auth` / `@tch/core/i18n` | Session, guards, login / locale runtime | ✅ | transverse |
| `@tch/web` (façade racine) | Ancienne façade auth/i18n/errors/shell | 💀 (C4) | 0 import |
| `@tch/web/console` | 2 fichiers data-access draw-lifecycle ; collision de nom avec `ui/console` | 🟡 (C5 : devenir le foyer domaine inter-consoles, renommé) | 2 imports |
| `@tch/web/sandbox` | Outils dev (theme sandbox, debug) | ✅ | 3 imports |
| `apps/*/src/app/shared/{lottery,results}` | Mapping jeux Haïti + affichage lots — **dupliqué et divergé** entre admin et platform | ⚠️ (C5) | 2 copies |

### `libs/ui/console` — le DS console

Responsabilité :

- `AdminPageShellComponent` (`tch-admin-page-shell`) : racine de toute page console — `[title]`, `[description]`, slots `[meta]`, `[actions]`, contenu ;
- `AdminSectionCardComponent` : carte de section (`[title]`, `[icon]`, `[description]`) ;
- `AdminDetailLayoutComponent` : layout détail — slots `[main]`, `[footer]`, `[aside]` (la « partie de droite ») ;
- `AdminCrudShellComponent` : zones `[toolbar]`, `[content]`, `[footer]` d’une liste ;
- `AdminEmptyStateComponent`, `TchIdentityCard`, `AdminDataToolbar`, next-steps/health cards ;
- routes partagées account/activation/profile.

Ne contient pas : services API métier, stores métier, pages routées métier.

À venir (C2) : brique table + pagination console pour supprimer la duplication `mat-table` dans ~20 pages.

### `libs/ui/components`

Responsabilité :

- composants UI réutilisables, stateless, `input()`/`output()` ;
- aucun appel HTTP, aucune logique métier applicative.

Partie vivante :

```text
loading · error-panel · page-error · field-error · section-error · notice
status-badge · confirm-dialog · action-button · submit-button
admin-list-surface · search-select · multi-search-select
brand · nav · overlay-nav · sidebar-nav · lang-switcher · lang-theme-group
card · empty-state · section-header · user-menu · breakpoints
```

Partie deprecated (C4, ne plus utiliser) : `admin-crud/` (`AdminDataTable`, `AdminFormShell`,
`AdminListToolbar`, `AdminMobileCardList`, `AdminFormActions`, `AdminStatusPill`).

Ces composants consomment des contrats partagés, mais ne les possèdent pas.

### `libs/api`

Responsabilité :

- contrats backend/web génériques (`ApiResponse`, `ApiNotice`, `ProblemDetail`, `ActionItem`,
  `NavigationDestination`, `TchPage`, `ServiceStatus`) ;
- helpers/interceptors HTTP ;
- `TchBackendClient`, wrapper technique autour de `HttpClient`.

Règle importante :

```text
ui/components peut consommer ActionItem.
ui/components ne possède pas ActionItem.
```

**Les services API métier ne vivent PAS ici.** La règle réelle (46 services en features vs 4 dans
`libs/api`) :

```text
Contrats génériques + client technique  -> libs/api
Service API d'une feature               -> features/<surface>/<feature>/data-access/
Extraction vers une lib                 -> seulement au 2e consommateur réel inter-apps
```

### `libs/page-model`

Responsabilité :

- contrats `PageRuntimeResponse` et types associés ;
- client `PageModelApi` ;
- renderer rows/columns et `WidgetHostComponent` ;
- fallbacks de widgets contenus ;
- token injectable abstrait `WIDGET_REGISTRY` ;
- helpers communs aux widgets et `LabelPipe`.

Cette lib ne dépend jamais de `libs/widgets`.

### `libs/widgets`

Responsabilité :

- widgets PageModel concrets ;
- mapping direct type backend → composant Angular ;
- provider `provideWidgets()`, activé par l'app composition root.

Cette lib dépend de `libs/page-model`.

### `libs/core/auth`

Responsabilité : login partagé, session store, guards, permissions, état d'authentification.
Aucun client métier admin, platform ou POS.

### `libs/core/i18n`

Responsabilité : contrats et état de locale partagés, bootstrap i18n réutilisable, helpers runtime.
Les écrans métier d'administration i18n restent dans leur feature platform/admin.

**Décision 2026-07-02 — i18n obligatoire sur les 3 apps.** Toute nouvelle page (publique comme
console) passe par `TranslatePipe` et les bundles de traduction par surface. Le texte FR codé en dur
présent dans les consoles (~80 % des pages en 2026-07) est une dette à résorber, pas un régime
accepté. Minimiser la duplication de clés entre bundles.

### `libs/shared-assets`

Responsabilité : logos, icônes, fonts, images publiques, markdown partagés, bundles JSON i18n
locaux de fallback, constantes de chemins `/assets/**`. Chaque app copie
`libs/shared-assets/public` dans son browser output. Ni secrets runtime, ni store i18n, ni shell.

### `libs/shared-config`

Responsabilité : settings runtime, feature flags, configuration frontend runtime. Pas d'UI.

### `libs/web/errors`

Responsabilité :

- modèle d'erreur web consommable par les pages ;
- mapping copy/traduction depuis les codes backend stables ;
- helpers de routage shell/page/section/field ;
- helpers field error côté formulaire.

Elle ne possède pas `HttpClient` et ne promet pas de workflow support/ticket.

### `libs/web/shell`

Responsabilité : primitives shell web partagées, feedback shell routable, actions shell qui
préservent la surface courante. La navigation reste dans shell en V0 ; ne pas créer
`libs/web/navigation` sans frontière réelle.

### `libs/web/console` (C5)

Aujourd'hui : 2 fichiers de data-access draw-lifecycle. Cible : devenir (renommé) le foyer du
**métier partagé entre consoles** — mapping lottery/jeux Haïti, affichage lots/résultats — pour
résorber la duplication divergée `apps/{admin,platform}-portal/src/app/shared/`. Ne pas y ajouter
de nouveaux contenus tant que le renommage n'est pas acté.

### `libs/web` (racine) — 💀

Façade historique auth/i18n/errors/shell. **0 import.** À supprimer avec son alias `@tch/web` (C4).

### `libs/ui/styles`

Primitives SCSS compile-time : `_breakpoints`, `_functions`, `_mixins`, `_typography`, `_overlay`,
`_material-overrides`, `_index`. Cette lib ne décide jamais du thème courant.

### `libs/ui/theme`

Responsabilité : thème runtime, light/dark, `ThemeDomApplier`, CSS variables, sync Material
OverlayContainer, presets, tokens, future génération de thème tenant.

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

## 5. Graphe de dépendances actif

```text
apps/*      -> core/auth, core/i18n, web/errors, web/shell, ui/*, api, page-model, widgets
widgets     -> page-model
web/console -> api
web/errors  -> api
web/shell   -> ui/components, page-model selon besoin
page-model  -X-> widgets
ui/console  -X-> services API métier / stores métier
ui          -X-> api clients métier / features
```

Une lib est créée seulement lorsqu’un change :

- déplace un slice cohérent ;
- définit ses exports publics ;
- valide ses dépendances Nx ;
- supprime ou réduit une dépendance depuis une app.

---

## 6. PageModel : frontière

Le PageModel rend uniquement le contenu.

```text
PageModel = rows / columns / widgets
```

Il ne possède pas :

- PublicHeader / PublicFooter ;
- PrivateShell / SidebarNav / TopAppBar ;
- theme runtime ;
- i18n bootstrap ;
- résolution fileKey/jsonFile.

Le shell rend le chrome de page.

```text
PublicShell = PublicHeader + main + PublicFooter
PrivateShell = PrivateTopAppBar + SidebarNav + main
```

---

## 7. Contrat runtime Page

Le runtime frontend doit recevoir une page prête à rendre.

### Public runtime

```json
{
  "meta": {},
  "theme": {},
  "shell": {
    "type": "public",
    "header": { "brand": {}, "primary": [], "utilities": [], "actions": [] },
    "footer": {
      "brand": {},
      "descriptionKey": "public.footer.description",
      "statusKey": "public.footer.status.operational",
      "copyrightKey": "app.footer.copyright",
      "columns": [],
      "social": []
    }
  },
  "content": { "layout": { "rows": [] }, "widgets": {} },
  "dynamic": { "widgets": {}, "errors": [] }
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
    "navigationDrawer": { "brand": {}, "primary": [], "sections": [], "secondary": [] }
  },
  "content": { "layout": { "rows": [] }, "widgets": {} },
  "dynamic": { "widgets": {}, "errors": [] }
}
```

Règle non négociable :

```text
Ne pas remettre PrivateShell dans shell.header.
La sidenav vient de shell.navigationDrawer.
```

---

## 8. Bindings backend vs runtime frontend

La DB peut contenir des bindings internes :

```json
{
  "binding": { "mode": "dynamic", "source": "jsonFile" },
  "props": { "fileKey": "public_footer_links" }
}
```

Mais l’API runtime frontend doit renvoyer le fragment résolu :

```json
{
  "footer": { "brand": {}, "descriptionKey": "...", "columns": [], "social": [] }
}
```

Règle :

```text
fileKey/jsonFile/binding sont des détails backend.
Ils ne doivent pas être nécessaires pour rendre côté Angular.
```

---

## 9. Contrat JSON

Les contrats backend/web utilisent **camelCase**.

```text
schemaVersion · logicalId · isDefault · tenantId · labelKey · titleKey
descriptionKey · activeMatch · reasonKey · fileKey · maxItems · showDates · includeHistory
```

Interdit comme cible durable : `label_key`, `schema_version`, `file_key` en doublon des camelCase.

Les clés i18n peuvent garder leurs underscores dans les valeurs :

```text
public.nav.check_ticket
home.check_ticket.title
```

---

## 10. ActionItem : contrat unique action/navigation

`ActionItem` est le contrat unique pour : header nav, footer links, sidebar, overlay nav, buttons,
CTA, social links. `TchLink` est legacy et doit disparaître progressivement.

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

Helpers communs centralisés : `actionText`, `actionRoute`, `actionHref`, `isExternalAction`,
`isRouteAction`.

---

## 11. Surfaces applicatives

| Surface        | Route frontend cible | Rôle                                             |
| -------------- | -------------------: | ------------------------------------------------ |
| Public         |            `/public` | Résultats, vérification ticket, PageModel public |
| Cashier/POS    |      `/admin/pos` V0 | Vente, paiement, session caisse                  |
| Tenant Admin   |             `/admin` | Dashboard et configuration tenant                |
| Platform Admin |          `/platform` | Opérations plateforme                            |
| Auth           |  `/login`, callbacks | Authentification                                 |

```text
Les routes frontend ne sont pas forcément les scopes API backend.
Ex: /admin peut appeler /api/v1/admin/** ou /api/v1/tenant/** selon le cas.
```

---

## 12. Convention composants

Conventions **alignées sur le code réel** (2026-07-02) :

```text
Route → Page → Component(s) de feature
```

| Type | Suffixe | Règle |
| ---- | ------- | ----- |
| Page | `*.page.ts` (+ `.html`/`.scss` séparés) | Obligatoire. Routée, orchestre : route params, appels API, signals d'état, navigation |
| Component de feature | `*.component.ts` dans `<feature>/components/` | Sous-bloc UI d'une feature : inputs/outputs, pas d'appel API |
| Dialog | `*.dialog.ts` | Ouvert via `MatDialog`, données par `MAT_DIALOG_DATA` |
| Drawer | `*-drawer.component.ts` | Panneau latéral contextuel rendu par la page via `@if (selected())` |
| Store | `*.store.ts` | Optionnel — seulement quand la page dépasse l'orchestration simple (cf. state-management.md) |
| Widget | `*.widget.ts` | Rendu par PageModel, props/data uniquement |
| Shell | `*-shell.component.ts` | Structure globale d'une surface |
| Component UI lib | nom court (`loading.ts`, `status-badge.ts`) | Stateless, `input()`/`output()` ; `.component.ts` seulement si ambigu |

> Historique : les suffixes `*.container.ts` et `*.shell.ts` étaient prescrits mais n'ont jamais
> été adoptés (0 fichier). Ils ne sont plus des conventions. Ne pas les introduire.

Le guide opérationnel « t'as une liste / un détail / une création → voici le squelette » est dans
[`conventions/feature-playbook.md`](./conventions/feature-playbook.md).

---

## 13. Règles non négociables

- Toutes les routes pointent vers une `Page`.
- Toute page console a `tch-admin-page-shell` pour racine.
- Les composants UI ne font pas d’appel HTTP.
- Les composants UI ne dépendent pas de NgRx ni de services applicatifs.
- Les pages orchestrent des services applicatifs/state dédiés, sans appeler directement `HttpClient`.
- Tout texte visible passe par i18n (`TranslatePipe` + bundles par surface) — décision 2026-07-02.
- Les contrats HTTP génériques ciblent `libs/api/contracts` ; les services API métier restent dans `data-access/` de leur feature.
- Les contrats runtime PageModel ciblent `libs/page-model`.
- Pas de nouvelle lib sans frontière claire et stable.
- Pas de lib Nx vide créée uniquement pour correspondre au diagramme cible.
- Ne pas utiliser le kit `admin-crud` deprecated de `ui/components`.
- PageModel ne gère pas le shell.
- Angular ne résout pas `fileKey/jsonFile` en runtime.
- Les styles consomment `--tch-*` ; les composants exposent des variables locales `--comp-*`.

---

## 14. Non-goals immédiats

Ne pas faire maintenant :

- un CMS complet ;
- un theme builder visuel ;
- une migration massive de tous les widgets ;
- un moteur de layout avancé ;
- des conditions complexes dans PageModel ;
- des appels frontend pour résoudre `fileKey` ;
- des hacks Material dans chaque composant ;
- une réorganisation Nx complète en une seule PR.
