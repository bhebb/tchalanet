# Module `common` — Technical Shared Kernel

> **Archetype** : Technical Shared Kernel.  
> **Règle d'or** : si ce code a une table, une policy applicative, un workflow, un adapter externe ou une notion utilisateur/tenant métier → il appartient à `platform/`, pas à `common/`.

---

## 1. Rôle

`common` contient les primitives techniques partagées par **toutes** les couches (common, catalog, platform, core, features).

Il n'a pas de domaine métier. Il ne connaît ni les tenants, ni les utilisateurs, ni les produits.

---

## 2. Ce qui appartient ici

```text
common.bus              CommandBus, QueryBus (interfaces + implémentation in-memory)
common.event            DomainEventPublisher, AfterCommit, helpers publication
common.context          TchRequestContext, TchContext, thread-local lifecycle
common.types.id         TypedId<T>, IdGenerator, bases pour TicketId / TenantId / etc.
common.types.money      CurrencyCode, Money (si value object pur)
common.types.enums      Enums partagés cross-domain (hors logique métier)
common.cache            CacheSpecProvider, abstractions cache (sans implémentation métier)
common.security         TchPermissionEvaluator (glue technique Spring Security uniquement)
common.persistence      RLS infrastructure, soft-delete, audit entities de base
common.web              ApiResponse<T>, TchPage, TchPageRequest, ProblemRest
common.tx               @TchTx, AfterCommit, helpers transaction
common.stereotype       @UseCase, @Adapter, @DomainService (annotations partagées)
common.validation       Validators Jakarta Bean Validation génériques
common.config           Spring @Configuration transversaux
common.util             Utilitaires purement techniques (sans logique métier)
```

---

## 3. Ce qui N'appartient PAS ici

| Concept | Appartient à |
|---|---|
| Audit applicatif (table, workflow) | `platform.audit` |
| Contrôle d'accès / permissions | `platform.accesscontrol` |
| Profil utilisateur / tenant user | `platform.identity` |
| Configuration tenant (values) | `platform.tenantconfig` |
| Thème tenant | `platform.tenanttheme` |
| Idempotence persistée (table) | `platform.idempotence` |
| Notification / communication | `platform.notification`, `platform.communication` |
| Tout adapter externe métier | `platform.*` ou `core.*` |

---

## 4. Règles

- ✅ Aucun import de `core`, `catalog`, `platform`, `features`
- ✅ Thread-safe (context, bus, caches)
- ✅ Pas de `@Entity` JPA avec logique métier
- ✅ Pas de `@Service` avec workflow applicatif
- ❌ Pas de référence à un domaine métier nommé (ticket, draw, session…)
- ❌ Pas de table avec `tenant_id` + logique métier

---

## 5. Packages actuels

```text
common/
  batch/        helpers Spring Batch génériques
  bus/          CommandBus, QueryBus, handlers
  cache/        abstractions cache (Caffeine, Redis)
  client/       bases HTTP client techniques
  config/       Spring config transversaux
  constant/     constantes globales
  context/      TchRequestContext, TchContext
  event/        DomainEventPublisher, AfterCommit
  json/         Jackson helpers
  mapper/       bases mapping
  persistence/  RLS, soft-delete, audit infra
  security/     TchPermissionEvaluator (glue Spring Security)
  selection/    utilitaires sélection
  stereotype/   @UseCase, @Adapter, @DomainService
  time/         utilitaires date/heure
  tx/           @TchTx, helpers transaction
  types/        TypedId, CurrencyCode, enums partagés
  util/         utilitaires divers
  web/          ApiResponse, TchPage, ProblemRest
```

---

## 6. Migration rule

Avant d'ajouter quelque chose dans `common`, vérifier :

1. Est-ce que ça a une table ? → `platform/`
2. Est-ce que ça connaît un tenant/utilisateur spécifiquement ? → `platform/`
3. Est-ce que ça implémente une règle métier (même légère) ? → `core/` ou `platform/`
4. Est-ce que plusieurs autres couches en ont besoin (technique uniquement) ? → `common/` ✅
