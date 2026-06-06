# Web Naming Conventions — Tchalanet

> **Status**: DRAFT v0.2
> **Scope**: `tchalanet-web` / `apps/tch-portal` / `libs/**`
> **Goal**: noms prédictibles, searchable, cohérents avec l’architecture Angular/Nx Tchalanet.

---

## 1. Principes

* Les noms doivent être **searchable** et **prédictibles**.
* Le nom encode l’intention métier ou UI, pas l’implémentation accidentelle.
* Éviter les noms génériques : `common`, `helpers`, `misc`, `stuff`, `utils` sans contexte.
* Ne pas utiliser `Dto` comme suffixe par défaut côté frontend.
* Ne pas créer un deuxième nom pour le même concept.
* Ne pas créer une lib Nx vide uniquement pour correspondre à un diagramme cible.

---

## 2. Contrats backend/web

Les contrats backend/web vivent dans :

```text
libs/api/src/lib/contracts
```

Exception possédée par sa capacité : les contrats runtime PageModel vivent dans
`libs/page-model/src/lib/runtime`.

Exemples :

```text
api-response.ts
action-item.ts
navigation-destination.ts
problem-detail.ts
service-status.ts
```

Types recommandés :

```text
ActionItem
NavigationDestination
ApiResponse<T>
ApiNotice
ProblemDetail
TchPage<T>
ServiceStatus
```

Règle :

```text
ui/components peut consommer ActionItem.
ui/components ne possède pas ActionItem.
```

`TchLink` est legacy. Les nouvelles navigations/actions doivent utiliser `ActionItem`.

---

## 3. Style JSON backend/web

Les champs du contrat JSON utilisent **camelCase**.

Correct :

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

À éviter comme cible durable :

```text
schema_version
logical_id
is_default
tenant_id
label_key
title_key
file_key
```

Les valeurs i18n peuvent garder leurs underscores :

```text
public.nav.check_ticket
home.check_ticket.title
dashboard.tenant_admin.kpis.title
```

---

## 4. Features applicatives

Dans `apps/tch-portal/src/app/features`, organiser par surface puis feature :

```text
features/<surface>/<feature>
```

Exemples :

```text
features/public/home
features/public/results
features/public/check-ticket
features/cashier/dashboard
features/cashier/sale
features/admin/dashboard
features/admin/outlets
features/admin/users
features/platform/dashboard
features/platform/page-models
```

Fichiers recommandés :

```text
home.routes.ts
home.page.ts
home.container.ts
result-list.container.ts
result-card.ts
```

Règle :

```text
Route → Page → Container(s) → Component(s)
```

---

## 5. Pages, containers, components

### Page

Suffixe obligatoire :

```text
*.page.ts
```

Une page est routée. Elle peut injecter :

* router ;
* services applicatifs ;
* stores ;
* API services ;
* bootstrap/runtime services.

Exemples :

```text
public-home.page.ts
tenant-admin-dashboard.page.ts
cashier-dashboard.page.ts
not-found.page.ts
```

### Container

Suffixe recommandé :

```text
*.container.ts
```

Un container n’est pas routé. Il orchestre une sous-zone de page.

Exemples :

```text
ticket-search.container.ts
tenant-readiness.container.ts
cashier-session.container.ts
```

### Component

Les composants UI simples peuvent garder un nom court.

Exemples acceptés :

```text
loading.ts
error-panel.ts
field-error.ts
brand.ts
nav.ts
overlay-nav.ts
sidebar-nav.ts
status-badge.ts
empty-state.ts
```

Utiliser `*.component.ts` seulement si le nom est ambigu ou si le dossier contient plusieurs classes proches.

Exemples :

```text
public-header.component.ts
public-footer.component.ts
```

ou, si le dossier est déjà clair :

```text
public-header.ts
public-footer.ts
```

Règle projet :

```text
Les composants UI purs sont stateless, basés sur input()/output(), sans appel HTTP.
```

---

## 6. Shells

Un shell structure une surface entière.

Suffixe recommandé :

```text
*.shell.ts
```

Exemples :

```text
public-shell.shell.ts
private-shell.shell.ts
cashier-shell.shell.ts
```

Les shells rendent le chrome de page :

```text
PublicShell = PublicHeader + main + PublicFooter
PrivateShell = PrivateTopAppBar + SidebarNav + main
```

Le PageModel ne rend pas le shell.

---

## 7. PageModel

Lib active :

```text
libs/page-model
```

Responsabilité :

```text
contrat runtime PageModel
renderer rows/columns/widgets
WidgetHost abstrait
helpers de rendu
```

Noms recommandés :

```text
page-model-renderer.ts
widget-host.ts
page-layout.ts
page-model.types.ts
```

Le PageModel ne doit pas posséder :

* widgets concrets ;
* PublicHeader ;
* PublicFooter ;
* PrivateShell ;
* SidebarNav ;
* runtime theme ;
* i18n bootstrap.

---

## 8. Widgets

Lib active :

```text
libs/widgets
```

Un widget est rendu par PageModel.

Suffixe obligatoire :

```text
*.widget.ts
```

Exemples :

```text
hero.widget.ts
kpi-grid.widget.ts
alerts.widget.ts
quick-actions.widget.ts
public-draw-results.widget.ts
ticket-verification.widget.ts
```

Types :

```text
HeroWidgetProps
KpiGridWidgetProps
AlertsWidgetProps
```

Règle :

```text
Widget = props + data.
Widget ne fait pas d’appel HTTP direct.
```

---

## 9. Data access / API

Pour les services applicatifs réutilisables, utiliser une frontière explicite.

Dans une feature locale :

```text
features/admin/outlets/outlets-api.service.ts
features/admin/outlets/outlets.store.ts
```

Dans une lib cible future :

```text
libs/api/src/lib/http
libs/api/src/lib/contracts
```

Suffixes recommandés :

```text
XxxApiService
XxxRequest
XxxResponse
XxxItem
XxxDetails
XxxSummary
XxxView
XxxStore
```

Éviter :

```text
Dto
Data
Payload
Model
```

sauf si le terme a un sens précis dans le contrat.

`Payload` reste acceptable pour certains fragments PageModel si c’est le nom métier du contrat, par exemple :

```text
HeroPayload
PublicFooterPayload
PrivateShellPayload
```

---

## 10. Stores

### Stores globaux

```text
AuthSessionStore
LocaleStore
RuntimeConfigStore
ThemeStore
```

### Stores feature

```text
PayoutsStore
TenantDashboardStore
PageModelEditorStore
CashierSaleStore
```

### Stores cache/référentiel

```text
TenantConfigStore
CatalogCacheStore
ThemePresetStore
```

À éviter :

```text
BaseStore
CrudStore
GenericStore
```

Sauf besoin répété, documenté et testé.

---

## 11. UI libs

### `libs/ui/components`

Composants réutilisables :

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

Pas de contrat propriétaire ici. Les contrats viennent de `libs/api/contracts`.

### `libs/ui/styles`

SCSS primitives :

```text
_breakpoints.scss
_functions.scss
_mixins.scss
_typography.scss
_overlay.scss
_material-overrides.scss
_index.scss
```

### `libs/ui/theme`

Runtime theme :

```text
theme-types.ts
theme-store.ts
theme-dom-applier.ts
theme-switcher.ts
theme-preset-registry.ts
```

---

## 12. SCSS et tokens

Les composants consomment les tokens globaux :

```text
--tch-color-surface
--tch-color-on-surface
--tch-color-primary
--tch-color-on-primary
--tch-color-outline
--tch-radius-md
--tch-elevation-1
--tch-focus-ring-width
--tch-font-family
```

Chaque composant expose des variables locales :

```text
--comp-footer-bg
--comp-footer-fg
--comp-nav-gap
--comp-sidebar-width
```

Règle :

```text
--tch-* = tokens globaux du thème
--comp-* = variables locales d’un composant
```

---

## 13. Helpers

Éviter les dossiers génériques :

```text
helpers
utils
common
misc
```

Préférer un nom d’intention :

```text
navigation.helpers.ts
page-model-normalizers.ts
theme-token-mapper.ts
api-error.mapper.ts
```

Les helpers de navigation pour `ActionItem` doivent être centralisés :

```text
actionText
actionRoute
actionHref
isExternalAction
isRouteAction
```

---

## 14. Anti-patterns

Éviter :

```text
shared/data-access
shared/facades
shared/components
ui/payout-card
feature-home-public
feature-home-private
common/helpers
misc/utils
BaseStore
CrudStore
GenericStore
```

Préférer :

```text
features/public/home
features/admin/dashboard
features/cashier/sale
libs/ui/components/card
libs/api/contracts/action-item.ts
libs/api/http
navigation.helpers.ts
```
