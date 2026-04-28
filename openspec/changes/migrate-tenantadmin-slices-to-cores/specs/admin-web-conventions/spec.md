## ADDED Requirements

### Requirement: Controller admin dans le package core cible

Tout controller HTTP gérant le CRUD d'un seul bounded context SHALL résider dans `core/<bc>/infra/web/admin/` et non dans `features/`. Un controller admin dans `features/` est uniquement acceptable s'il produit un **payload composite** agrégeant des données de 2 bounded contexts distincts ou plus.

#### Scenario: Controller mono-domaine dans core

- **WHEN** un endpoint HTTP ne lit ou n'écrit que des données d'un seul bounded context
- **THEN** le controller est dans `core/<bc>/infra/web/admin/` et dispatche directement via `CommandBus`/`QueryBus`

#### Scenario: Absence d'orchestrateur intermédiaire

- **WHEN** un controller ne fait que transposer une requête HTTP en commande ou requête bus
- **THEN** aucun `@Service` orchestrateur n'est interposé entre le controller et le bus

### Requirement: OutletAdminController dans core.outlet

`OutletAdminController` SHALL être dans `com.tchalanet.server.core.outlet.infra.web.admin`, path `/admin/outlets`, dispatching via `CommandBus`/`QueryBus` uniquement. `TenantAdminOutletsController` et `TenantAdminOutletsOrchestrator` ne DOIVENT plus exister.

#### Scenario: Endpoint GET /admin/outlets via bus

- **WHEN** GET `/admin/outlets` est appelé par un TENANT_ADMIN
- **THEN** `OutletAdminController` dispatche `ListOutletsByTenantQuery` via `QueryBus` et retourne `ApiResponse<List<OutletResponse>>`

#### Scenario: Endpoint POST /admin/outlets via bus

- **WHEN** POST `/admin/outlets` est appelé avec un body valide
- **THEN** `OutletAdminController` dispatche `CreateOutletCommand` via `CommandBus` et retourne `ApiResponse<OutletId>` (typed ID, pas UUID brut)

### Requirement: TerminalAdminController dans core.pos avec path corrigé

`TerminalAdminController` SHALL être dans `com.tchalanet.server.core.pos.infra.web.admin`, path `/admin/terminals` (valeur littérale, pas `${tch.web.paths.tenant_admin}`). Tous les endpoints DOIVENT retourner `ApiResponse<T>`. Les request DTOs DOIVENT utiliser des typed IDs (`UserId`) au lieu de `UUID` brut pour les champs `actorId`.

#### Scenario: Path fixe /admin/terminals

- **WHEN** le controller est défini
- **THEN** `@RequestMapping("/admin/terminals")` utilise une string littérale

#### Scenario: registerDevice retourne ApiResponse<TerminalId>

- **WHEN** POST `/admin/terminals` est appelé
- **THEN** la réponse est `ApiResponse<TerminalId>` (pas `UUID` brut direct)

#### Scenario: actorId typé UserId

- **WHEN** `LockRequest`, `UnlockRequest`, `UpdateMetadataRequest`, `UnregisterRequest` sont définis
- **THEN** le champ `actorId` est de type `UserId` (pas `UUID`)

### Requirement: LimitPolicyAdminController et AutonomyAdminController dans leurs cores

Les endpoints CRUD de limit definitions/assignments SHALL être dans `core.limitpolicy.infra.web.admin.LimitPolicyAdminController`. Les endpoints CRUD d'autonomy rules SHALL être dans `core.autonomy.infra.web.admin.AutonomyAdminController`. L'overview multi-domaine `/admin/policies/overview` DOIT rester dans `features/tenantadmin/`.

#### Scenario: LimitPolicy endpoints dans core.limitpolicy

- **WHEN** GET/PUT/DELETE `/admin/policies/definitions` ou `/admin/policies/assignments` est appelé
- **THEN** le controller responsable est dans `core.limitpolicy.infra.web.admin`

#### Scenario: Autonomy endpoints dans core.autonomy

- **WHEN** GET/PUT `/admin/policies/autonomy` est appelé
- **THEN** le controller responsable est dans `core.autonomy.infra.web.admin`

#### Scenario: Overview reste feature

- **WHEN** GET `/admin/policies/overview` est appelé (payload composite limitpolicy + autonomy)
- **THEN** le controller responsable est dans `features/tenantadmin/` (légitime : agrège ≥ 2 cores)

### Requirement: TenantUserAdminController enrichi dans core.tenantuser

`TenantUserAdminController` dans `com.tchalanet.server.core.tenantuser.infra.web.admin` SHALL implémenter tous les endpoints actuellement sur `TenantAdminUsersController`, avec dispatch direct via `CommandBus`/`QueryBus`. Le path SHALL être `/admin/users`.

#### Scenario: Path /admin/users sans caractère parasite

- **WHEN** le controller est défini
- **THEN** `@RequestMapping("/admin/users")` est sans le `}` parasite

#### Scenario: Create+Assign+SetRole sans orchestrateur

- **WHEN** POST `/admin/users` est appelé pour créer un utilisateur
- **THEN** le controller dispatche séquentiellement `CreateUserCommand`, `AssignUserToTenantCommand`, `SetTenantUserRoleCommand` via `commandBus` sans `@Service` intermédiaire

### Requirement: Autorisation via hasAnyAuthority

Tous les controllers admin migrés SHALL utiliser `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")` et non `hasAnyRole(...)`.

#### Scenario: Autorisation cohérente avec le projet

- **WHEN** un controller admin est annoté
- **THEN** l'annotation utilise `hasAnyAuthority` (compatible Keycloak authorities) et non `hasAnyRole`

### Requirement: @AuditLog sur les mutations

Tous les endpoints de création, modification et suppression dans les controllers migrés SHALL porter `@AuditLog`. Les lectures (GET) n'en ont pas besoin.

#### Scenario: Mutation avec AuditLog

- **WHEN** un endpoint POST/PUT/PATCH/DELETE est défini dans un controller admin migré
- **THEN** l'annotation `@AuditLog` est présente sur la méthode

### Requirement: Suppression des slices features après migration

`features/tenantadmin/outlets/`, `features/tenantadmin/terminals/`, `features/tenantadmin/policies/` (sauf le controller overview) et `features/tenantadmin/users/` DOIVENT être supprimés intégralement après que leurs remplaçants core soient opérationnels et testés.

#### Scenario: Aucun import vers les packages supprimés

- **WHEN** les slices features sont supprimées
- **THEN** `grep -r "features.tenantadmin.outlets\|features.tenantadmin.terminals\|features.tenantadmin.users" src/` retourne 0 résultat
