# Nx Boundaries — Tchalanet Web

> Status: DRAFT v0.1

## 1. Purpose

Nx est utilisé comme garde-fou, pas comme architecture en soi.

```text
L'architecture vient de Tchalanet.
Nx sert à l'imposer.
```

## 2. Tags recommandés

Chaque lib Nx devrait recevoir deux dimensions de tags :

```text
type:<role>
scope:<area>
```

### 2.1 Types

```text
type:core
type:feature
type:data-access
type:ui
type:shared
```

Optionnels plus tard :

```text
type:e2e
type:testing
type:config
```

### 2.2 Scopes

Produit :

```text
scope:public
scope:tenant
scope:admin
scope:platform
```

Technique ou partagé :

```text
scope:shared
scope:core
```

Domaine API :

```text
scope:payout
scope:sales
scope:draw
scope:tenant-config
scope:page-model
scope:catalog
```

## 3. Exemples de tags

```json
{
  "name": "core-auth",
  "tags": ["type:core", "scope:core"]
}
```

```json
{
  "name": "data-access-payout",
  "tags": ["type:data-access", "scope:payout"]
}
```

```json
{
  "name": "ui-layout",
  "tags": ["type:ui", "scope:shared"]
}
```

```json
{
  "name": "features-tenant-payouts",
  "tags": ["type:feature", "scope:tenant"]
}
```

## 4. Règles de dépendances

```text
features    -> data-access, ui, core, shared
core        -> shared, ui/layout avec prudence
data-access -> core/http, shared
ui          -> shared uniquement
shared      -> rien
```

## 5. Règles strictes

- `ui` ne dépend jamais de `data-access`.
- `ui` ne dépend jamais de `features`.
- `data-access` ne dépend jamais de `features`.
- `shared` ne dépend jamais de `core`, `features`, `data-access`, `ui`.
- `features` ne doivent pas accéder directement à `HttpClient` si une lib `data-access` existe.

## 6. Exemple ESLint boundary

Voir `examples/eslint/module-boundaries.example.json`.

## 7. Quand extraire une feature en lib Nx

Extraire une feature vers `libs/features/...` seulement si :

```text
elle est utilisée par plusieurs apps
elle devient grosse
elle a son propre cycle de tests
elle est stratégique
elle doit avoir des boundaries Nx propres
```

Sinon, garder dans :

```text
apps/tch-web/src/app/features/<scope>/<feature>
```

## 8. Anti-patterns Nx

Ne pas faire :

```text
une lib Nx par composant
feature/data-access/ui/util pour chaque petit écran
20 libs vides pour prévoir le futur
```

Faire :

```text
libs transverses stables
features dans app au début
extraction progressive
```
