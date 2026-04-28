## Status: DRAFT

## 0. Rappel architectural (OBLIGATOIRE à lire avant toute implémentation)

> ❌ Ne pas ajouter `WHERE tenant_id = ?` dans le code Java.  
> ✅ L'isolation tenant est assurée par RLS PostgreSQL (`V40__rls_policies.sql`).  
> Le fix corrige l'API trompeuse et le comportement SUPER_ADMIN, pas les requêtes SQL.

## 1. Audit des call sites

- [ ] 1.1 `grep -r "findByTenantId\|findAllActiveUsersByTenant\|searchByCriteria" tchalanet-server/src/` — inventorier tous les call sites
- [ ] 1.2 Identifier quels call sites sont en scope TENANT_ADMIN vs SUPER_ADMIN/platform
- [ ] 1.3 Vérifier si les call sites SUPER_ADMIN positionnent déjà `TchContext` avant l'appel
- [ ] 1.4 Vérifier que `EntityManager` (Criteria API dans `searchByCriteria`) passe bien par `RlsAwareDataSource` → inspecter la config Spring Boot `DataSource`

## 2. Nettoyage de l'API trompeuse

- [ ] 2.1 Supprimer le paramètre `TenantId` de `UserReaderPort.findByTenantId(TenantId, Pageable)` — renommer en `findAll(Pageable)` (ou conserver le nom + documenter que le filtre tenant est via RLS, pas via le param)
- [ ] 2.2 Idem pour `findAllActiveUsersByTenant(TenantId, Pageable)` → `findAllActive(Pageable)`
- [ ] 2.3 Mettre à jour `AppUserPersistenceAdapter` en conséquence
- [ ] 2.4 Adapter tous les call sites

## 3. Correction du flow SUPER_ADMIN

- [ ] 3.1 Pour chaque call site SUPER_ADMIN qui demande les users d'un tenant spécifique : s'assurer que `TchContext` est positionné avec le `tenantUuid` cible avant l'appel à l'adapter
- [ ] 3.2 Si le pattern `TchContext.set/clear` est répété ≥ 2 fois : créer un utilitaire `TchContextScope.withTenant(TenantId, Supplier<T>)` dans `common/`
- [ ] 3.3 Vérifier que le `finally { TchContext.clear(); }` est systématique

## 4. Tests d'intégration RLS (Testcontainers)

- [ ] 4.1 Créer `AppUserRlsIsolationIT` (Testcontainers PostgreSQL) :
  - Insérer des `app_user` rows pour tenant-A et tenant-B
  - Exécuter `set_config('app.current_tenant', '<tenant-A-uuid>', false)`
  - Appeler `AppUserPersistenceAdapter.findAll(pageable)`
  - Vérifier : uniquement les users de tenant-A dans les résultats
- [ ] 4.2 Test `findAllActive_withNoTenantContext_returnsEmpty()` : sans `app.current_tenant`, expect 0 rows
- [ ] 4.3 Test `searchByCriteria_isolatedByRls()` : même setup, vérifie que la Criteria API est aussi isolée

## 5. Vérification finale

- [ ] 5.1 `./mvnw clean verify -pl tchalanet-server` → build vert + tous tests
- [ ] 5.2 Confirmer : aucune ligne `WHERE tenant_id` ajoutée dans `AppUserPersistenceAdapter`
- [ ] 5.3 Mettre à jour CHANGELOG (`SECURITY: fix SUPER_ADMIN cross-tenant user exposure — RLS context must be set before user queries`)
