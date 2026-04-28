## Why

La règle d'architecture impose qu'une **Feature = BFF orchestrant ≥ 2 cores** pour un payload composite. Le CRUD mono-domaine appartient dans `core/<bc>/infra/web/<scope>/`, pas dans `features/`.

Quatre slices de `features/tenantadmin/` sont des dispatchers mono-domaine (ou quasi-mono) déguisés en features : `outlets`, `terminals`, `policies`, `users`. Leur présence dans `features/` crée :

- une couche `Orchestrator` sans valeur (re-wrap `CommandBus`/`QueryBus`)
- une confusion sur ce qui mérite une feature
- des violations de nommage et de typage à corriger lors du déplacement

Cette migration nettoie l'architecture des features, conforme au playbook et à `ARCHITECTURE.md`.

## What Changes

### 1. `features/tenantadmin/outlets/` → `core/outlet/infra/web/admin/`

- Créer `OutletAdminController` dans `com.tchalanet.server.core.outlet.infra.web.admin`
- Path : `/admin/outlets` — dispatch direct via `CommandBus`/`QueryBus`
- Supprimer `TenantAdminOutletsOrchestrator` et `TenantAdminOutletsController`
- Déplacer les DTOs (`OutletResponse`, `OutletWebMapper`, `CreateOutletReq`) dans `core.outlet.infra.web.admin.model`
- Supprimer `features/tenantadmin/outlets/` intégralement

### 2. `features/tenantadmin/terminals/` → `core/pos/infra/web/admin/`

- Créer `TerminalAdminController` dans `com.tchalanet.server.core.pos.infra.web.admin`
- Path : `/admin/terminals` (fix : supprimer `${tch.web.paths.tenant_admin}`)
- Dispatch direct via `CommandBus`/`QueryBus` ; supprimer `TenantAdminTerminalsOrchestrator`
- Corriger les violations dans le controller existant :
  - `registerDevice` : retourner `ApiResponse<TerminalId>` (typed ID) au lieu de `UUID` brut
  - `sendHeartbeat` : retourner `ApiResponse<Void>` au lieu de `void`
  - `LockRequest.actorId`, `UnlockRequest.actorId`, `UpdateMetadataRequest.actorId`, `UnregisterRequest.actorId` : `UUID` → `UserId`
- Supprimer `features/tenantadmin/terminals/` intégralement

### 3. `features/tenantadmin/policies/` → split selon les domaines

L'orchestrateur touche **2 cores** (`core.limitpolicy` + `core.autonomy`) — ce n'est pas mono-domaine pur.
Le `getPoliciesOverview()` agrège les deux → reste multi-domaine.

**Split en 3 destinations :**

- **`core/limitpolicy/infra/web/admin/LimitPolicyAdminController`** : endpoints CRUD definitions + assignments (mono `limitpolicy`)

  - `GET/PUT/DELETE /admin/policies/definitions`
  - `GET/PUT/DELETE /admin/policies/assignments`

- **`core/autonomy/infra/web/admin/AutonomyAdminController`** (à créer) : endpoints autonomy rules (mono `autonomy`)

  - `GET/PUT /admin/policies/autonomy`

- **`features/tenantadmin/config/` (inchangé)** : `GET /admin/policies/overview` reste une feature légitime car agrège limitpolicy + autonomy en un payload composite → `TenantAdminConfigOverviewController` ou nouvel endpoint consolidé

- Supprimer `TenantAdminPoliciesOrchestrator`, `TenantAdminPoliciesController`, `TenantAdminPoliciesLimitsController`, `TenantAdminPoliciesAutonomyController`
- Supprimer `features/tenantadmin/policies/` intégralement

### 4. `features/tenantadmin/users/` → `core/tenantuser/infra/web/admin/`

L'orchestrateur touche **3 cores** (`core.user`, `core.tenantuser`, `core.accesscontrol`). C'est multi-domaine.
Cependant, toutes les actions sont déclenchées par une **seule intention** (gérer un utilisateur dans le contexte tenant) et l'entité primaire est `TenantUser`. Le controller multi-dispatch direct est conforme : 1 HTTP → N `commandBus.send()` sans logique métier dans le controller.

- Enrichir `TenantUserAdminController` dans `com.tchalanet.server.core.tenantuser.infra.web.admin` avec tous les endpoints du `TenantAdminUsersController` (actuellement stub partiel)
- Path : `/admin/users` (fix : supprimer le `}` parasite dans `@RequestMapping("/admin}/users")`)
- Supprimer `TenantAdminUsersOrchestrator` — la séquence CreateUser + Assign + SetRole est inlinée directement dans le controller (acceptable : séquençage de commandes, pas de logique métier)
- Déplacer `TenantUserWebMapper` et les DTOs dans `core.tenantuser.infra.web.admin.model`
- Supprimer `features/tenantadmin/users/` intégralement

### Ce qui reste dans `features/tenantadmin/`

- `config/identity` : orchestre `core.tenantconfig` + `core.tenanttheme` + `core.subscription` → feature légitime, **NE PAS TOUCHER**
- `config/settings` et `config/i18n` : traitement dans une spec `platformadmin` séparée, **NE PAS TOUCHER**
- `config/TenantAdminConfigOverviewController` : agrège config + policies overview → **conserver, potentiellement absorber l'overview policies**

## Capabilities

### New Capabilities

<!-- aucune nouvelle capability fonctionnelle — refactoring structurel pur -->

### Modified Capabilities

- `auth-rbac`: les conventions d'autorisation (`TENANT_ADMIN + SUPER_ADMIN`) sont standardisées via `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")` sur tous les controllers migrés (passages de `hasAnyRole` à `hasAnyAuthority` pour cohérence projet)

## Impact

- **Java sources** : 4 controllers feature supprimés + 4 orchestrateurs supprimés ; 4 controllers core créés/enrichis ; packages DTOs déplacés
- **HTTP paths** : aucun changement fonctionnel — les paths `/admin/outlets`, `/admin/terminals`, `/admin/policies/*`, `/admin/users` restent identiques; seule correction : fix du `${tch.web.paths.tenant_admin}` dans terminals et du `}` parasite dans users
- **Aucune migration Flyway** — base recréée from scratch
- **Tests** : les tests existants des orchestrateurs et controllers features doivent être migrés vers les controllers core cibles
- **Docs** : `FEATURE_TENANT_ADMIN.md` et `ARCHITECTURE.md` mis à jour
