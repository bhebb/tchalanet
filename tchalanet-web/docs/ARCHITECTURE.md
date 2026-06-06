# tch-portal — Architecture

> **Statut** : Target architecture — migration en cours  
> **App** : `apps/tch-portal/` (Angular 20 / Nx)  
> **Référence détaillée** : [`docs/web/frontend-architecture-todo.md`](./web/frontend-architecture-todo.md)

---

## Structure active

```
tchalanet-web/
├── apps/
│   └── tch-portal/         ← application Angular principale
│       └── src/app/
│           ├── core/       ← auth, HTTP, i18n, runtime, settings, PageModel API
│           ├── features/   ← pages, shells, renderer PageModel et widgets actifs
│           └── shared/     ← contrats encore locaux à l'application
└── libs/
    ├── api/                ← contrats et infrastructure HTTP transverses
    ├── shared-config/      ← settings runtime et feature flags
    └── ui/
        ├── components/     ← composants réutilisables + ActionItem + breakpoints
        ├── styles/         ← primitives SCSS compile-time
        └── theme/          ← thème runtime, presets Material 3 et tokens
```

Cette structure est volontairement petite. Chaque lib active porte une frontière déjà utilisée.

## Structure cible par extraction

```text
libs/
  api/            contrats backend/web, clients HTTP et interceptors
  shared-auth/    OIDC/Keycloak, session et guards
  shared-i18n/    traduction runtime et sélection de langue
  shared-config/  feature flags, settings et configuration runtime
  ui/             components, styles et theme
  page-model/     moteur de layout/rendu PageModel, sans widgets concrets
  widgets/        registry et widgets dynamiques
  web/            routes, pages, containers et shells par surface
```

Ces libs sont des **cibles de migration**, pas des dossiers à créer à vide. Une lib est créée
seulement lorsqu'un change déplace un slice cohérent, définit ses exports publics et valide ses
dépendances Nx.

Ordre recommandé :

1. `shared-auth` / `shared-i18n`;
2. `page-model`;
3. `widgets`;
4. `web`.

`page-model` doit précéder `widgets` : il possède le contrat, l'API et le renderer abstrait.
`widgets` possède ensuite le registry et les widgets concrets. Les dashboards, shells, routes et
pages par rôle appartiennent à `web`.

---

## Surfaces applicatives

| Surface | Route prefix | Rôle |
|---|---|---|
| Public | `/public` | Résultats, vérification ticket, PageModel public |
| Cashier/POS | `/cashier` | Vente, paiement, session caisse |
| Private (tenant) | `/private` | Dashboard tenant, gestion |
| Tenant Admin | `/admin` | Configuration tenant |
| Platform Admin | `/platform` | Opérations plateforme |

---

## Convention composants

```
Route → Page → Container(s) → Component(s)
```

| Type | Suffixe | Règle |
|---|---|---|
| Page | `*.page.ts` | Routée, layout principal, peut injecter services applicatifs/store/router |
| Container | `*.container.ts` | Jamais routé, orchestre une sous-zone |
| Component | `*.component.ts` | Stateless/presentational, `input()`/`output()` |
| Widget | `*.widget.ts` | Rendu par PageModel, props uniquement |
| Shell | `*.shell.ts` | Structure globale d'une surface |

---

## Règles non négociables

- Toutes les routes pointent vers une `Page`
- Les composants UI ne font pas d'appel HTTP
- Les composants UI ne dépendent pas de NgRx ni de services applicatifs
- Les nouveaux contrats backend/frontend ciblent `libs/api/contracts`; les contrats actifs restent
  temporairement dans `apps/tch-portal/src/app/shared/types`
- Les pages orchestrent des services applicatifs/state dédiés, sans appeler directement `HttpClient`
- Pas de nouvelle lib sans frontière claire et stable
- Pas de lib Nx vide créée uniquement pour correspondre au diagramme cible

---

## Conventions

Voir [`docs/conventions/`](./conventions/README.md) :
- Naming, state management, Nx boundaries, feature playbook, placement guide.
- Auth, theme, i18n, runtime settings, PageModel, HTTP API.
