# Domain — PageModel

## Responsabilité

`core.pagemodel` gère le cycle de vie des documents PageModel : templates globaux, personnalisations tenant, publications, mises à jour de templates.

Un PageModel est un document JSON qui décrit le layout et les widgets d'une surface applicative (public home, dashboards, cashier). Le frontend le consomme pour construire dynamiquement l'écran.

---

## Surfaces

| Type | Scope | Usage |
|---|---|---|
| `PUBLIC_HOME` | public | Page d'accueil publique |
| `DASHBOARD_SUPERADMIN` | private | Dashboard super-admin |
| `DASHBOARD_TENANT_ADMIN` | private | Dashboard admin tenant |
| `DASHBOARD_OPERATOR` | private | Dashboard opérateur |
| `DASHBOARD_CASHIER` | private | Dashboard cashier mobile |
| `DASHBOARD_CASHIER_WEB` | private | Dashboard cashier web |

---

## États d'un document

```
(template global)
      ↓ DuplicatePageModel / CreateDraftFromTemplateUpdate
   DRAFT
      ↓ PublishPageModel
 PUBLISHED  ←──────────────────────
      ↓ MergePageModelWithTemplate   │
   (mise à jour template disponible) │
      └──────────────────────────────┘
      ↓ IgnoreTemplateUpdate
(reste PUBLISHED, update ignoré)
      ↓ archivage
  ARCHIVED
```

| Statut | Signification |
|---|---|
| `DRAFT` | En cours d'édition, non visible frontend |
| `PUBLISHED` | Actif, servi au frontend |
| `ARCHIVED` | Désactivé, conservé pour historique |

---

## Gestion des mises à jour de template

Quand un template global change, `core.pagemodel` évalue la compatibilité avec les personnalisations tenant :

| Compatibilité | Action recommandée |
|---|---|
| `PATCH` | `MERGE_SAFE` — aucun conflit |
| `MINOR` | `MERGE_WITH_CONFLICTS` ou `CREATE_DRAFT` |
| `MAJOR` | `REPLACE_ALL` ou `REQUIRES_MIGRATION` |

| Action | Signification |
|---|---|
| `MERGE_SAFE` | Fusionner automatiquement |
| `MERGE_WITH_CONFLICTS` | Fusionner avec revue |
| `CREATE_DRAFT` | Créer un draft pour révision |
| `REPLACE_ALL` | Remplacer entièrement par le template |
| `IGNORE` | Ignorer la mise à jour |
| `REQUIRES_MIGRATION` | Migration manuelle requise |

---

## Commandes

| Commande | Rôle |
|---|---|
| `DuplicatePageModelCommand` | Dupliquer un template pour personnalisation tenant |
| `CreateDraftFromTemplateUpdateCommand` | Créer un draft suite à mise à jour template |
| `MergePageModelWithTemplateCommand` | Fusionner un doc tenant avec le nouveau template |
| `IgnoreTemplateUpdateCommand` | Ignorer une mise à jour de template |
| `PublishPageModelCommand` | Publier un draft |
| `ReplacePageModelFromTemplateCommand` | Remplacer entièrement par le template |
| `CreatePageTemplateUpdateNotificationsCommand` | Notifier les tenants d'une mise à jour template |

---

## Dynamic Providers

Les dynamic providers sont des Spring singleton beans qui enrichissent le PageModel au runtime HTTP.

- Ils s'exécutent pendant la requête HTTP après résolution du document.
- Ils ne créent pas le contexte HTTP — ils utilisent le contexte déjà bindé.
- Un provider qui échoue ne doit pas bloquer le rendu de la page entière.

Voir : `docs/conventions/context/request-context.md` §PageModel Rules.

---

## Invariants

- Un seul document `PUBLISHED` par surface par tenant à la fois.
- Un `DRAFT` ne modifie pas le document `PUBLISHED` actif.
- Les templates globaux ne sont pas modifiables directement par les tenants.
- `core.pagemodel` ne connaît pas les widgets — il connaît leur type et leurs props.

---

## Règles

- Le frontend consomme uniquement le PageModel `PUBLISHED`.
- Les providers s'enregistrent dans un registry global (`PageModelDynamicProvider`).
- Voir conventions : `docs/conventions/command_query_handlers.md` · `docs/conventions/event_model.md`
