## Why

`AppUserPersistenceAdapter.findByTenantId()` et `findAllActiveUsersByTenant()` acceptent un paramètre `TenantId` qui est **totalement ignoré** — ces méthodes appellent `jpa.findAll()` sans condition. Pour un `TENANT_ADMIN`, le RLS (`V40__rls_policies.sql`) filtre implicitement via `app.current_tenant`. Mais pour un `SUPER_ADMIN` en scope `platform` (qui bénéficie de `allow_platform_cross_tenant_select()`), ces méthodes retournent tous les utilisateurs de tous les tenants même quand un tenant spécifique est demandé. De même, `searchByCriteria()` repose exclusivement sur le RLS sans assurer que l'intention du `tenantId` encodé dans le contexte est respectée. Anomalie identifiée à l'audit du 2026-04-27.

> ⚠️ La règle projet est absolue : ❌ jamais de `WHERE tenant_id = ?` dans le code applicatif — PostgreSQL via RLS est la seule source de vérité. Le fix ne consiste PAS à ajouter des filtres SQL côté Java.

## What Changes

- Nettoyage de l'API trompeuse : les paramètres `TenantId` sur `findByTenantId()` et `findAllActiveUsersByTenant()` sont supprimés ou documentés comme informatifs uniquement (pas de filtre SQL attendu — le RLS gère).
- Correction de l'isolation SUPER_ADMIN : les endpoints admin qui appellent `findByTenantId(someTenantId)` pour le compte d'un tenant spécifique doivent positionner `TchContext` avec le bon `tenantUuid` avant la requête, afin que RLS filtre correctement.
- Ajout de tests d'intégration Testcontainers qui vérifient l'isolation RLS réelle (connexion en tant que `app_user`, `set_config('app.current_tenant', ...)`, vérification que seules les lignes du bon tenant sont retournées).
- Vérification que `searchByCriteria()` via `EntityManager` passe bien par `RlsAwareDataSource`.

## Capabilities

### New Capabilities

<!-- aucune nouvelle capability fonctionnelle -->

### Modified Capabilities

- `auth-rbac`: le comportement SUPER_ADMIN pour les requêtes users par tenant est clarifié et sécurisé.

## Impact

- `core.user/infra/persistence/AppUserPersistenceAdapter` — API nettoyée, comportement SUPER_ADMIN corrigé.
- `core.user/application/port/out/UserReaderPort` — signature potentiellement simplifiée.
- Endpoints `features/tenantadmin` qui appellent `findByTenantId` — à vérifier si le contexte tenant est correctement positionné.
- Tests d'intégration : nouveaux tests RLS à créer.
- **Aucun breaking change sur l'API HTTP externe.**

---

## Archive Information

**Archived:** 2026-04-28
**Duration:** 1 day (2026-04-27 → 2026-04-28)
**Outcome:** Successfully implemented

### Implementation Note

The original design relied on RLS to filter `app_user` by tenant. During implementation it was discovered that `app_user` has no `tenant_id` column and is therefore excluded from V40 RLS policies. The fix was implemented via a `tenant_user` JOIN instead (filtering on `status = 'ACTIVE' AND deleted_at IS NULL`), which is the correct isolation mechanism for this global identity table.

### Files Modified

- `tchalanet-server/src/main/java/com/tchalanet/server/core/user/application/port/out/UserReaderPort.java`
- `tchalanet-server/src/main/java/com/tchalanet/server/core/user/infra/persistence/JpaAppUserRepository.java`
- `tchalanet-server/src/main/java/com/tchalanet/server/core/user/infra/persistence/AppUserPersistenceAdapter.java`
- `tchalanet-server/CHANGELOG.md`

### Files Created

- `tchalanet-server/src/test/java/com/tchalanet/server/core/user/infra/persistence/AppUserTenantMembershipIT.java`

### Specs Updated

- `openspec/specs/user-tenant-isolation/spec.md` — new spec (created from delta)
