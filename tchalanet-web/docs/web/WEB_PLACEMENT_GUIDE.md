# Web Placement Guide — Tchalanet

> Status: DRAFT v0.1

## 1. Table de placement

| Élément                      | Placement                                       |
| ---------------------------- | ----------------------------------------------- |
| AuthService                  | `libs/core/auth`                                |
| AuthSessionStore             | `libs/core/auth`                                |
| AuthGuard / PermissionGuard  | `libs/core/auth`                                |
| Login page                   | `apps/tch-web/src/app/features/public/login`    |
| Runtime config               | `libs/core/config`                              |
| ApiClient                    | `libs/core/http`                                |
| HTTP interceptors            | `libs/core/http` ou `libs/core/auth` selon rôle |
| ApiResponse / ProblemDetail  | `libs/core/http`                                |
| TchPage                      | `libs/core/http` ou `libs/data-access/common`   |
| LocaleStore                  | `libs/core/i18n`                                |
| Traductions backend          | `libs/data-access/i18n`                         |
| Écran platform i18n          | `features/platform/i18n-overrides`              |
| Header visuel                | `libs/ui/layout/app-header`                     |
| Header connecté              | `libs/core/shell`                               |
| Footer visuel                | `libs/ui/layout/app-footer`                     |
| Footer connecté              | `libs/core/shell` si dynamique                  |
| Sidebar visuel               | `libs/ui/layout/sidebar`                        |
| Navigation selon permissions | `libs/core/shell`                               |
| Button / Card / Badge        | `libs/ui/components`                            |
| PayoutApiService             | `libs/data-access/payout/api`                   |
| Payout models                | `libs/data-access/payout/model`                 |
| Payouts page                 | `features/tenant/payouts`                       |
| Payouts page state           | `features/tenant/payouts`                       |
| Payout cache partagé         | `libs/data-access/payout/state`                 |
| PageModel API                | `libs/data-access/page-model/api`               |
| PageModel models             | `libs/data-access/page-model/model`             |
| Page renderer visuel         | `libs/ui/page-renderer`                         |
| Widgets visuels              | `libs/ui/widgets`                               |
| Page publique dynamique      | `features/public/dynamic-page`                  |
| PageModel editor             | `features/platform/page-models`                 |
| Pure util                    | `libs/shared/utils`                             |
| Validator générique          | `libs/shared/validators`                        |
| Test helpers                 | `libs/shared/testing`                           |

## 2. Header

Deux responsabilités :

```text
ui/layout/app-header        rendu visuel
core/shell/header-container connexion auth/nav/tenant
```

## 3. Footer

Si statique :

```text
ui/layout/app-footer
```

Si dynamique :

```text
ui/layout/app-footer
core/shell/footer-container
```

## 4. PageModel

Pas de dossier `engine`.

```text
data-access/page-model       données/API
ui/page-renderer             rendu pur
ui/widgets                   widgets purs
features/public/dynamic-page runtime public
features/platform/page-models admin editor
```

## 5. i18n

```text
core/i18n                    runtime Angular, locale courant
data-access/i18n             API backend/catalog i18n
features/platform/i18n-overrides écran d'administration
```

## 6. Config

```text
core/config                   runtime config chargée au démarrage
data-access/tenant            tenant config venant du backend
features/*                    config locale à une feature
```
