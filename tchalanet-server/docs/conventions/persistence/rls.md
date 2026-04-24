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

| Variable                 | Rôle                                                               |
| ------------------------ | ------------------------------------------------------------------ | ----------------------------------------------------- | ------- | ----------------- |
| `app.current_tenant`     | UUID du tenant courant (ou empty string when none)                 |
| `app.deleted_visibility` | `active` \| `deleted` \| `all` — contrôle la visibilité des lignes |
| `app.api_scope`          | scope de l'API en cours (`public`                                  | `tenant`                                              | `admin` | `platform`, etc.) |
| `app.is_super_admin`     | `true`                                                             | `false` — indique si l'appelant a le rôle SUPER_ADMIN |

Ces variables sont lues par les policies RLS pour décider de l'accès. Les deux
nouveaux flags (`app.api_scope` et `app.is_super_admin`) permettent :

- d'avoir des policies qui autorisent des lectures cross-tenant pour les super-admins ;
- de conserver un comportement différent selon le scope (p.ex. `platform` vs `tenant`).

---

## 3) Flow runtime réel (aligné code)

```
HTTP Request
    ↓
TchContextFilter (OncePerRequestFilter)
  • resolve ApiScope (ex: PUBLIC, TENANT, ADMIN, PLATFORM)
  • resolve tenant code (JWT claim tenant_code) / optional override for SUPER_ADMIN
  • resolve tenant UUID (TenantBootstrapLookup + cache) and build TchRequestContext
  • TchContext.set(ctx) ← ThreadLocal
    ↓
RlsAwareDataSource (DelegatingDataSource wrapper)
  • getConnection()
  • read TchContext via TchContextResolver
  • set_config('app.current_tenant', <uuid|''>)
  • set_config('app.deleted_visibility', <active|deleted|all>)
  • set_config('app.api_scope', <scope>)
  • set_config('app.is_super_admin', <true|false>)
  • wrap connection with ResetOnCloseConnection
    ↓
PostgreSQL
  • RLS policies enforced (policies may inspect app.api_scope and app.is_super_admin)
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
-- standard: tenant must match current_tenant() and respect deleted_visibility
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

### Autoriser un SUPER_ADMIN à lire cross-tenant

Si vous souhaitez qu'un super-admin puisse lire des données cross-tenant (cas platform admin),
la policy peut consulter `current_setting('app.is_super_admin', true)` ou `current_setting('app.api_scope', true)`.
Exemple minimal qui autorise l'accès pour les lignes quand `app.is_super_admin = 'true'` :

```sql
(
  current_setting('app.is_super_admin', true) = 'true'
)
OR (
  tenant_id = current_tenant()
  AND (
    deleted_visibility() = 'all'
    OR (deleted_visibility() = 'active' AND deleted_at IS NULL)
    OR (deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
  )
)
```

Remarques :

- cette autorisation explicite doit être utilisée avec prudence (ne pas donner automatiquement
  tous les droits aux super-admins si ce n'est pas voulu);
- l'application met `app.is_super_admin` à `true` uniquement lorsque le `TchRequestContext`
  indique que l'appelant possède le rôle `SUPER_ADMIN` (via `TchContextFilter`).

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
  // accès DB normal → RLS appliqué (RlsAwareDataSource prendra les variables depuis le contexte)
} finally {
  TchContext.clear();
}
```

---

## 10) Règles absolues

- ❌ Jamais de filtre tenant dans le code (ne pas ajouter WHERE tenant_id = ? côté application)
- ❌ Jamais de DataSource par tenant
- ❌ Jamais de lookup tenant via JPA dans le DataSource
- ✅ PostgreSQL est la seule source de vérité

Notes spécifiques au nouveau design :

- `TchContextFilter` est responsable de la résolution du contexte de requête (scope, tenant code,
  override par header pour SUPER_ADMIN, deleted visibility) et construit un `TchRequestContext`.
- `RlsAwareDataSource` lit ce `TchRequestContext` via `TchContextResolver` et applique
  les `set_config(...)` nécessaires : `app.current_tenant`, `app.deleted_visibility`, `app.api_scope`,
  `app.is_super_admin`.
- `ResetOnCloseConnection` remet ces variables à des valeurs sûres à la fermeture de la connection
  (empêche fuite entre requêtes dans le pool).
