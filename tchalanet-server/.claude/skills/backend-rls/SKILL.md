---
name: backend-rls
description: >
  Use when writing database queries, repositories, JPA entities, batch jobs, or any tenant-aware code in tchalanet-server — enforces PostgreSQL Row-Level Security patterns, explains TchContextFilter/RlsAwareDataSource flow, and prevents tenant filter leakage into Java code.
---

# Multi-tenancy et RLS — Row-Level Security

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/persistence/rls.md`

## Principe (ABSOLU)

L'isolation des tenants est déléguée à **PostgreSQL via RLS**.  
L'application **ne filtre jamais par tenant dans le code Java**.

---

## Flow runtime

```
HTTP Request
  → TchContextFilter (OncePerRequestFilter)
      • résout ApiScope (PUBLIC | TENANT | ADMIN | PLATFORM | _SDR)
      • lit JWT : tenant_code, sub (user), rôles
      • override tenant autorisé uniquement pour SUPER_ADMIN
      • résout UUID tenant via TenantBootstrapLookup (DataSource raw + cache)
      • publie TchRequestContext (ThreadLocal + request attribute + MDC)

  → RlsAwareDataSource.getConnection()
      set_config('app.current_tenant',    <uuid | ''>)
      set_config('app.deleted_visibility', 'active' | 'deleted' | 'all')
      set_config('app.api_scope',          'public' | 'tenant' | 'admin' | 'platform')
      set_config('app.is_super_admin',     'true' | 'false')

  → PostgreSQL policies appliquées automatiquement

  → ResetOnCloseConnection.close()
      reset app.current_tenant = ''
      reset app.deleted_visibility = 'active'
      (empêche fuite entre connexions HikariCP)
```

---

## Politiques RLS — patterns SQL

### Table tenantée + soft delete (cas standard)

```sql
CREATE POLICY tenant_rls ON my_table
  USING (
    tenant_id = current_tenant()
    AND (
      deleted_visibility() = 'all'
      OR (deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
    )
  );
```

### Table tenantée sans soft delete

```sql
CREATE POLICY tenant_rls ON my_table
  USING (tenant_id = current_tenant());
```

### Cross-tenant pour SUPER_ADMIN

```sql
CREATE POLICY tenant_or_superadmin ON my_table
  USING (
    current_setting('app.is_super_admin', true) = 'true'
    OR (
      tenant_id = current_tenant()
      AND deleted_at IS NULL
    )
  );
```

---

## Tables non-tenantées (pas de RLS)

- `tenant`, `result_slot`, `draw_result`, catalogues globaux
- Utilisent `BaseEntity` (pas `BaseTenantEntity`)
- Pas de policy RLS sur ces tables

---

## Batch / Scheduler (hors HTTP)

```java
// Toujours setter/clearer le contexte manuellement
try {
  TchContext.set(batchContext);
  // accès DB normal → RlsAwareDataSource lit TchContext
} finally {
  TchContext.clear();
}
```

---

## Règles absolues

| Interdit                                 | Autorisé                                 |
| ---------------------------------------- | ---------------------------------------- |
| `WHERE tenant_id = ?` dans le code Java  | Laisser RLS filtrer                      |
| DataSource par tenant                    | DataSource unique partagé                |
| Lookup tenant via JPA dans le DataSource | `TenantBootstrapLookup` (DataSource raw) |
| `client-provided tenant_id` trusted      | Toujours résoudre depuis JWT             |
| `@RequestScope` pour le contexte         | ThreadLocal (`TchContext`)               |

---

## Checklist nouvelle table

- [ ] Table tenantée → étend `BaseTenantEntity` + colonne `tenant_id UUID NOT NULL`
- [ ] Policy RLS créée dans la migration Flyway
- [ ] Table non-tenantée → étend `BaseEntity` + documentée dans `rls.md`
- [ ] Aucun `findByTenantId(...)` dans les queries read-side
- [ ] Batch : `TchContext.set(ctx)` avant tout accès DB, `TchContext.clear()` en finally
