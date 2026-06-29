# web-apps-split-v1

## Objectif

Restructurer `tchalanet-web` en apps Angular/Nx déployables indépendamment, avec des libs stables,
une structure feature claire, des pages lisibles, et une base compatible SSR/hydration pour plus
tard.

## Pourquoi

`apps/tch-portal` contient aujourd'hui les surfaces public, tenant admin, seller-terminal/POS et
platform. Cette structure freine :

- le déploiement indépendant d'`admin-portal`, `public-portal` et `platform-portal`;
- la réutilisation propre de login/auth/i18n/errors/shell;
- la séparation claire entre clients API métier et contrats HTTP techniques;
- la future extraction POS si la vente grossit;
- la préparation SSR/SSG par surface.

## Ce qui change

- Créer/valider trois apps Angular/Nx V0 :
  - `apps/public-portal`
  - `apps/admin-portal`
  - `apps/platform-portal`
- Ne pas créer `apps/pos-portal` en V0.
- Stabiliser les libs cibles :
  - `libs/api`
  - `libs/core/auth`
  - `libs/core/i18n`
  - `libs/ui/theme`
  - `libs/ui/styles`
  - `libs/ui/components`
  - `libs/web/errors`
  - `libs/web/shell`
  - `libs/page-model`
  - `libs/shared-config`
- Garder les clients API métier dans les features, sous `data-access`.
- Standardiser la structure feature/page/components/data-access.
- Préparer proxy sous-routes et déploiement indépendant.
- Prioriser les dernières pratiques Angular : standalone, lazy routes, signals, signal forms,
  `httpResource`/`resource` quand adapté, `@defer`, OnPush.

## Non-objectifs

- Pas de `pos-portal` en V0.
- Pas de Module Federation.
- Pas de centralisation des clients métier dans `libs/api/clients`.
- Pas de migration immédiate de tous les formulaires existants vers signal forms.
- Pas d'activation SSR forcée pour `admin-portal` en V0.
- Pas de lib séparée `libs/shell`; les shells partagés vivent dans `libs/web/shell`.

## Impact

- `admin-portal` build/deploy seul, sans public ni platform.
- `public-portal` build/deploy seul.
- `platform-portal` build/deploy seul.
- Login partagé depuis `libs/core/auth`.
- Seller-terminal/POS reste lazy-loaded sous `admin-portal` en V0.
- Les apps ne dépendent pas directement les unes des autres.
- Les routes locales peuvent être servies derrière `/public`, `/admin`, `/platform` et `/api/v1`.
- Les libs déjà bien placées ne sont pas déplacées inutilement.
