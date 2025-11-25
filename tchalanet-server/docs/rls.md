# Row-Level Security (RLS) — Tchalanet

## 1. Objectif

Le RLS garantit qu’un utilisateur ne voit que les données de _son tenant_ + gère la visibilité des soft deletes.

RLS repose sur **2 variables de session** :

| Variable                 | Type                              | Rôle                            |
| ------------------------ | --------------------------------- | ------------------------------- |
| `app.current_tenant`     | UUID                              | Filtrer les données d’un tenant |
| `app.deleted_visibility` | text (`active`, `deleted`, `all`) | Filtrer soft delete             |

---

## 2. Fonctions SQL

### set_current_tenant

```sql
CREATE OR REPLACE FUNCTION set_current_tenant(p uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  PERFORM set_config('app.current_tenant', p::text, true);
END $$;
```

### current_tenant

```sql
CREATE OR REPLACE FUNCTION current_tenant()
RETURNS uuid LANGUAGE sql STABLE AS
$$ SELECT current_setting('app.current_tenant', true)::uuid; $$;
```

### set_deleted_visibility

```sql
CREATE OR REPLACE FUNCTION set_deleted_visibility(p text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  IF p NOT IN ('active','deleted','all') THEN
    PERFORM set_config('app.deleted_visibility','active',true);
  ELSE
    PERFORM set_config('app.deleted_visibility',p,true);
  END IF;
END $$;
```

---

## 3. Politiques RLS

Chaque table tenant applique :

```sql
tenant_id = current_setting('app.current_tenant')::uuid
AND (
    visibility = 'all'
 OR (visibility='active'  AND deleted_at IS NULL)
 OR (visibility='deleted' AND deleted_at IS NOT NULL)
)
```

---

## 4. Côté Spring

### RequestUserContextFilter

Extrait :

- tenant du JWT
- rôles (SYSTEM + CUSTOM)
- override possible pour SUPER_ADMIN

### DbAppRlsFilter

Applique :

```sql
SELECT set_current_tenant(?);
SELECT set_deleted_visibility(?);
```

---

## 5. Tests

En test JUnit/JDBC :

```sql
SELECT set_current_tenant('tenant-dev');
SELECT set_deleted_visibility('all');
```

---

## 6. Jobs et batchs

En batch :

- Appliquer explicitement le tenant
- Pas de filtrage par deleted sauf si nécessaire

---

## 7. Résumé

✔ Multi‑tenant sécurisé via PostgreSQL  
✔ Support soft delete nativement  
✔ Override super‑admin  
✔ Compatible Testcontainers/Batch

# Row-Level Security (RLS) — Tchalanet

Ce document décrit la configuration RLS mise en place, l'API pour renseigner la session (tenant + deleted_visibility), et les recommandations d'usage.

## Principe

- On utilise PostgreSQL Row-Level Security pour garantir qu'une connexion/transaction ne voit que les données du `tenant` courant et selon la visibilité `deleted`/`active`/`all`.
- Deux variables session sont utilisées :
  - `app.current_tenant` (uuid)
  - `app.deleted_visibility` (text) values: `active` | `deleted` | `all`

## Fonctions SQL

- `set_current_tenant(p uuid)` — configure la variable de session `app.current_tenant` (use `set_config(..., true)` pour être local à la transaction/session).
- `current_tenant()` — renvoie le `uuid` de `app.current_tenant`.
- `set_deleted_visibility(p text)` — configure `app.deleted_visibility` (valeurs acceptées: `active` (default), `deleted`, `all`).

Ces fonctions sont déclarées dans `src/main/resources/db/migration/V2__rls_helpers.sql`.

## Politiques RLS (V8)

- Les politiques RLS sont créées pour chaque table listée dans `V8__rls_init_policies.sql`.
- La policy `*_rls_all` applique :
  - `tenant_id = current_setting('app.current_tenant', true)::uuid`
  - et filtre sur `deleted_at` selon `app.deleted_visibility`
- `WITH CHECK` garantit l'insertion/UPDATE n'échappe pas la contrainte tenant.

## Configuration côté application

- `DbAppRlsFilter` (Spring `OncePerRequestFilter`) applique, pour chaque requête:
  - lecture du `TchRequestContext` (via `RequestContextHolder`) -> `tenantId` et `systemRoles`
  - application des variables via JDBC `Connection`/`Statement`: `SELECT set_deleted_visibility('active')` et `SELECT set_current_tenant('<uuid>')`.
- Ce filtre doit s'exécuter après le filtre qui construit `TchRequestContext` (JWT parser `RequestUserContextFilter`).

## Overrides & Super-admin

- Seul un utilisateur avec le rôle `SUPER_ADMIN` peut passer l'override `X-Deleted-Visibility` ou `X-Tenant-Id`.
- `DbAppRlsFilter` vérifie le rôle dans le `TchRequestContext`.

## Cas spéciaux

- Jobs batch / CLI: pas de contexte HTTP. Les jobs doivent explicitement appeler `SELECT set_current_tenant(...)` dans leur logique ou utiliser une API utilitaire (helper) pour "s'installer" sur un tenant avant d'exécuter des opérations.
- Tests d'intégration: utiliser Testcontainers et appeler `set_current_tenant()` dans la session JDBC du test.

## Debugging

- Pour debug, tu peux exécuter dans la session SQL (psql) :

```sql
SELECT current_setting('app.current_tenant', true), current_setting('app.deleted_visibility', true);
```

- Vérifie les logs SQL (hibernate SQL) pour t'assurer que la variable est définie avant les requêtes critiques.

## Recommandations

- Centraliser la résolution du tenant (ne pas faire un lookup DB per query). Résoudre une fois dans `RequestUserContextFilter`.
- Utiliser `afterCommit` pour l'enregistrement d'audit non critique (audit_event) si tu veux t'assurer que l'audit est seulement persisté quand la transaction principale commit.
- Documenter dans les runbooks comment lancer des jobs multi-tenant (set_current_tenant per job run).
