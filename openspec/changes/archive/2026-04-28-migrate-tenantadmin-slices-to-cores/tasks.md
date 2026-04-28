## Status: DRAFT

---

## Slice 1 — outlets → core.outlet.infra.web.admin

### 1.1 Vérification préalable

- [ ] `grep -r "features.tenantadmin.outlets" tchalanet-server/src/` — inventorier tous les imports

### 1.2 Création du controller

- [ ] Créer `OutletAdminController.java` dans `com.tchalanet.server.core.outlet.infra.web.admin`
  - `@RestController`
  - `@RequestMapping("/admin/outlets")`
  - `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - Constructor injection : `CommandBus`, `QueryBus`
  - Endpoints :
    - `GET /` → `ListOutletsByTenantQuery(tenantId)` → `ApiResponse<List<OutletSummary>>`
    - `GET /{id}` → `GetOutletByIdQuery(tenantId, id)` → `ApiResponse<OutletView>`
    - `POST /` `@AuditLog` → `CreateOutletCommand(tenantId, ...)` → `ApiResponse<OutletId>`
    - `PATCH /{id}/config` `@AuditLog` → `UpdateOutletConfigCommand(tenantId, id, patch)` → `ApiResponse<Void>`
- [ ] Créer les DTOs nécessaires dans `core.outlet.infra.web.admin.model` en reprenant la logique de `OutletResponse` et `OutletWebMapper`
  - `OutletAdminRequest` record (nom, slug, address)
  - `OutletAdminResponse` record (ou réutiliser `OutletView` si suffisant)
- [ ] `@CurrentContext TchRequestContext ctx` sur tous les endpoints ; `tenantId` extrait de `ctx.tenantIdSafe()`

### 1.3 Suppression

- [ ] Supprimer `TenantAdminOutletsController.java`
- [ ] Supprimer `TenantAdminOutletsOrchestrator.java`
- [ ] Supprimer `OutletWebMapper.java` de features (si non utilisé ailleurs)
- [ ] Supprimer `features/tenantadmin/outlets/model/OutletResponse.java`
- [ ] `git rm -r features/tenantadmin/outlets/`
- [ ] Vérifier `grep -r "features.tenantadmin.outlets"` → 0 résultats

### 1.4 Tests

- [ ] Créer/migrer `OutletAdminControllerTest` (MockMvc) : `GET`, `POST`, `PATCH`
- [ ] `./mvnw test -pl tchalanet-server -Dtest=OutletAdminControllerTest`

---

## Slice 2 — terminals → core.pos.infra.web.admin

### 2.1 Vérification préalable

- [ ] `grep -r "features.tenantadmin.terminals" tchalanet-server/src/` — inventorier imports
- [ ] Vérifier `application.yaml` de tous les profils : la propriété `tch.web.paths.tenant_admin` est-elle définie et différente de `/api/v1/tenant-admin` ?

### 2.2 Création du package et du controller

- [ ] Créer le package `com.tchalanet.server.core.pos.infra.web.admin`
- [ ] Créer `TerminalAdminController.java` :
  - `@RequestMapping("/admin/terminals")` — string littérale, plus de `${...}`
  - `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - Corriger les retours manquants :
    - `POST /` `@AuditLog` → return `ApiResponse<TerminalId>` (pas `UUID`)
    - `POST /{id}/heartbeat` → return `ApiResponse<Void>`
  - Corriger les typed IDs dans les request records :
    - `LockRequest(UserId actorId, String reason)`
    - `UnlockRequest(UserId actorId)`
    - `UpdateMetadataRequest(UserId actorId, Map<String, Object> metadataPatch, boolean heartbeatAlso)`
    - `UnregisterRequest(UserId actorId, String reason)`
  - `@AuditLog` sur : `POST /`, `POST /{id}/lock`, `POST /{id}/unlock`, `PUT /{id}/metadata`, `DELETE /{id}`
  - Déplacer le record `TerminalResponse` dans `core.pos.infra.web.admin.model`

### 2.3 Suppression

- [ ] Supprimer `TenantAdminTerminalsController.java`
- [ ] Supprimer `TenantAdminTerminalsOrchestrator.java`
- [ ] Supprimer `features/tenantadmin/terminals/model/TerminalResponse.java`
- [ ] `git rm -r features/tenantadmin/terminals/`
- [ ] Vérifier `grep -r "features.tenantadmin.terminals"` → 0 résultats

### 2.4 Tests

- [ ] Créer/migrer `TerminalAdminControllerTest` (MockMvc)
- [ ] `./mvnw test -pl tchalanet-server -Dtest=TerminalAdminControllerTest`

---

## Slice 3 — policies → split limitpolicy + autonomy

### 3.1 Vérification préalable

- [ ] `grep -r "features.tenantadmin.policies" tchalanet-server/src/` — inventorier imports
- [ ] Identifier précisément les classes des sous-controllers : `TenantAdminPoliciesLimitsController`, `TenantAdminPoliciesAutonomyController`, `TenantAdminPoliciesController`

### 3.2 LimitPolicyAdminController (core.limitpolicy)

- [ ] Créer le package `com.tchalanet.server.core.limitpolicy.infra.web.admin`
- [ ] Créer `LimitPolicyAdminController.java` :
  - `@RequestMapping("/admin/policies")`
  - `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - Endpoints limit definitions :
    - `GET /definitions` → `ListLimitDefinitionsQuery`
    - `PUT /definitions` `@AuditLog` → `UpsertLimitDefinitionCommand`
    - `DELETE /definitions/{id}` `@AuditLog` → `DeleteLimitDefinitionCommand`
  - Endpoints limit assignments :
    - `GET /assignments` → `ListLimitAssignmentsByTargetQuery`
    - `PUT /assignments` `@AuditLog` → `UpsertLimitAssignmentCommand`
    - `DELETE /assignments/{id}` `@AuditLog` → `DeleteLimitAssignmentCommand`
  - DTOs : reprendre `UpsertLimitDefinitionRequest`, `UpsertLimitAssignmentRequest` depuis `features.tenantadmin.policies.model` → déplacer dans `core.limitpolicy.infra.web.admin.model`

### 3.3 AutonomyAdminController (core.autonomy)

- [ ] Créer le package `com.tchalanet.server.core.autonomy.infra.web.admin`
- [ ] Créer `AutonomyAdminController.java` :
  - `@RequestMapping("/admin/policies/autonomy")`
  - `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - `GET /` → `GetAutonomyOverviewQuery`
  - `PUT /` `@AuditLog` → `UpsertAutonomyPolicyRuleCommand` (via `CommandBus` — **pas d'injection directe du handler**)
  - DTO : reprendre `UpsertAutonomyRuleRequest` → déplacer dans `core.autonomy.infra.web.admin.model`

### 3.4 Overview reste dans features (légitime)

- [ ] Conserver `TenantAdminPoliciesController` avec uniquement `GET /overview` (payload composite limitpolicy + autonomy)
- [ ] Supprimer `TenantAdminPoliciesOrchestrator` et en inliner la logique dans le controller restant si l'overview est le seul endpoint
- [ ] Supprimer `TenantAdminPoliciesLimitsController` (migré en 3.2)
- [ ] Supprimer `TenantAdminPoliciesAutonomyController` (migré en 3.3)
- [ ] Supprimer `PoliciesOverviewView` si dupliqué → conserver une seule version

### 3.5 Tests

- [ ] Créer `LimitPolicyAdminControllerTest` (MockMvc)
- [ ] Créer `AutonomyAdminControllerTest` (MockMvc)
- [ ] `./mvnw test -Dtest=LimitPolicyAdminControllerTest,AutonomyAdminControllerTest`

---

## Slice 4 — users → core.tenantuser.infra.web.admin

### 4.1 Vérification préalable

- [ ] `grep -r "features.tenantadmin.users" tchalanet-server/src/` — inventorier imports
- [ ] Consulter le `TenantUserAdminController` existant (stub) — identifier ce qui est déjà implémenté vs ce qui manque
- [ ] Vérifier que `@RequestMapping("/admin/tenant-users")` → sera changé en `/admin/users`

### 4.2 Enrichissement TenantUserAdminController

- [ ] Changer `@RequestMapping` : `/admin/tenant-users` → `/admin/users`
- [ ] Changer `@PreAuthorize` : `SUPER_ADMIN` seul → `hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')`
- [ ] Ajouter les endpoints manquants (tout ce que `TenantAdminUsersController` expose) :
  - `GET /bootstrap` → `GetCurrentUserQuery(ctx.userId())`
  - `GET /` paginé → `PagedListTenantUsersQuery(tenantId, pageReq)` (déjà présent — vérifier)
  - `GET /{userId}` → `GetCurrentUserQuery(userId)`
  - `POST /` `@AuditLog` → séquence : `CreateUserCommand` + `AssignUserToTenantCommand` + `SetTenantUserRoleCommand`
  - `PUT /{userId}` `@AuditLog` → `UpdateUserProfileCommand`
  - `PATCH /{userId}/preferences` `@AuditLog` → `UpdateUserPreferencesCommand`
  - `PUT /{userId}/membership` `@AuditLog` → `AssignUserToTenantCommand`
  - `DELETE /{userId}/membership` `@AuditLog` → `UnassignUserFromTenantCommand`
  - `PUT /{userId}/role` `@AuditLog` → `SetTenantUserRoleCommand`
- [ ] Supprimer la méthode privée `toResponse(TenantUserRow r)` stub — utiliser mapper ou direct si record
- [ ] Déplacer `TenantUserWebMapper` dans `core.tenantuser.infra.web.admin`
- [ ] Déplacer les DTOs de `features.tenantadmin.users.model` → `core.tenantuser.infra.web.admin.model`
  - `CreateUserRequest`, `UpdateUserRequest`, `UpdatePreferencesRequest`, `UpsertMembershipRequest`, `SetUserRoleRequest`
  - `TenantUserRow`, `TenantUserFilter`, `TenantUserResponse`, `TenantUserDetails`
- [ ] Ajouter `@CurrentContext TchRequestContext ctx` sur tous les endpoints
- [ ] **Ne PAS créer d'orchestrateur** — dispatch direct depuis le controller

### 4.3 Suppression

- [ ] Supprimer `TenantAdminUsersOrchestrator.java`
- [ ] Supprimer `TenantAdminUsersController.java`
- [ ] Supprimer `TenantUserWebMapper.java` de features (maintenant dans core)
- [ ] Supprimer tous les fichiers de `features/tenantadmin/users/model/`
- [ ] `git rm -r features/tenantadmin/users/`
- [ ] Vérifier `grep -r "features.tenantadmin.users"` → 0 résultats

### 4.4 Tests

- [ ] Migrer/créer `TenantUserAdminControllerTest` (MockMvc) : tous les endpoints
- [ ] `./mvnw test -Dtest=TenantUserAdminControllerTest`

---

## Documentation

- [ ] Mettre à jour `tchalanet-server/docs/tenant-admin/FEATURE_TENANT_ADMIN.md` :
  - Retirer "outlets", "terminals", "policies/limits", "policies/autonomy", "users" de la section "Mandatory decomposition"
  - Ajouter : "CRUD mono-domaine → `core/<bc>/infra/web/admin/`"
  - Garder : `config/identity` (feature légitime 3 cores), `config/policies/overview` (feature légitime 2 cores)
- [ ] Mettre à jour `tchalanet-server/docs/ARCHITECTURE.md` :
  - Section features actives : retirer les 4 slices migrées

---

## Vérification finale

- [ ] `./mvnw clean verify -pl tchalanet-server` → build vert + aucun test rouge
- [ ] `grep -r "features.tenantadmin.outlets\|features.tenantadmin.terminals\|features.tenantadmin.users" tchalanet-server/src/main` → 0 résultats
- [ ] `grep -r "features.tenantadmin.policies.model\|TenantAdminPoliciesOrchestrator" tchalanet-server/src/main` → 0 résultats
- [ ] Mettre à jour CHANGELOG (`REFACTOR: migrate tenantadmin mono-domain slices to respective core infra/web/admin`)
