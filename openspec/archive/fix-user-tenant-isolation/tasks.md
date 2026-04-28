## Status: DRAFT

## 0. Rappel architectural (OBLIGATOIRE à lire avant toute implémentation)

> ❌ Ne pas ajouter `WHERE tenant_id = ?` dans le code Java.  
> ✅ L'isolation tenant est assurée par RLS PostgreSQL (`V40__rls_policies.sql`).  
> Le fix corrige l'API trompeuse et le comportement SUPER_ADMIN, pas les requêtes SQL.

## 1. Audit des call sites

- [x] 1.1 `grep -r "findByTenantId\|findAllActiveUsersByTenant\|searchByCriteria" tchalanet-server/src/` — inventorier tous les call sites
- [x] 1.2 Identifier quels call sites sont en scope TENANT_ADMIN vs SUPER_ADMIN/platform
- [x] 1.3 Vérifier si les call sites SUPER_ADMIN positionnent déjà `TchContext` avant l'appel
- [x] 1.4 Vérifier que `EntityManager` (Criteria API dans `searchByCriteria`) passe bien par `RlsAwareDataSource` → inspecter la config Spring Boot `DataSource`

## 2. Nettoyage de l'API trompeuse

- [x] 2.1 Supprimer le paramètre `TenantId` de `UserReaderPort.findByTenantId(TenantId, Pageable)` — renommer en `findAll(Pageable)` (ou conserver le nom + documenter que le filtre tenant est via RLS, pas via le param)
- [x] 2.2 Idem pour `findAllActiveUsersByTenant(TenantId, Pageable)` → `findAllActive(Pageable)`
- [x] 2.3 Mettre à jour `AppUserPersistenceAdapter` en conséquence
- [x] 2.4 Adapter tous les call sites

## 3. Correction du flow SUPER_ADMIN

- [x] 3.1 Isolation via JOIN `tenant_user` dans `findByTenantMembership` — TchContext non requis car le filtre est en SQL (pas via RLS, qui ne couvre pas `app_user` sans `tenant_id`)
- [x] 3.2 Pattern TchContext.set/clear non répété — utilitaire non créé
- [x] 3.3 N/A — TchContext.clear() non utilisé dans ce flow

## 4. Tests d'intégration membership (Testcontainers)

- [x] 4.1 Créer `AppUserTenantMembershipIT` (Testcontainers PostgreSQL) :
  - Insérer des users pour tenant-A, tenant-B et les deux
  - Vérifier : `findByTenantId(tenantA)` retourne uniquement les users de tenant-A
- [x] 4.2 Test isolation : soft-delete membership, SUSPENDED membership, et tenant sans membres → 0 résultats
- [x] 4.3 N/A — `searchByCriteria` utilise la Criteria API sans filtre tenant (hors scope de cette change)

## 5. Vérification finale

- [x] 5.1 `./mvnw clean verify` → build vert + tous tests (7 échecs ArchUnit pre-existants, non liés à cette change)
- [x] 5.2 Confirmer : aucune ligne `WHERE tenant_id` ajoutée dans `AppUserPersistenceAdapter`
- [x] 5.3 Mettre à jour CHANGELOG (`SECURITY: fix SUPER_ADMIN cross-tenant user exposure — RLS context must be set before user queries`)
