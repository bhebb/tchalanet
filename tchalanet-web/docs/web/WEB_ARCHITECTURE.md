# Web Architecture — Tchalanet

> Status: DRAFT v0.1  
> Scope: `tchalanet-web` / Angular / Nx  
> But: stabiliser le modèle mental frontend avant d'ajouter des features.

## 1. Références

Cette architecture s'appuie sur :

- Angular Style Guide : organiser le projet par feature areas et garder les fichiers liés ensemble.
- Angular Workspace structure : un workspace peut contenir une app ou plusieurs projets/libs partageables.
- Nx Project Dependency Rules : types de libs `feature`, `ui`, `data-access`, `utility`.
- Nx Enforce Module Boundaries : règles de dépendances via tags.

## 2. Modèle mental canonique

```text
core         connecte l'application
features     composent les écrans
data-access  parle au backend et garde le state réutilisable
ui           dessine
shared       aide
```

Ces cinq familles sont la base. Ne pas ajouter de nouveaux top-level concepts sans raison claire.

## 3. Structure cible légère

```text
apps/
  tch-web/
    src/app/
      features/
        public/
        tenant/
        admin/
        platform/

libs/
  core/
    auth/
    config/
    http/
    i18n/
    shell/

  data-access/
    payout/
    sales/
    draw/
    tenant/
    catalog/
    page-model/
    i18n/

  ui/
    components/
    layout/
    theme/
    styles/
    page-renderer/
    widgets/

  shared/
    utils/
    validators/
    testing/
    types/
```

## 4. Pourquoi les features peuvent rester dans `apps/tch-web`

Tchalanet n'a actuellement pas plusieurs apps Angular actives. Les futures apps sont possibles, mais pas garanties.

Règle :

```text
Une feature spécifique à tch-web reste dans apps/tch-web/src/app/features.
Une feature devient une lib Nx quand elle est partagée, grosse, stratégique ou fortement testée séparément.
```

## 5. `core/`

`core` contient l'infrastructure globale, singleton, branchée au démarrage de l'app.

Exemples :

```text
core/auth          session, guards, permissions, interceptors auth
core/config        runtime config
core/http          api client, ApiResponse, ProblemDetail, interceptors
core/i18n          locale courant, loader, provider Angular
core/shell         app shell, navigation globale, header/footer connectés
```

`core` ne contient pas de composant visuel pur, sauf containers/connecteurs liés au shell.

## 6. `features/`

`features` contient les routes, pages, composants connectés et stores d'écran.

Structure :

```text
features/<scope>/<feature>/
```

Scopes :

```text
public    pages publiques
tenant    espace tenant/utilisateur
admin     administration tenant
platform  administration plateforme / super admin
```

Exemple :

```text
features/tenant/payouts/
  payouts.routes.ts
  payouts.page.ts
  payouts.store.ts
  payout-list.component.ts
  payout-detail-panel.component.ts
```

Règle : rester plat dans une feature tant qu'elle est petite. Créer `components/`, `pages/`, `state/` seulement quand il y a au moins 3 fichiers du même rôle.

## 7. `data-access/`

`data-access` contient les contrats backend, clients API, stores/cache réutilisables.

Exemple :

```text
data-access/payout/
  model/
    payout-item.ts
    payout-details.ts
    execute-payout.request.ts
  api/
    payout-api.service.ts
  state/
    payout-cache.store.ts
```

Règles :

- Ne dépend jamais de `features`.
- Peut dépendre de `core/http` et `shared`.
- Peut contenir un state réutilisable par plusieurs features.
- N'est pas un endroit pour le state purement local d'un écran.

## 8. `ui/`

`ui` contient les composants présentationnels et le design system.

Exemples :

```text
ui/components/button
ui/components/card
ui/components/badge
ui/layout/app-header
ui/layout/app-footer
ui/layout/sidebar
ui/page-renderer
ui/widgets
```

Règles :

- Pas d'appel HTTP.
- Pas de dépendance vers `data-access` ou `features`.
- Pas de logique métier.
- Inputs/outputs uniquement.

## 9. `shared/`

`shared` contient du code générique très bas niveau.

Exemples :

```text
shared/utils/format-date.ts
shared/utils/is-empty.ts
shared/validators/email.validator.ts
shared/testing/test-builders.ts
```

Règles :

- Pas de `HttpClient`.
- Pas d'auth.
- Pas de composants métier.
- Pas de `Ticket`, `Payout`, `Tenant`, `Draw` sauf types vraiment génériques et justifiés.

## 10. Header, footer, sidebar

Le shell a deux faces : visuelle et connectée.

```text
ui/layout/app-header       header visuel pur
core/shell/header-container header connecté auth/nav/tenant
```

Même règle pour footer/sidebar :

```text
ui/layout/sidebar          rendu visuel des items
core/shell/navigation       construction selon permissions/scope
```

## 11. PageModel et widgets

Pas de dossier spécial `engine`.

PageModel est découpé par rôle :

```text
data-access/page-model       modèles + API + cache réutilisable
ui/page-renderer             rendu visuel d'un PageModel reçu en input
ui/widgets                   composants widgets purs
features/public/dynamic-page page publique qui charge et rend une page
features/platform/page-models écran admin/platform pour éditer les pages
```

## 12. Contrats backend

Le frontend doit refléter les contrats backend :

```text
2xx       ApiResponse<T>
4xx/5xx   ProblemDetail
listes    TchPage<T>
```

Ces types vivent dans `core/http` ou dans une petite lib dédiée `data-access/common-http` si nécessaire.

## 13. Règle anti-dérive

Si un fichier ne rentre pas clairement dans `core`, `features`, `data-access`, `ui`, ou `shared`, ne crée pas un nouveau top-level folder immédiatement.

Décider d'abord :

```text
Est-ce qu'il connecte l'app ? core
Est-ce qu'il affiche un écran ? feature
Est-ce qu'il parle au backend ? data-access
Est-ce qu'il dessine ? ui
Est-ce qu'il aide de façon générique ? shared
```
