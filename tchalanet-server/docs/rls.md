# Architecture RLS (Row-Level Security) — Tchalanet

## 1. Objectif

Tchalanet est une plateforme multi‑tenant stricte : toute requête SQL doit être automatiquement isolée par tenant, sans dépendre de filtres applicatifs (no @Where, no filters au niveau du code). La sécurité est déléguée à PostgreSQL via Row‑Level Security (RLS).

👉 Principe : laisser le moteur SQL (PostgreSQL) garantir l'isolation, l'application ne fait que renseigner le contexte (tenant + visibilité supprimés) sur la connexion JDBC.

---

## 2. Qu'est‑ce que le RLS (PostgreSQL)

RLS permet d'appliquer des règles au niveau des lignes selon :

- l'utilisateur (role),
- des variables de session (current_setting),
- ou des fonctions SQL.

Dans Tchalanet nous utilisons principalement deux variables de session :

- `app.current_tenant` — UUID du tenant courant
- `app.deleted_visibility` — `active` | `deleted` | `all`

Ainsi, toute requête (JPA/Hibernate, JDBC, native, Batch, scheduler...) est automatiquement filtrée par les policies RLS côté base.

---

## 3. Principe global dans Tchalanet (récapitulatif)

Variables session utilisées

| Variable                 | Rôle                         |
| ------------------------ | ---------------------------- |
| `app.current_tenant`     | UUID du tenant actif         |
| `app.deleted_visibility` | `active` / `deleted` / `all` |

Comportement attendu

- Les variables sont posées au moment où une connexion est empruntée (DataSource wrapper).
- Les variables sont réinitialisées automatiquement quand la connexion est rendue au pool (safety for pooled connections).

---

## 4. Vue d'ensemble technique

HTTP Request
↓
`RequestUserContextFilter` (construit `TchRequestContext`)
↓
`TchRequestContextHolder` (RequestScope)
↓
`RlsAwareDataSource` (DelegatingDataSource) — central
↓
PostgreSQL RLS policies

---

## 5. Composants clés

### 5.1 `TchRequestContext`

Contient le contexte per‑request :

- `originalTenantCode`, `effectiveTenantCode`
- `originalTenantUuid`, `effectiveTenantUuid` (peut être null au début)
- `deletedVisibility` (valeur demandée par l'utilisateur / SA)

⚠️ Le `tenantUuid` peut être résolu plus tard : la résolution finale peut se faire dans le `RlsAwareDataSource` si l'UUID n'est pas connu au moment de la construction du context.

### 5.2 `RlsAwareDataSource` (composant central)

- Type : `DelegatingDataSource` (wrapper autour du `DataSource` brut).
- Comportement :
  - Intercepte `getConnection()` (et `getConnection(user,pw)`), lit le `TchRequestContext` si le scope request est actif.
  - Si `tenantUuid` inconnu, effectue un lookup du tenant UUID _sur la même connexion_ (query sur la table `tenant`) pour éviter des connexions additionnelles et des cycles Spring.
  - Applique les variables session en exécutant des `SET` via `set_config(...)` (ou via fonctions helper si vous préférez) :
    - `select set_config('app.current_tenant', ?, true)`
    - `select set_config('app.deleted_visibility', ?, true)`
  - Wrappe la connexion retournée dans `ResetOnCloseConnection` qui réinitialise la session au `close()`.

Bénéfices :

- Fonctionne pour Hibernate bootstrap, Flyway, Batch, scheduler.
- Évite les cycles d'initialisation Spring (en faisant la résolution tenant UUID _dans_ la connexion).
- Pas d'appel JPA pour résoudre le tenant dans le DataSource ; lookup SQL direct sur la même connexion.

### 5.3 Pourquoi le lookup tenant UUID est fait dans le DataSource

- Évite les cycles de dépendances Spring (bean A dépend de DataSource qui dépend de bean A).
- Évite d’ouvrir des connexions supplémentaires ou d’appeler des repositories JPA dans la phase d’acquisition de connexion.
- Garantit que la première requête exécutée sur la connexion sera déjà isolée.

⚠️ La table `tenant` NE DOIT PAS être protégée par RLS (sinon le lookup échouera).

### 5.4 `ResetOnCloseConnection`

- Wrapper de `Connection` qui intercepte `close()` et exécute :
  - `SELECT reset_rls_context()` (implémentée en SQL dans V1), ou utilise `set_config('app.current_tenant','',true)` + `set_config('app.deleted_visibility','active',true)`.
- Empêche la fuite de contexte entre connexions dans le pool (HikariCP, etc.).

---

## 6. Fonctions SQL (migrations Flyway)

Dans `V1__extensions_and_functions.sql` on ajoute :

- `deleted_visibility()` — getter safe (whitelist)
- `set_deleted_visibility(p text)` — setter safe
- `set_current_tenant(p uuid)` — setter safe
- `current_tenant()` — getter retournant uuid ou null
- `reset_rls_context()` — remet `app.current_tenant` et `app.deleted_visibility` à une valeur neutre (utilisé par `ResetOnCloseConnection`)

> Remarque : l'application Java n'appelle pas directement ces fonctions sauf `reset_rls_context()` via le ResetOnClose wrapper — la mise en session se fait via `set_config(...)` pour garantir la portée session/connexion.

---

## 7. Policies RLS (exemples)

### 7.1 Tables multi‑tenant + soft delete

Condition typique :

```
tenant_id = current_tenant()::uuid
AND (
  deleted_visibility() = 'all'
  OR (deleted_visibility() = 'active' AND deleted_at IS NULL)
  OR (deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
)
```

### 7.2 Tables multi‑tenant sans soft delete

```
tenant_id = current_tenant()::uuid
```

---

## 8. Checklist OBLIGATOIRE pour ajouter une nouvelle entité

1. Identifier le type de table :

   - Global (pas de RLS) — ex: `tenant`, `country`, `currency`.
   - Multi‑tenant — ex: `ticket`, `outlet`.
   - Multi‑tenant + soft delete — la plupart des tables métier.

2. Colonnes requises :

   - `tenant_id UUID NOT NULL` pour une table multi‑tenant.
   - `deleted_at timestamptz` si soft‑delete.

3. Mise à jour des migrations RLS

   - Dans `V40__rls_policies.sql` (ou votre script RLS central) ajouter la table :
     - `soft_tables := ARRAY['new_table_name'];` ou
     - `tenant_only_tables := ARRAY['new_table_name'];`
   - Le script vérifie la présence des colonnes et applique la policy.

4. Ne JAMAIS filtrer par tenant côté JPA
   - ❌ Mauvais : `where tenant_id = :tenantId` dans le code
   - ✅ Correct : `findByStatus(...)` — laisser PostgreSQL appliquer le filtre via RLS

---

## 9. Règles d'or (à ne jamais violer)

- ❌ Ne pas utiliser `@Where(tenant_id = ...)` ou filtres Hibernate globaux pour le tenant.
- ❌ Ne pas créer un DataSource par tenant.
- ❌ Ne pas faire un lookup tenant via JPA à l'intérieur du DataSource.
- ✅ Une seule source de vérité : PostgreSQL via RLS.

---

## 10. Bénéfices clés

- Isolation forte et centralisée côté DB.
- Impossible d'oublier un filtre côté applicatif.
- Batch / API / Admin protégés de façon homogène.
- Compatible audit & reporting.
- Scalable pour du multi‑tenant réel.

---

## 11. Résumé exécutif

Dans Tchalanet le tenant n'est pas une option applicative : c'est une propriété du moteur SQL.
Chaque ligne sait à qui elle appartient, chaque requête est filtrée par défaut. Le code métier reste simple et sécurisé.

---

## 12. Useful actions (propositions)

- Générer `ARCHITECTURE.md` prêt à commit (si souhaité).
- Générer une checklist PR « nouvelle entité » (script/template) pour automatiser la vérification.
- Ajouter des tests unitaires/integration pour :
  - `RlsAwareDataSource` (mock DataSource/Connection)
  - `ResetOnCloseConnection` (vérifier reset au close)
  - migration SQL (Testcontainers + Flyway)

---

Fin de la documentation RLS mise à jour.
