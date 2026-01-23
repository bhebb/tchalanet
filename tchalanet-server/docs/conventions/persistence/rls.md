# Architecture RLS (Row-Level Security) — Tchalanet

## Status

**NORMATIVE**

---

## 1) Objectif

Tchalanet est une plateforme **multi-tenant stricte**.  
Toute requête SQL doit être automatiquement isolée par tenant **sans filtre applicatif** (`@Where`, `@Filter`, etc.).

👉 La sécurité d'isolation est **déléguée à PostgreSQL via RLS**.  
L'application **ne filtre jamais** : elle ne fait que **poser le contexte** sur la connexion JDBC.

---

## 2) Variables de session PostgreSQL

Tchalanet utilise exclusivement des variables de session (`set_config`) :

| Variable                 | Rôle                           |
| ------------------------ | ------------------------------ |
| `app.current_tenant`     | UUID du tenant courant         |
| `app.deleted_visibility` | `active` \| `deleted` \| `all` |

Ces variables sont lues par les **policies RLS**.

---

## 3) Flow runtime réel (aligné code)

```
HTTP Request
    ↓
TchContextFilter (OncePerRequestFilter)
  • resolve ApiScope
  • resolve tenant code / override
  • resolve tenant UUID (TenantBootstrapLookup + cache)
  • build TchRequestContext
  • TchContext.set(ctx) ← ThreadLocal
    ↓
RlsAwareDataSource
  • getConnection()
  • read TchContext via TchContextResolver
  • set_config(app.current_tenant, ...)
  • set_config(app.deleted_visibility, ...)
  • wrap connection with ResetOnCloseConnection
    ↓
PostgreSQL
  • RLS policies enforced
```

📌 **Pas de `@RequestScope`**, pas de magie Spring :  
le contexte est **ThreadLocal** (`TchContext`).

---

## 4) TchContextFilter (point d'entrée)

**Responsabilités** :

- Résolution du **scope API** (`public / admin / tenant / platform / _sdr`)
- Lecture JWT :
  - `tenant_code`
  - `sub` (Keycloak user id)
  - rôles système + custom
- Override tenant autorisé **uniquement pour SUPER_ADMIN**
- Résolution **UUID du tenant** via `TenantBootstrapLookup`
  - DataSource raw (bypass RLS)
  - cache TTL
- Publication du contexte :
  - `request.setAttribute(REQUEST_CONTEXT, ctx)`
  - `TchContext.set(ctx)`
  - MDC (tenant, reqId, user…)

⚠️ Si le scope requiert un tenant et qu'il est absent → **403 immédiat**

---

## 5) RlsAwareDataSource (cœur RLS)

**Type** : `DelegatingDataSource`

### Rôle

- Intercepte `getConnection()`
- Lit le contexte via `TchContextResolver`
- Applique ou réinitialise le RLS **avant toute requête SQL**

### Comportement

- Si `tenantUuid == null` :
  - `set_config('app.current_tenant','', false)`
  - `set_config('app.deleted_visibility','active', false)`
- Sinon :
  - `set_config('app.current_tenant', <uuid>, false)`
  - `set_config('app.deleted_visibility', <visibility>, false)`

📌 `false` = portée **session/connexion** (obligatoire avec pool).

---

## 6) ResetOnCloseConnection (sécurité pool)

Wrapper dynamique de `Connection`.

À l'appel de `close()` :

- reset systématique :
  - `app.current_tenant = ''`
  - `app.deleted_visibility = 'active'`
- puis `delegate.close()`

🎯 Empêche toute **fuite de tenant** entre requêtes HikariCP.

---

## 7) Policies RLS (exemples)

### Table tenantée + soft delete

```sql
tenant_id = current_tenant()
AND (
  deleted_visibility() = 'all'
  OR (deleted_visibility() = 'active' AND deleted_at IS NULL)
  OR (deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
)
```

### Table tenantée sans soft delete

```sql
tenant_id = current_tenant()
```

---

## 8) Tables non-RLS (globales)

Certaines tables ne doivent jamais être protégées par RLS :

- `tenant`
- `result_slot`
- `draw_result`
- catalogues globaux (plans, providers, etc.)

👉 Elles utilisent `BaseEntity`, pas `BaseTenantEntity`.

---

## 9) Batch / Scheduler

Avant toute opération DB hors HTTP :

```java
try {
  TchContext.set(ctx);
  // accès DB normal → RLS appliqué
} finally {
  TchContext.clear();
}
```

---

## 10) Règles absolues

- ❌ Jamais de filtre tenant dans le code
- ❌ Jamais de DataSource par tenant
- ❌ Jamais de lookup tenant via JPA dans le DataSource
- ✅ PostgreSQL est la seule source de vérité

---

## 11) Résumé exécutif

Dans Tchalanet, **le tenant est une propriété SQL, pas applicative**.  
Le code métier reste simple, la base garantit l'isolation.
