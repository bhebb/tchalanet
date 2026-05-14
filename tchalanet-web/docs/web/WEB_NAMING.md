# Web Naming Conventions — Tchalanet

> Status: DRAFT v0.1

## 1. Principes

- Les noms doivent être searchable et prédictibles.
- Le nom encode l'intention, pas l'implémentation accidentelle.
- Éviter les noms génériques : `common`, `helpers`, `misc`, `stuff`.
- Ne pas utiliser `Dto` comme suffixe par défaut.

## 2. Features

Dossier :

```text
features/<scope>/<feature>
```

Exemples :

```text
features/public/home
features/tenant/payouts
features/admin/outlets
features/platform/page-models
```

Fichiers :

```text
payouts.routes.ts
payouts.page.ts
payouts.store.ts
payout-list.component.ts
payout-detail-panel.component.ts
```

## 3. Data access

```text
data-access/<domain>/model
data-access/<domain>/api
data-access/<domain>/state
```

Suffixes :

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

## 4. UI

Composants purs :

```text
ui/components/button
ui/components/card
ui/layout/app-header
ui/layout/sidebar
```

Suffixes :

```text
XxxComponent
```

Pour composants très spécifiques, nommer selon l'intention visuelle :

```text
EmptyStateComponent
StatusBadgeComponent
PageContainerComponent
```

## 5. Stores

- State global : `AuthSessionStore`, `LocaleStore`, `RuntimeConfigStore`.
- State feature : `PayoutsStore`, `PageModelEditorStore`.
- State API réutilisable : `TenantConfigStore`, `CatalogCacheStore`.

Éviter :

```text
BaseStore
CrudStore
GenericStore
```

sauf besoin répété et documenté.

## 6. Routes et pages

```text
home.routes.ts
home.page.ts
payouts.routes.ts
payouts.page.ts
```

## 7. Anti-patterns

```text
shared/data-access
shared/facades
shared/components
ui/payout-card si payout est métier
feature-home-public
feature-home-private
```

Préférer :

```text
features/public/home
features/tenant/home
ui/components/card
data-access/payout
```
