# Feature PageModel — Le mini-CMS Tchalanet

> **Rôle** : moteur de composition de pages publiques et de dashboards  
> **Scope** : `features.pagemodel` — BFF/composition layer  
> **Domaine** : `core.pagemodel` (lifecycle, templates, publications)  
> **Specs** : `openspec/specs/pagemodel-contract/spec.md` · `openspec/specs/platform-pagemodel/spec.md` · `openspec/specs/pagemodel-security/spec.md`

---

## Concept central

Un PageModel est un document JSON interne qui décrit le **layout et les widgets** d'une surface
applicative. Le frontend consomme uniquement le `PageRuntimeResponse` résolu par
`features.pagemodel`.

```
Backend : PageModel definition (layout + widget bindings + props)
        ↓
features.pagemodel : résout fragments/providers + retire binding/fileKey
        ↓
Frontend : PageRuntimeResponse prêt à rendre
```

**Le PageModel est layout/config, pas vérité opérationnelle.**  
Pour savoir si un seller peut vendre → utiliser le cashier home BFF, pas le PageModel.

---

## Surfaces

| Type | logicalId | Scope | Consommateur |
|---|---|---|---|
| `PUBLIC_HOME` | `public.home` | public | Web public, anonymous |
| `DASHBOARD_SUPERADMIN` | `private.dashboard.superadmin` | private | Super-admin |
| `DASHBOARD_TENANT_ADMIN` | `private.dashboard.tenant_admin` | private | Tenant admin |
| `DASHBOARD_OPERATOR` | `private.dashboard.operator` | private | Opérateur |
| `DASHBOARD_CASHIER` | `private.dashboard.cashier` | private | Cashier (legacy) |
| `DASHBOARD_CASHIER_WEB` | `private.dashboard.cashier.web` | private | Cashier web dashboard |

> **Note** : Le POS mobile utilise `GET /tenant/cashier/home` — pas le PageModel.

---

## Endpoints

```http
GET /public/page                         ← unique page publique (anonymous)
GET /tenant/dashboard                    ← dashboard tenant (résolu par rôle)
GET /platform/dashboard                  ← dashboard platform (SUPER_ADMIN)
```

La résolution du logicalId est faite server-side. Le client ne fournit aucun logicalId runtime.

Le modèle interne peut contenir `binding`, `source` et `fileKey`. Le runtime frontend ne les reçoit
jamais. Son `meta` contient seulement `logicalId`, `scope`, `slug` et `schemaVersion`.
Ses payloads volatils sont exposés sous `dynamic.widgets`; les `notices` et `services` restent
uniquement dans l'enveloppe `ApiResponse`. Le champ `theme` est un hint/fallback de page, tandis que
le bootstrap `ThemeApi` reste la source de vérité runtime.

---

## Architecture : Dynamic Providers

`features.pagemodel` is the BFF/composition layer for public and private PageModel
responses. It resolves the effective PageModel through core queries, then enriches
dynamic widgets and shell sections through `PageModelDynamicProvider`.

## Dynamic source rules

- `binding.source` = identifiant stable `snake_case`
- `providerKey()` doit correspondre exactement au `binding.source` géré
- Les providers composent via `QueryBus` ou application services stables
- Les providers n'accèdent jamais directement aux repositories, JPA, SQL, tables métier
- Les données tenant/actor viennent de `TchRequestContext` — le client ne fournit pas de tenant data dans les widget props

## JSON fragments

Reusable navigation and shell payloads use the generic `json_file` source.
Templates provide `props.file_key`; the provider resolves that key through
`PageModelJsonFragmentRegistry`.

Raw paths are forbidden. Unknown keys, including path traversal strings, are rejected before
any classpath resource is loaded.

Fragments live under:

```text
src/main/resources/pagemodel/fragments/
```

Fragments use camelCase API fields such as `labelKey`, `activeMatch`, `reasonKey`, and
`requiredRoles`. Do not put translated labels in fragment JSON.

## Current fragment registry

| `file_key`                      | Resource                                                       |
| ------------------------------- | -------------------------------------------------------------- |
| `public_header_links`           | `pagemodel/fragments/public/header.links.json`                 |
| `public_footer_links`           | `pagemodel/fragments/public/footer.links.json`                 |
| `private_footer_links`          | `pagemodel/fragments/private/footer.links.json`                |
| `private_header_cashier`        | `pagemodel/fragments/private/cashier/header.links.json`        |
| `private_sidebar_cashier`       | `pagemodel/fragments/private/cashier/sidebar.links.json`       |
| `private_cashier_quick_actions` | `pagemodel/fragments/private/cashier/quick_actions.links.json` |

## Current dynamic sources

| Source                     | Provider                         |
| -------------------------- | -------------------------------- |
| `json_file`                | `JsonFileProvider`               |
| `public_news`              | `PublicNewsProvider`             |
| `public_draw_results`      | `PublicDrawResultsProvider`      |
| `public_features`          | `PublicFeaturesProvider`         |
| `public_tchala`            | `PublicTchalaProvider`           |
| `public_testimonials`      | `PublicTestimonialsProvider`     |
| `public_plans`             | `PlansProvider`                  |
| `cashier_overview`         | `CashierOverviewProvider`        |
| `cashier_quick_sale`       | `CashierQuickSaleProvider`       |
| `cashier_recent_tickets`   | `CashierRecentTicketsProvider`   |
| `cashier_session`          | `CashierSessionProvider`         |
| `cashier_next_draws`       | `CashierNextDrawsProvider`       |
| `cashier_limits`           | `CashierLimitsProvider`          |
| `admin_kpis`               | `AdminKpisProvider`              |
| `admin_draw_operations`    | `AdminDrawOperationsProvider`    |
| `admin_approval_queue`     | `AdminApprovalQueueProvider`     |
| `admin_alerts`             | `AdminAlertsProvider`            |
| `superadmin_system_health` | `SuperAdminSystemHealthProvider` |
| `superadmin_batch_status`  | `SuperAdminBatchStatusProvider`  |
| `superadmin_tenants`       | `SuperAdminTenantsProvider`      |
| `superadmin_version`       | `SuperAdminVersionProvider`      |

---

## Sécurité PageModel

**PageModels publics** : exposent uniquement du contenu public-safe — pas de routes privées/admin, pas de contexte opérationnel.

**PageModels privés** : le logicalId est résolu server-side depuis `TchRequestContext`.

| Rôle acteur | PageModel retourné |
|---|---|
| `CASHIER` | `private.dashboard.cashier.web` |
| `TENANT_ADMIN` | `private.dashboard.tenant_admin` |
| `SUPER_ADMIN` | `private.dashboard.superadmin` |

**Accès non autorisé → 403** — aucun provider n'est invoqué si l'acteur n'a pas le droit.  
Les providers sensibles re-valident l'accès indépendamment.

---

## Cycle de vie des documents (core.pagemodel)

```
Template global (PUBLISHED)
      ↓ DuplicatePageModel
   DRAFT tenant
      ↓ PublishPageModel
 PUBLISHED (actif)
      ↓ Mise à jour template disponible
   Évaluation compatibilité (PATCH / MINOR / MAJOR)
      ↓ MergePageModelWithTemplate ou CreateDraftFromTemplateUpdate
 DRAFT → PUBLISHED
      ↓
  ARCHIVED
```

Voir domaine : `core/pagemodel/DOMAIN_PAGEMODEL.md`

---

## Onboarding tenant

Lors du provisioning d'un nouveau tenant, `core.pagemodel` reçoit une commande `DuplicatePageModelCommand` depuis `features.platformadmin.tenantonboarding` pour chaque surface.

Le tenant reçoit ses propres documents (draft ou published selon profil). Il peut ensuite les personnaliser.

---

## Règles critiques

- PageModel ≠ vérité opérationnelle (POS readiness, session, draw status)
- Le POS mobile utilise `/tenant/cashier/home`, pas le PageModel
- Les fragments JSON utilisent des champs API camelCase — jamais de labels traduits inline
- Chemins raw interdits dans `file_key` — path traversal rejeté avant tout chargement classpath
- Les providers s'exécutent dans le contexte HTTP existant — ne pas le modifier

---

## Références

- Domaine lifecycle : `core/pagemodel/DOMAIN_PAGEMODEL.md`
- Contract widgets/shell : `openspec/specs/pagemodel-contract/spec.md`
- Sécurité : `openspec/specs/pagemodel-security/spec.md`
- Platform rules : `openspec/specs/platform-pagemodel/spec.md`
- Contexte HTTP : `docs/conventions/context/request-context.md` §PageModel Rules
