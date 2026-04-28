## Context

### Architecture RLS du projet (NON-NÉGOCIABLE)

L'isolation multi-tenant est **entièrement déléguée à PostgreSQL via RLS**. La règle absolue :

> ❌ Jamais `WHERE tenant_id = ?` dans le code Java.
> ✅ Le filtre tenant est posé via `set_config('app.current_tenant', ...)` par `RlsAwareDataSource`, qui lit le `TchContext` peuplé par `TchContextFilter`.

`V40__rls_policies.sql` active RLS + FORCE ROW LEVEL SECURITY sur `app_user`. La policy standard filtre par `tenant_id = current_tenant()`. La policy SELECT autorise une lecture cross-tenant pour `allow_platform_cross_tenant_select()` = `is_super_admin() AND api_scope() = 'platform'`.

### Diagnostic précis des 3 méthodes incriminées

```java
// BUG 1 : tenantId param totalement ignoré
public Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable) {
  var page = jpa.findAll(pageable);  // ← findAll() sans condition
}

// BUG 2 : même problème
public Page<AppUser> findAllActiveUsersByTenant(TenantId tenantId, Pageable pageable) {
  var page = jpa.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE.name(), pageable); // ← pas de tenant
}

// BUG 3 : searchByCriteria via EntityManager — pas de prédicat tenant_id
// (RLS le gère, mais le CriteriaBuilder ne l'exprime pas)
```

**Pour un TENANT_ADMIN** (scope `tenant`, `app.current_tenant = <uuid>`) : RLS filtre correctement → comportement correct en pratique, malgré le code trompeur.

**Pour un SUPER_ADMIN en scope `platform`** (`allow_platform_cross_tenant_select() = true`) : `jpa.findAll()` retourne TOUS les utilisateurs de TOUS les tenants, car la policy SELECT n'a pas de filtre `tenant_id`. `findByTenantId(someTenantId)` était censé filtrer, mais le paramètre est ignoré → fuite cross-tenant réelle.

### Racine du problème

L'API `UserReaderPort.findByTenantId(TenantId)` est **trompeuse** : elle prétend filtrer par tenant alors qu'elle ne le fait pas. Le code s'est fait passer pour un filtre applicatif alors que l'isolation est censée être uniquement le travail du RLS.

## Goals / Non-Goals

**Goals:**

- Corriger le comportement SUPER_ADMIN/platform : `findByTenantId(tenantId)` doit retourner uniquement les users du tenant demandé, même pour une requête cross-tenant autorisée
- Supprimer ou documenter clairement l'API trompeuse (`findByTenantId` avec paramètre ignoré)
- Ajouter des tests d'intégration Testcontainers qui **prouvent** l'isolation RLS réelle (pas des mocks, des vraies connexions PostgreSQL avec `set_config`)
- Vérifier que les requêtes `EntityManager` (Criteria API) passent bien par `RlsAwareDataSource`

**Non-Goals:**

- ❌ Ajouter `WHERE tenant_id = :tenantId` dans le code Java (violation de l'architecture RLS)
- Refonte de l'architecture RLS ou de `TchContextFilter`
- Correction des anomalies typed IDs de `core.user` (traité dans `fix-typed-ids-user-domain`)

## Decisions

### D1 — Correction du cas SUPER_ADMIN : positionner TchContext avant la requête

Pour les endpoints SUPER_ADMIN qui veulent lister les users d'un tenant spécifique, la solution conforme à l'architecture est d'**exécuter la requête dans le contexte tenant** :

```java
// Dans le handler ou le controller appelant
try {
  TchContext.set(buildTenantContext(targetTenantId));
  return userReaderPort.findAll(pageable); // RLS filtre par le tenant positionné
} finally {
  TchContext.clear();
}
```

Ainsi, `findByTenantId(TenantId)` peut être renommé/simplifié en `findAll(Pageable)` — le tenant vient du contexte, pas du paramètre.

### D2 — Simplification de l'API `UserReaderPort`

Le paramètre `TenantId` dans `findByTenantId` et `findAllActiveUsersByTenant` est redondant avec le RLS. Il sera **supprimé** de l'interface et remplacé par les variantes sans paramètre tenant.

Le call site SUPER_ADMIN est responsable de positionner le contexte tenant avant l'appel (D1).

### D3 — Tests d'intégration RLS obligatoires

Les tests doivent utiliser Testcontainers PostgreSQL, se connecter en tant que `app_user` DB role, et vérifier qu'avec `set_config('app.current_tenant', '<uuid-A>')`, seules les lignes du tenant A sont retournées — même quand les deux tenants sont présents en base.

## Risks / Trade-offs

- **[Risque] Call sites SUPER_ADMIN existants** : les endpoints qui appellent `findByTenantId(tenantId)` doivent positionner `TchContext` — à auditer. → Mitigation : `grep` des call sites + vérification du flow SUPER_ADMIN.
- **[Risque] Tests unitaires existants** : des tests qui mockent l'adapter peuvent masquer le vrai comportement. → Les tests d'intégration Testcontainers valident le comportement réel.
- **[Trade-off] Complexité SUPER_ADMIN** : le pattern `TchContext.set/clear` en dehors de `TchContextFilter` est à encadrer (risque de fuite si pas de `finally`). → Créer un utilitaire `TchContextScope.withTenant(id, callable)` si le pattern est répété.

## Migration Plan

1. Auditer les call sites de `findByTenantId` et `findAllActiveUsersByTenant` — identifier les appels SUPER_ADMIN
2. Supprimer le paramètre `TenantId` des méthodes dans le port et l'adapter
3. Adapter les call sites SUPER_ADMIN pour positionner `TchContext` avec le tenant cible
4. Vérifier que `EntityManager` (Criteria API) passe par `RlsAwareDataSource`
5. Créer tests d'intégration Testcontainers prouvant l'isolation RLS
6. `./mvnw clean verify`

## Open Questions

- Q1 : Les appels SUPER_ADMIN à `findByTenantId` positionnent-ils déjà `TchContext` ? (à auditer dans `features/tenantadmin`)
- Q2 : `EntityManager` est-il correctement wrappé par `RlsAwareDataSource` ? (vérifier la config DataSource Spring Boot)
