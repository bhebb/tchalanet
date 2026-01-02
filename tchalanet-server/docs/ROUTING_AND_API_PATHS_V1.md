# Tchalanet — Routing & API Paths (v1) + Context / RLS Architecture

Objectif : documenter clairement

1) nos paths (`/api/v1/...`) et leur rôle,
2) comment ajouter de nouveaux endpoints,
3) l'architecture Context + Tenant resolution + RLS et le flow complet d'une requête HTTP.

---

## 0) Rappel : servlet path global

Nous utilisons une base stable et versionnée pour toutes les routes via Spring MVC :

```yaml
spring:
  mvc:
    servlet:
      path: ${app.base-path}/${app.api-version} # ex: /api/v1
```

Résultat : toutes les routes MVC sont servies sous `/api/v1/...`.

- Swagger UI : `http://host:port/api/v1/swagger-ui`
- OpenAPI JSON (springdoc) : `http://host:port/api/v1/openapi` et `http://host:port/api/v1/openapi/{group}`

Note importante : springdoc peut produire dans le JSON des paths sans le préfixe `/api/v1`. Donc, dans `GroupedOpenApi` on matche généralement les patterns sans le préfixe (ex. `/admin/**`).

---

## 1) Grands groupes d'API (Custom APIs)

On sépare les APIs custom en 4 familles principales + 1 interne (SDR) :

A) PUBLIC — `/api/v1/public/**`
- Accessible sans auth (ou auth optionnelle).
- Peut utiliser le default tenant (décision V1).
- Exemples : home/public pages, vérification de ticket publique, flux d'actualité.

B) PLATFORM — `/api/v1/platform/**`
- APIs globales plateforme / catalogue.
- En général réservées au SUPER_ADMIN.
- Tenant optionnel (souvent `null`).
- Exemples : jeux, themes, plans, app-settings globaux.

C) ADMIN — `/api/v1/admin/**`
- Administration d'un tenant (back-office tenant).
- Tenant requis. Auth obligatoire.
- Rôles attendus : `TENANT_ADMIN` (ou `SUPER_ADMIN`).
- Exemples : outlets, tenant-users, i18n overrides tenant.

D) TENANT — `/api/v1/tenant/**`
- APIs opérationnelles tenant (ventes, tickets, sessions...).
- Tenant requis. Auth obligatoire. Rôles métiers (cashier, operator, ...).

---

## 2) Spring Data REST (SDR) — Isolation sous `/api/v1/_sdr/**`

Pourquoi : SDR génère beaucoup d'endpoints (search/profile/projections) qui polluent Swagger et compliquent la sécu et le routing tenant.

Décision : isoler tout SDR sous un préfixe interne unique : `/api/v1/_sdr/**`.

Configuration (application.yml) :

```yaml
spring:
  data:
    rest:
      enabled: true
      base-path: /_sdr
      detection-strategy: annotated
```

Règles importantes :
- Tout `@RepositoryRestResource(path=...)` doit utiliser un seul segment (pas de slash). Exemple :
  - ✅ `path="app-settings"`
  - ❌ `path="platform/app-settings"`
- `detection-strategy: annotated` : SDR n'exporte QUE les repositories annotés `@RepositoryRestResource`.
- Au runtime, SDR exposera : `/api/v1/_sdr/<repo>` et `/api/v1/_sdr/<repo>/{id}` et `/api/v1/_sdr/<repo>/search/...`.

Sécurité : SDR est interne et doit être restreint (ex: `SUPER_ADMIN` seulement) — voir section Sécurité.

---

## 3) Comment ajouter un endpoint (convention projet)

### 3.1 Endpoints custom (recommandé)

Structure & emplacement :
- Package : `core.<bounded-context>.infra.web`
- Controllers : `@RestController` + `@RequestMapping("/public" | "/platform" | "/admin" | "/tenant")`
- Règle : NE PAS placer des endpoints business sous `/_sdr`.

Exemple :

```java
@RestController
@RequestMapping("/admin/outlets")
public class OutletAdminController {
  // ...
}
```

Le préfixe `/api/v1` est ajouté automatiquement par `spring.mvc.servlet.path` donc `@RequestMapping` doit démarrer par `/admin`, `/platform`, `/public`, etc.

### 3.2 Endpoints SDR (rarement)

Si tu veux exposer un repository via SDR :

```java
@RepositoryRestResource(exported = true, path = "outlets")
public interface OutletRepository extends JpaRepository<OutletEntity, UUID> {}
```

Il sera accessible sous `/api/v1/_sdr/outlets`. Utilise `exported = true` explicitement ; la config globale désactive l'export par défaut.

---

## 4) Swagger / OpenAPI groups

But : grouper la spec par scope (public/platform/admin/tenant) et éventuellement séparer SDR.

`GroupedOpenApi` recommended patterns (matcher sans `/api/v1`):
- public: `/public/**`
- platform: `/platform/**`
- admin: `/admin/**`
- tenant: `/tenant/**`
- sdr (optionnel): `/_sdr/**`

Bonus : pour SDR on peut normaliser les tags en `SDR • <Resource>` via un `OpenApiCustomizer` qui réécrit les tags pour les paths commençant par `/_sdr/`.

Note : springdoc UI permet de sélectionner un groupe via la dropdown. Garde le groupe `sdr` séparé pour éviter la pollution des groups 'admin' ou 'platform'.

---

## 5) Security (règles pratiques)

Matchers recommandés (adapter selon servlet path observé en runtime) :

- PermitAll:
  - `/api/v1/openapi/**`, `/api/v1/swagger-ui/**`, `/api/v1/public/**`, `/actuator/health`
- `SUPER_ADMIN` only:
  - `/api/v1/platform/**`
  - `/api/v1/_sdr/**` (SDR)
- `TENANT_ADMIN` or `SUPER_ADMIN`:
  - `/api/v1/admin/**`
- Authenticated (tenant roles):
  - `/api/v1/tenant/**`
- Par défaut : authenticated

Remarques :
- Selon la façon dont Spring Security voit les URLs (avec ou sans `servlet.path`), il peut être plus sûr de matcher LES DEUX variantes :
  - `"/admin/**"` et `"/api/v1/admin/**"`.
- Pour la sécurité SDR, on recommande de n'autoriser que des comptes INTERNAL/SUPER_ADMIN car ces endpoints donnent accès à des opérations CRUD larges.

---

## 6) Context & Tenant resolution (flow HTTP complet)

Objectif : à chaque requête HTTP, construire un contexte immuable et thread-local (`TchRequestContext`) contenant les informations essentielles (tenant, user, roles, requestId, ip, userAgent, deletedVisibility) et appliquer RLS.

### 6.1 Résumé du flow

1. Une requête arrive (ex: `GET /api/v1/admin/outlets`).
2. `TchContextFilter` (OncePerRequestFilter) s'exécute :
   - Résout `ApiScope` via `ApiScopeResolver.resolve(req)` (public/platform/admin/tenant/_sdr).
   - Si scope = PUBLIC, applique `defaultTenant` (optionnel en v1).
   - Construit `TchRequestContext` via `buildBaseContext(req, defaultTenant)` : récupère user from JWT (subject), roles, tenant claim, requestId, clientIp, locale, userAgent, etc.
   - Si scope == TENANT (admin/tenant):
     - Vérifie qu'un tenant code est disponible (original ou override). Sinon 403.
     - Résout `tenantUuid` via `TenantBootstrapLookup.findTenantUuidByCode` (rawDataSource bypassant RLS) + cache TTL.
     - Si absent -> 403.
     - Rebuild context avec `effectiveTenantUuid`.
   - Publish context :
     - `req.setAttribute(REQUEST_CONTEXT, ctx)` (legacy compat)
     - `TchContext.set(ctx)` (ThreadLocal)
     - Put MDC entries (tenant_original, tenant_effective, user, reqId,...)
   - `chain.doFilter(req, res)`
   - finally : `MDC.clear(); TchContext.clear();`

3. `RlsAwareDataSource`: lorsqu'une connexion est prise, on lit `TchContext.currentOrNull()` :
   - si `tenantUuid != null` => execute `SELECT set_config('app.current_tenant', '<uuid>', false)`
   - sinon => reset to empty: `set_config('app.current_tenant', '', false)`
   - also set `app.deleted_visibility` accordingly
   - wrap connection with `ResetOnCloseConnection` which resets these config vars on close.

4. Requête JPA/Hibernate s'exécute avec RLS activé via Postgres (policies lisent `current_setting('app.current_tenant')`).

5. Après le traitement, la connexion est fermée (ou rendue au pool). `ResetOnCloseConnection.close()` réinitialise `app.current_tenant` & `app.deleted_visibility` avant de réellement close() la connexion.

### 6.2 Pourquoi ThreadLocal (TchContext) ?
- Evite `@RequestScope` qui lève `ScopeNotActiveException` pour les jobs/batch async.
- Permet aux jobs / schedulers d'exécuter du code DB en fixant manuellement `TchContext.set(ctx)` avant l'opération.

---

## 7) TchRequestContext (contrat)

Champs importants :
- `originalTenantCode` (String)
- `effectiveTenantCode` (String)
- `effectiveTenantUuid` (UUID) — nullable
- `keycloakUserId` (String)
- `appUserId` (UUID) — rempli via bootstrap après lookup
- `systemRoles` (Set<TchRole>)
- `customRoles` (Set<String>)
- `locale`, `requestId`, `clientIp`, `userAgent`
- `tenantOverridden` (boolean)
- `deletedVisibility` (String: active|deleted|all)

Méthode utilitaire : `tenantid()` -> `TenantId.nullableOf(tenantUuid())` (sécurisé si tenantUuid null).

---

## 8) Tenant UUID lookup (TenantBootstrapLookup)

- Utiliser une source de données *raw* (bypass RLS) pour lookup code -> uuid.
- Cacher en mémoire (TTL, ex. 5 minutes) pour réduire la charge.
- API : `Optional<UUID> findTenantUuidByCode(String code)`.

---

## 9) RlsAwareDataSource & ResetOnCloseConnection

- `RlsAwareDataSource.getConnection()` applique `set_config('app.current_tenant', ...)` et `set_config('app.deleted_visibility', ...)` sur la connexion SQL.
- On retourne un proxy `ResetOnCloseConnection` qui lors de `close()` exécute `set_config('app.current_tenant','')` et `set_config('app.deleted_visibility','active')` avant de deleguer le close().
- Important pour les pools (Hikari) : sans reset, une connexion réutilisée conserverait le contexte du tenant précédent.

---

## 10) Batch / Scheduler

- Avant toute opération DB dans un job :

```java
try {
  TchContext.set(ctx); // ctx construit manuellement (tenantUuid etc.)
  // opération DB... via RlsAwareDataSource
} finally {
  TchContext.clear();
}
```

- Les jobs héritent alors du RLS par la variable de connexion.

---

## 11) Exemples pratiques

**ApiScopeResolver (extrait)**

```java
if (path.startsWith("/api/v1/_sdr") || path.startsWith("/_sdr")) {
  return ApiScope.PLATFORM; // traitée comme interne/no-tenant
}
```

**SecurityConfig (extrait)**

```java
.requestMatchers("/_sdr/**", "/api/v1/_sdr/**").hasRole("SUPER_ADMIN")
.requestMatchers("/api/v1/public/**", "/public/**").permitAll()
.requestMatchers("/api/v1/admin/**", "/admin/**").hasAnyRole("TENANT_ADMIN","SUPER_ADMIN")
```

**application.yaml (SDR)**

```yaml
spring:
  data:
    rest:
      enabled: true
      base-path: /_sdr
      detection-strategy: annotated
```

---

## 12) Checklist de validation

✅ Swagger groups : `/openapi/public`, `/openapi/platform`, `/openapi/admin`, `/openapi/tenant`, `/openapi/sdr` (optionnel)

✅ SDR isolé : `/api/v1/_sdr/**`

✅ Repos SDR : `@RepositoryRestResource(path = "<single-segment>", exported = true)`

✅ `TchContextFilter` : publie `REQUEST_CONTEXT`, `TchContext.set(ctx)`, MDC, et clear en finally

✅ `RlsAwareDataSource` : applique `set_config('app.current_tenant', ...)` seulement si tenantUuid != null, sinon reset to empty

✅ `ResetOnCloseConnection` : reset configs before close

✅ Batch : wrappers `TchContext.set(...)` / clear()

---

## 13) Décisions (v1)

- PUBLIC utilise `props.defaultTenant()` par défaut (considération client site public multi tenant).
- ADMIN + TENANT nécessitent un tenant résolu (uuid) — sinon 403.
- SDR isolé sous `/_sdr` et restreint à `SUPER_ADMIN`.
- Pas d'utilisation de `@RequestScope` pour le context holder ; on privilégie `ThreadLocal`.

---

## 14) FAQ / pitfalls

Q : Pourquoi `detection-strategy: annotated` ?
A : Pour éviter que tous les `JpaRepository` non-annotés soient exposés automatiquement.

Q : Pourquoi ne pas exposer tout via SDR ?
A : SDR produit beaucoup d'endpoints non contrôlés, complique la sécu, et pollue Swagger. Les APIs custom restent lisibles et contrôlées.

Q : Comment tester localement ?
A :
- Build : `./mvnw -DskipTests package`
- Run : `java -jar target/tchalanet-server-0.0.1-SNAPSHOT.jar`
- Open : `http://localhost:8083/api/v1/swagger-ui`

---

## 15) Actions recommandées pour les développeurs

1. Lorsque tu ajoutes un repo que tu veux exposer via SDR, ajoute `@RepositoryRestResource(exported = true, path = "<single-segment>")`.
2. Par défaut, implémente des controllers dans `core.<bc>.infra.web` pour toutes les APIs business.
3. Si tu modifies la structure RLS (nouvelles variables), mets à jour `RlsAwareDataSource` et `ResetOnCloseConnection`.
4. Avant d'exécuter un job, construis et set `TchContext` manuellement.

---

Si tu veux, je peux :
- générer une checklist automatisée qui vérifie les `@RepositoryRestResource` en code et te liste ceux qui sont `exported=true`;
- lancer une build et vérifier swagger openapi groups ici.

Dis ce que tu veux que je fasse ensuite.

