# Tchalanet — Diagnostic & Architecture cible Authentification, Autorisation, Identity, Context

**Statut :** proposition d’architecture cible  
**Date :** 2026-06-14  
**Auteur :** Architecture Tchalanet  
**Périmètre :** `platform.identity`, `platform.accesscontrol`, `common.context`, `app.config.security`, bootstrap public/tenant, clients web/mobile, batch/scheduler, RLS/DB

---

## 0. Note importante sur les sources

Ce document est une **vision d’architecture cible** basée sur :

- l’état actuel des packages fournis dans les zips `identity`, `context`, `accesscontrol` ;
- le `SecurityConfig` partagé ;
- les conventions Tchalanet déjà existantes ;
- les décisions récentes autour du provider-neutral authentication.

Certains documents du projet peuvent ne pas être complètement à jour. En cas de conflit, la stratégie de décision recommandée est :

1. **code réel inspecté** ;
2. **décisions récentes validées dans cette note** ;
3. `ARCHITECTURE.md` et `PLAYBOOK.md` ;
4. conventions techniques existantes ;
5. anciens documents OpenSpec/context.

Ce document doit donc servir de **référence de transition** pour refactorer progressivement l’authentification, l’autorisation et le contexte sans créer une usine à gaz.

---

## 1. Vision d’architecture Tchalanet

Tchalanet doit rester capable de changer de provider d’authentification sans changer son métier.

La décision centrale est :

```text
Provider externe = authentification seulement.
Tchalanet DB = identité applicative, tenants, rôles, permissions, contexte POS, RLS, audit.
```

Conséquence :

```text
Firebase / Keycloak / Clerk / LocalJwt / LocalPerf prouvent l'identité externe.
Ils ne décident jamais les rôles Tchalanet, les permissions, le tenant actif ou la capacité de vendre.
```

Phrase de vision :

```text
Même si demain Tchalanet remplace Firebase par Keycloak, Clerk ou un provider local,
aucun endpoint métier, aucun DTO public, aucun handler core et aucune table métier ne doivent changer.
```

Règle de conception :

```text
Identity ne construit pas l'écran.
Context ne décide pas les permissions.
AccessControl ne connaît pas Firebase.
Features composent les payloads clients.
Core revalide les opérations sensibles.
RLS commence après résolution du contexte.
Batch n'utilise jamais l'authentification externe.
```

---

## 2. Décisions structurantes

### 2.1 Pas de racine `/runtime`

Tchalanet a déjà des scopes HTTP structurants :

```text
/public/**
/tenant/**
/admin/**
/platform/**
/_sdr/**
```

Il ne faut donc pas ajouter une racine parallèle `/runtime/**`.

Endpoints cibles :

```http
GET /public/bootstrap
GET /tenant/me/bootstrap
GET /tenant/me/profile
PATCH /tenant/me/profile
GET /tenant/me/operational-context
POST /tenant/me/operational-context/select
DELETE /tenant/me/operational-context
```

Important : selon la configuration actuelle, `spring.mvc.servlet.path` injecte déjà `/api/v1`. Les controllers doivent donc déclarer des paths logiques comme `/tenant/me/bootstrap`, pas `/api/v1/tenant/me/bootstrap`.

---

### 2.2 Provider-neutral authentication

Le token externe doit être transformé en un modèle interne stable :

```java
public record ExternalAuthenticatedUser(
    IdentityProviderType provider,
    String issuer,
    String subject,
    String email,
    boolean emailVerified,
    Map<String, Object> safeClaims
) {}
```

Le backend ne doit pas dépendre de `realm_access`, `resource_access`, `tenant_code` ou de custom claims Firebase pour l’autorisation métier.

---

### 2.3 AppUser est l’identité applicative

Ordre de résolution cible :

```text
external token
  -> provider / issuer / subject
  -> app_user_external_identity
  -> app_user ACTIVE
  -> UserAccessSnapshot DB
  -> effective tenant
  -> RLS context
  -> business handlers
```

---

### 2.4 POS = auth + contexte opérationnel + device binding

Le POS ne peut pas vendre simplement parce que le user est authentifié.

Pour vendre, confirmer un payout ou synchroniser offline, il faut :

```text
AppUser actif
tenant membership actif
permission métier
terminal actif
outlet actif
session ouverte
device binding valide
signature device si opération sensible
re-check transactionnel dans le handler métier
```

---

## 3. Inventaire synthétique des classes inspectées

Les trois zips contiennent environ **233 fichiers Java**.

### 3.1 `platform.identity`

Classes principales observées :

```text
api/
  IdentityApi
  IdentityBootstrapFilter
  IdentityProviderApi
  IdentityProvisioningApi
  ExternalAuthenticatedUser
  VerifiedExternalToken
  IdentityProviderType
  IdentityVerificationPolicy
  IdentityProviderException
  ProvisionExternalUserRequest
  ProvisionedExternalUser

api/model/
  UserStatus
  TenantUserStatus
  AutonomyLevel
  ClientSurface
  ClientSurfacePolicy
  AppUserView
  CurrentUserView
  TenantMembershipView
  UserProfileView
  BootstrapCurrentUserRequest
  GetCurrentUserRequest
  GetUserProfileRequest
  UpdateUserProfileRequest
  AssignUserToTenantRequest
  UnassignUserFromTenantRequest
  ProvisionTenantUserRequest
  BootstrapUserResult
  CreateUserResult
  ProvisionTenantUserResult

internal/service/
  UserBootstrapService
  ExternalIdentityAppUserResolver
  CurrentUserProfileService
  TenantMembershipService
  TenantUserAdministrationService
  TenantUserProvisioningService
  ExternalIdentityLinkService
  UnsupportedIdentityProvisioningService
  UserBootstrapProperties
  AppUserBootstrapProductionGuard
  UserSubToUuidCache

internal/firebase/
  FirebaseIdentityProvider
  FirebaseJwtDecoderConfig
  FirebaseEmulatorJwtDecoderConfig
  FirebaseJwtTokenVerifier
  FirebaseTokenVerifier
  FirebaseAdminConfig
  FirebaseUserProvisionService
  FirebaseBootstrapSyncService
  FirebaseBootstrapSyncListener
  FirebaseRevocationChecker
  FirebaseAdminRevocationChecker
  FirebaseEmulatorRevocationChecker
  FirebaseIdentityProperties
  FirebaseBootstrapProperties
  FirebaseRevocationCheckMode

internal/keycloak/
  KeycloakIdentityProvider

internal/local/
  LocalJwtIdentityProvider
  LocalPerfIdentityProvider
  LocalJwtDecoderConfig
  LocalIdentitySupport
  LocalIdentityProperties
  LocalIdentityProductionGuard

internal/persistence/
  AppUserJpaEntity
  AppUserExternalIdentityJpaEntity
  TenantUserJpaEntity
  UserPreferenceJpaEntity
  AppUserJpaRepository
  AppUserExternalIdentityJpaRepository
  TenantUserJpaRepository
  UserPreferenceJpaRepository
  AppUserJpaAdapter
  TenantMembershipJpaAdapter
  UserPreferenceJpaAdapter
  IdentityPersistenceMapper

internal/web/
  UserBootstrapFilterImpl
  IdentityUserAdminController
  CurrentUserProfileController
  PlatformIdentitySyncOpsController
  TenantUserAdminViewAssembler
  MeResponse
  LandingResponse
  EffectiveUiContextResponse
  ProfileActionsResponse
  TenantContextResponse
  UserContextResponse
  UserPreferenceResponse
```

#### Diagnostic `identity`

Points solides :

- provider-neutral déjà amorcé ;
- `app_user_external_identity` existe ;
- `UserBootstrapFilterImpl` fait déjà le lien external identity -> `AppUser` ;
- Firebase, LocalJwt, LocalPerf et Keycloak sont isolés derrière des providers ;
- le provisioning backend-owned est déjà commencé.

Points à améliorer :

- `IdentityApi` expose encore des concepts trop larges ;
- certaines méthodes utilisent encore du `UUID` brut dans l’API ;
- `CurrentUserProfileController` mélange profile, landing, tenant context, UI context ;
- `identity` contient des modèles UI qui devraient vivre dans une feature BFF ;
- `TenantUserAdministrationService` et `TenantUserProvisioningService` risquent de devenir trop larges si on ajoute encore des responsabilités.

Règle cible :

```text
identity = AppUser + external identities + profile/preferences + membership simple + provider adapters + provisioning.
identity ≠ permissions.
identity ≠ navigation.
identity ≠ defaultRoute.
identity ≠ POS readiness.
```

---

### 3.2 `common.context`

Classes principales observées :

```text
common.context/
  TchRequestContext
  TchContext
  TchContextBinder
  TchContextResolver
  TchContextScope
  TchContextProperties
  ContextKeys

context/auth/
  AuthContextExtractor
  ExtractedAuthContext
  ActorContextResolver
  ActorAuthorizationContextResolver

context/jwt/
  SecurityClaims

context/tenant/
  TenantContextResolver
  TenantContextLookup
  TenantContextInfo

context/web/
  TchContextFilter
  TchRequestContextFactory
  ApiScopeResolver
  CurrentContext
  CurrentContextArgumentResolver
  CurrentContextWebMvcConfig

context/scope/
  ApiScope

context/operational/
  OperationalRequestContext
  OperationalContextHint
  OperationalContextResolver
  OperationalContextHeaderParser
  OperationalContextHeaders
  OperationalContextRole
  OperationalContextSource
  OperationalContextTrust
  TrustLevel
  PosOperationalContext
  SuperAdminOperationalContext
  MissingOperationalContextException
  UntrustedOperationalContextException

context/system/
  SystemContextProperties
```

#### Diagnostic `context`

Points solides :

- `TchContextFilter` existe comme point central HTTP ;
- `TchRequestContext` est déjà la structure canonique ;
- `TchContextScope` existe pour batch/startup/temp tenant ;
- `OperationalRequestContext` et les sources de confiance POS sont déjà modélisés ;
- `ApiScopeResolver` existe ;
- `TenantContextResolver` existe.

Points à améliorer :

- `AuthContextExtractor` est encore trop legacy Keycloak/JWT ;
- `TchRequestContext` contient encore `keycloakUserId`, qui n’est plus un concept provider-neutral ;
- `customRoles` semble servir de permissions, mais le nom est trompeur ;
- `systemRoles` et `customRoles` peuvent encore venir du JWT ;
- `TchContextFilter` doit être réordonné autour d’un `UserAccessSnapshot` DB ;
- le tenant override ne doit être validé qu’après chargement des permissions DB.

Règle cible :

```text
common.context assemble le contexte technique.
Il ne connaît pas Firebase, Keycloak, AppUser JPA, RoleRepository ou PermissionRepository.
```

---

### 3.3 `platform.accesscontrol`

Classes principales observées :

```text
api/
  AccessControlApi
  PermissionKeys
  RequiresPermission
  PermissionsDeniedException

api/permissionevaluator/
  TchPermissionEvaluator
  CheckPermissionsResult

api/model/request/
  CheckUserPermissionsRequest
  GetEffectivePermissionsRequest
  ListRolesRequest
  ListPermissionsRequest
  ListRolePermissionsRequest
  AssignRoleToUserRequest
  RemoveRoleFromUserRequest
  SetTenantUserRoleRequest
  CreateRoleRequest
  UpdateRoleRequest
  GrantPermissionToRoleRequest
  RevokePermissionToRoleRequest
  GrantUserPermissionRequest
  DenyUserPermissionRequest
  RemoveUserPermissionOverrideRequest
  BootstrapAccessControlRequest

api/model/result/
  CheckUserPermissionsResult
  BootstrapAccessControlResult

api/model/view/
  EffectivePermissionsView
  RoleView
  PermissionView
  RolePermissionView
  UserPermissionOverrideView

internal/service/
  AccessControlBootstrapService
  DatabaseActorAuthorizationContextResolver
  EffectivePermissionService
  EffectivePermissions
  PermissionRegistryService
  RoleCatalogService
  TenantUserRoleService
  UserPermissionOverrideService
  TenantUserDirectoryPort
  TenantUserRoleWriterPort
  TenantUserSnapshot
  Permission
  AuthCacheConfig

internal/persistence/
  AppRoleJpaEntity
  PermissionJpaEntity
  AppRolePermissionJpaEntity
  AppRolePermissionId
  TenantUserRoleJpaEntity
  UserPermissionOverrideJpaEntity
  AppRoleJpaRepository
  PermissionJpaRepository
  PermissionAdminJpaRepository
  PermissionHierarchyJpaRepository
  RoleAdminJpaRepository
  RolePermissionJpaRepository
  RolePermissionAdminJpaRepository
  TenantUserRoleJpaRepository
  UserPermissionOverrideJpaRepository
  PermissionCatalogAdminAdapter
  RolePermissionReaderAdapter
  RolePermissionRepositoryJpaAdapter
  RoleReaderJpaAdapter
  TenantUserRoleJpaAdapter

internal/web/
  AccessControlAdminController
  AccessControlWebMapper
  RoleAdminResponse
  PermissionResponse
  UpsertRoleRequest
  UpdateRolePermissionsRequest
```

#### Diagnostic `accesscontrol`

Points solides :

- `TchPermissionEvaluator` existe déjà ;
- `@RequiresPermission` existe ;
- permissions, rôles, role-permission matrix et user overrides existent ;
- `DatabaseActorAuthorizationContextResolver` est déjà le début du chargement DB ;
- `EffectivePermissionService` existe ;
- `AccessControlBootstrapService` existe ;
- l’admin controller est déjà présent.

Points à améliorer :

- `DatabaseActorAuthorizationContextResolver` devrait évoluer vers un vrai `UserAccessSnapshotResolver` ;
- `TchPermissionEvaluator` semble dépendre d’un tenant obligatoire, ce qui pose problème pour `/platform/**` ;
- le modèle DB ne distingue pas assez clairement rôles platform globaux et rôles tenant ;
- `PermissionHierarchyJpaRepository` lit une hiérarchie de rôles qui ne semble pas clairement modélisée partout ;
- `AuthCacheConfig` existe, mais il manque une vraie stratégie de cache déclarée et invalidable ;
- les permissions doivent devenir la source unique d’autorisation métier, pas un complément aux JWT authorities.

Règle cible :

```text
accesscontrol = rôles + permissions + UserAccessSnapshot + EffectiveTenantDecision + PermissionEvaluator.
accesscontrol ne dépend pas de Firebase, Keycloak ou du frontend.
```

---

## 4. Problème principal actuel

Le système est encore hybride :

```text
JWT claims / Keycloak roles
  + DB identity/accesscontrol
  + TchContextFilter
  + RLS
```

Le risque est que certaines décisions sensibles soient encore prises avant la résolution DB.

Exemples de risques :

```text
/platform/** autorisé par authority SUPER_ADMIN du JWT
tenant override validé trop tôt
roles extraits de realm_access/resource_access
TchRequestContext construit avec des roles token puis enrichi DB après coup
permissions platform difficiles à évaluer sans tenant
```

Architecture cible :

```text
JWT claims = identité externe seulement.
DB Tchalanet = autorisation complète.
```

---

## 5. SecurityConfig — diagnostic et cible

### 5.1 Problème actuel

Le `SecurityConfig` partagé contient :

```java
.requestMatchers("/api/v1/platform/**", "/platform/**")
.hasAnyAuthority("SUPER_ADMIN", "ROLE_SUPER_ADMIN")
```

Ce gate n’est pas provider-neutral. Il suppose qu’un token externe peut porter `SUPER_ADMIN`.

### 5.2 Cible recommandée

Court terme :

```java
.requestMatchers("/api/v1/platform/**", "/platform/**")
.authenticated()

.requestMatchers("/api/v1/admin/**", "/admin/**")
.authenticated()

.requestMatchers("/api/v1/tenant/**", "/tenant/**")
.authenticated()
```

Puis sur les controllers platform :

```java
@RequiresPermission(PermissionKeys.PLATFORM_ACCESS)
```

ou :

```java
@PreAuthorize("hasPermission(null, 'platform.access')")
```

### 5.3 JWT converter

Le converter peut continuer à créer une `Authentication`, mais ne doit plus injecter des authorities métier depuis :

```text
realm_access.roles
resource_access.roles
roles
custom claims provider
```

Cible :

```java
Collection<GrantedAuthority> auths = List.of(
    new SimpleGrantedAuthority("AUTHENTICATED_EXTERNAL")
);
```

Les rôles `SUPER_ADMIN`, `TENANT_ADMIN`, `CASHIER`, etc. doivent être reconstruits depuis la DB dans `platform.accesscontrol`.

### 5.4 SensitiveIdentityVerificationFilter

Le filtre `SensitiveIdentityVerificationFilter` est cohérent :

```text
mutating methods ou tenant override headers
  -> IdentityVerificationPolicy.SENSITIVE
```

Mais il doit rester une vérification **d’authentification renforcée**, pas une décision d’autorisation.

---

## 6. Architecture cible par module

### 6.1 `platform.identity`

Responsabilités :

```text
AppUser
external identities
provider adapters
profile/preferences
membership simple
user provisioning/invitation
bootstrap AppUser from external token
```

Structure cible :

```text
platform.identity
  api/
    IdentityApi
    IdentityProviderApi
    IdentityProvisioningApi
    ExternalAuthenticatedUser
    VerifiedExternalToken
    IdentityProviderType
    IdentityVerificationPolicy
    ProvisionExternalUserRequest
    ProvisionedExternalUser

  internal/bootstrap/
    UserBootstrapFilterImpl
    UserBootstrapService
    UserBootstrapProperties
    ExternalIdentityAppUserResolver
    AppUserBootstrapProductionGuard

  internal/provider/
    firebase/
    keycloak/
    local/

  internal/profile/
    CurrentUserProfileService
    UserPreferenceService

  internal/provisioning/
    TenantUserProvisioningService
    ExternalIdentityLinkService
    UnsupportedIdentityProvisioningService
    FirebaseUserProvisionService

  internal/persistence/
    entity/
    repository/
    adapter/
    mapper/

  internal/web/me/
    CurrentUserProfileController

  internal/web/admin/
    IdentityUserAdminController

  internal/web/ops/
    PlatformIdentitySyncOpsController
```

À sortir de `identity` :

```text
LandingResponse
EffectiveUiContextResponse
ProfileActionsResponse
TenantContextResponse
UserContextResponse
MeResponse si utilisé comme bootstrap global
```

Ces modèles doivent aller dans `features.tenantbootstrap`.

---

### 6.2 `platform.accesscontrol`

Responsabilités :

```text
permissions
roles
role-permission matrix
tenant user roles
platform user roles
user permission overrides
UserAccessSnapshot
EffectiveTenantDecision
PermissionEvaluator
```

Structure cible :

```text
platform.accesscontrol
  api/
    AccessControlApi
    RequiresPermission
    PermissionKeys
    UserAccessSnapshot
    TenantAccessView
    EffectiveTenantDecision

  api/model/request/
    CheckUserPermissionsRequest
    ResolveUserAccessSnapshotRequest
    ResolveEffectiveTenantRequest

  api/model/result/
    CheckUserPermissionsResult
    ResolveUserAccessSnapshotResult

  internal/snapshot/
    UserAccessSnapshotResolver
    UserAccessSnapshotReader
    AccessSnapshotCache
    AccessSnapshotInvalidator

  internal/tenant/
    EffectiveTenantResolver
    TenantOverridePolicy

  internal/permission/
    EffectivePermissionService
    PermissionRegistryService
    UserPermissionOverrideService

  internal/role/
    RoleCatalogService
    TenantUserRoleService
    PlatformUserRoleService

  internal/persistence/
    entity/
    repository/
    adapter/

  internal/web/
    AccessControlAdminController
```

`DatabaseActorAuthorizationContextResolver` doit devenir ou déléguer vers `UserAccessSnapshotResolver`.

---

### 6.3 `common.context`

Responsabilités :

```text
TchRequestContext
ThreadLocal binding
MDC binding
request id
locale/timezone
api scope
context lifecycle
operational context primitives
TchContextScope for batch/startup/temp tenant
```

Structure cible :

```text
common.context
  TchRequestContext
  TchContext
  TchContextBinder
  TchContextResolver
  TchContextScope
  ContextKeys

  web/
    TchContextFilter
    TchRequestContextFactory
    ApiScopeResolver
    CurrentContext
    CurrentContextArgumentResolver

  auth/
    ActorContext
    ActorContextResolver
    ActorAuthorizationContextResolver

  tenant/
    TenantContextResolver
    TenantContextLookup
    TenantContextInfo

  operational/
    OperationalRequestContext
    OperationalContextHint
    OperationalContextSource
    OperationalContextTrust
    TrustLevel
    OperationalContextResolver
```

À renommer progressivement :

```text
keycloakUserId -> externalSubject
customRoles -> permissions
systemRoles -> roles
```

---

### 6.4 `features.publicbootstrap`

Responsabilité : composer le bootstrap public.

Endpoint :

```http
GET /public/bootstrap
```

Structure :

```text
features.publicbootstrap
  web/PublicBootstrapController
  app/PublicBootstrapService
  model/PublicBootstrapResponse
```

Réponse cible :

```java
public record PublicBootstrapResponse(
    String tenantCode,
    ThemeView theme,
    I18nBundleView i18n,
    PublicPageModelView page,
    List<ApiNotice> notices
) {}
```

---

### 6.5 `features.tenantbootstrap`

Responsabilité : composer le bootstrap privé web/mobile.

Endpoint :

```http
GET /tenant/me/bootstrap
```

Structure :

```text
features.tenantbootstrap
  web/TenantBootstrapController
  app/TenantBootstrapService
  model/TenantBootstrapResponse
```

Réponse cible :

```java
public record TenantBootstrapResponse(
    CurrentUserSummary user,
    TenantSummary tenant,
    Set<String> roles,
    Set<String> permissions,
    Set<ClientSurface> availableSurfaces,
    ClientSurface preferredSurface,
    String defaultRoute,
    NavigationModel navigation,
    UserPreferenceResponse preferences,
    List<ApiNotice> notices
) {}
```

---

### 6.6 `features.tenantbootstrap.operational`

Responsabilité V0 : composer la vue opérationnelle POS/admin selection.

Endpoints :

```http
GET    /tenant/me/operational-context
POST   /tenant/me/operational-context/select
DELETE /tenant/me/operational-context
```

Structure V0 :

```text
features.tenantbootstrap.operational
  web/OperationalContextController
  app/OperationalContextService
  model/OperationalContextResponse
```

Si ça grossit plus tard, extraire vers :

```text
features.posruntime
```

---

## 7. Pipeline HTTP cible

Pipeline cible :

```text
BearerTokenAuthenticationFilter
  -> JwtDecoder provider courant
  -> IdentityProviderApi.mapVerifiedToken()
  -> Authentication.details = ExternalAuthenticatedUser

SensitiveIdentityVerificationFilter
  -> re-check SENSITIVE si mutation ou override header

UserBootstrapFilter
  -> ExternalAuthenticatedUser
  -> app_user_external_identity
  -> app_user ACTIVE
  -> request attribute BOOTSTRAPPED_APP_USER_ID

TchContextFilter
  1. resolve ApiScope
  2. create base context: requestId, locale, ip, idempotency key
  3. attach appUserId
  4. load UserAccessSnapshot from DB/cache
  5. resolve effective tenant for scope
  6. validate tenant override using DB permissions
  7. bind tenant/RLS
  8. attach operational context hint
  9. bind final TchRequestContext

Controllers
  -> @RequiresPermission / @PreAuthorize
  -> CommandBus / QueryBus
  -> handlers métier
```

Important : les checks suivants doivent disparaître avant chargement DB :

```java
ctx.isSuperAdmin()
ctx.hasPermissionClaim(...)
```

Ils peuvent exister seulement après résolution de `UserAccessSnapshot`.

---

## 8. UserAccessSnapshot — modèle central cible

Créer dans `platform.accesscontrol.api` :

```java
public record UserAccessSnapshot(
    UserId appUserId,
    String email,
    String displayName,
    Set<String> globalRoles,
    Set<String> globalPermissions,
    List<TenantAccessView> memberships,
    boolean platformAccess,
    Instant resolvedAt
) {}
```

```java
public record TenantAccessView(
    TenantId tenantId,
    String tenantCode,
    Set<String> roles,
    Set<String> permissions,
    boolean active,
    boolean defaultTenant
) {}
```

Règle :

```text
Toutes les décisions d'autorisation web, tenant override, platform access et UI bootstrap doivent partir de ce snapshot.
```

Cache recommandé V0 :

```text
key = provider|issuer|subject ou appUserId
TTL = 60 secondes
invalidations = user disabled, role changed, membership changed, permission changed, external identity linked/unlinked
```

Attention : le cache est une optimisation, pas une source de vérité.

---

## 9. Tenant resolution cible

### 9.1 PUBLIC

```text
/public/**
anonymous -> public default tenant, ex: tchalanet
auth optional -> peut enrichir actor mais ne doit pas être requis
```

### 9.2 TENANT / ADMIN

```text
/tenant/**
/admin/**
auth required
appUser required
tenant required
source tenant:
  - selected/default membership
  - admin selection
  - override header seulement si permission DB
```

### 9.3 PLATFORM

```text
/platform/**
auth required
no tenant by default
permission PLATFORM_ACCESS required
tenant override possible seulement si permission DB dédiée
```

`TchPermissionEvaluator` doit donc accepter `tenantId = null` pour certaines permissions platform.

---

## 10. DB cible

### 10.1 Tables identity

Tables existantes à garder :

```text
app_user
app_user_external_identity
tenant_user
user_preference
```

`app_user_external_identity` est central :

```text
provider
issuer
external_subject
app_user_id
email_snapshot
UNIQUE(provider, issuer, external_subject)
```

C’est la base du provider-neutral.

---

### 10.2 Tables accesscontrol

Tables existantes :

```text
permission
app_role
app_role_permission
tenant_user_role
user_permission_override
```

### 10.3 Rôles platform

Problème : `tenant_user_role.tenant_id` semble orienté tenant. Pour les rôles platform globaux, recommandation : créer une table dédiée.

```sql
CREATE TABLE platform_user_role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id),
  role_id uuid NOT NULL REFERENCES app_role(id),
  assigned_at timestamptz NOT NULL DEFAULT now(),
  assigned_by uuid NULL,
  deleted_at timestamptz NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_platform_user_role UNIQUE (user_id, role_id)
);
```

Alternative possible mais moins propre : autoriser `tenant_user_role.tenant_id NULL` pour les rôles platform. La table dédiée est plus lisible.

### 10.4 Unicité des rôles

Recommandation :

```sql
CREATE UNIQUE INDEX uq_app_role_global_code
ON app_role(code)
WHERE tenant_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_app_role_tenant_code
ON app_role(tenant_id, code)
WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;
```

### 10.5 Hiérarchie de rôles

Si la hiérarchie n’est pas complète partout, ne pas l’activer en V0.

Décision recommandée :

```text
V0 = rôles plats + role_permission matrix.
Pas de parent_role_id tant que le besoin n’est pas prouvé.
```

---

## 11. RLS et data sources

Règle cible :

```text
rawDataSource découvre le contexte.
dataSource RLS exécute le métier.
```

Usage autorisé de `rawDataSource` :

```text
ExternalIdentityBootstrapReader
UserAccessSnapshotReader
TenantRegistryReader
PermissionCatalogReader
SystemRoleReader
Spring Batch metadata
```

Usage interdit :

```text
core.sales
core.payout
features.cashier
reports tenant-scoped
ticket/session métier
```

RLS protège les données métier après résolution du contexte. Il ne sert pas à découvrir le tenant initial.

---

## 12. Batch et schedulers

Les batchs ne doivent jamais dépendre de Firebase, Keycloak ou Clerk.

### 12.1 Batch platform/global

```text
pas de tenant
acteur SYSTEM
lit seulement platform/registry
liste tenants actifs
lance des traitements tenant-scoped
```

### 12.2 Batch tenant-scoped

```text
bind tenant RLS
acteur SYSTEM
TchContext.set(ctx)
services métier normaux
repositories normaux
RlsAwareDataSource applique set_config
```

### 12.3 Exemple cible

```text
DrawProcessingPlatformJob
  -> list active tenants
  -> for tenant in tenants:
       BatchTchContextBinder.bindTenant(tenantId, "batch:draw-processing")
       commandBus.execute(new ProcessTenantDrawsCommand(...))
```

Schedulers restent fins : active flags, gates, contexte, commande/batch, logs.

---

## 13. Clients web et mobile

### 13.1 Web admin Angular

Flow cible :

```text
/login
  -> FirebaseAuthService.login()
  -> getIdToken()
  -> Authorization: Bearer <token>
  -> GET /tenant/me/bootstrap
  -> build UserSession from backend response
  -> router.navigateByUrl(defaultRoute)
```

Interdit :

```text
Firebase token -> roles frontend
```

Correct :

```text
Firebase token -> backend -> DB access snapshot -> frontend session
```

---

### 13.2 Mobile POS

Flow cible :

```text
provider login ou phone/PIN
Authorization: Bearer <token>
GET /tenant/me/bootstrap
GET /tenant/me/operational-context
sell/payout/offline sync avec device proof
```

Le POS doit toujours revalider :

```text
trusted operational context
terminal actif
outlet actif
session ouverte
seller assignment
permission métier
device binding
signature device pour opérations sensibles
```

---

## 14. Plan de migration pas à pas

### Slice 1 — Neutraliser Spring Security

Changer :

```java
.requestMatchers("/api/v1/platform/**", "/platform/**")
.hasAnyAuthority("SUPER_ADMIN", "ROLE_SUPER_ADMIN")
```

vers :

```java
.requestMatchers("/api/v1/platform/**", "/platform/**")
.authenticated()
```

Puis sécuriser les controllers via `@RequiresPermission`.

---

### Slice 2 — Débrancher `AuthContextExtractor` legacy

Remplacer le modèle :

```text
tenant_code depuis JWT
roles depuis realm_access/resource_access
keycloakUserId
```

par :

```text
provider
issuer
externalSubject
email
emailVerified
```

---

### Slice 3 — Créer `UserAccessSnapshot`

Créer dans `platform.accesscontrol.api` :

```text
UserAccessSnapshot
TenantAccessView
ResolveUserAccessSnapshotRequest
```

Puis faire évoluer `DatabaseActorAuthorizationContextResolver` vers `UserAccessSnapshotResolver`.

---

### Slice 4 — Refactor `TchContextFilter`

Ordre cible :

```text
base context
attach appUserId
load access snapshot
resolve tenant
validate override
bind RLS
attach operational hint
bind final context
```

---

### Slice 5 — Corriger `TchPermissionEvaluator`

Permettre :

```text
tenantId nullable pour permissions platform
tenantId obligatoire pour permissions tenant/admin
```

---

### Slice 6 — Sortir le bootstrap UI de `identity`

Déplacer les modèles UI :

```text
LandingResponse
EffectiveUiContextResponse
ProfileActionsResponse
TenantContextResponse
UserContextResponse
```

vers :

```text
features.tenantbootstrap.model
```

Limiter `CurrentUserProfileController` à :

```http
GET /tenant/me/profile
PATCH /tenant/me/profile
```

---

### Slice 7 — Ajouter les endpoints bootstrap

Créer :

```http
GET /public/bootstrap
GET /tenant/me/bootstrap
```

Dans :

```text
features.publicbootstrap
features.tenantbootstrap
```

---

### Slice 8 — DB cleanup

À faire :

```text
ajouter platform_user_role ou équivalent
corriger/retirer parent_role_id si hiérarchie non utilisée
ajouter unique indexes role global/tenant
ajouter indexes access snapshot
ajouter identities FIREBASE_EMULATOR au seed
```

---

### Slice 9 — Batch context hardening

Créer ou stabiliser :

```text
BatchTchContextBinder
SystemActorContextFactory
TenantBatchContextFactory
PlatformBatchContextFactory
```

Vérifier que les jobs tenant-scoped passent par RLS.

---

## 15. Critères d’acceptation

### Auth provider-neutral

- [ ] Aucun controller/handler/core ne lit `realm_access`, `resource_access`, `tenant_code` ou Firebase custom claims pour décider une permission.
- [ ] Les providers externes ne font que vérifier l’identité.
- [ ] Le changement Firebase -> LocalJwt ne change aucun endpoint métier.
- [ ] `app_user_external_identity(provider, issuer, external_subject)` est la source de mapping.

### Authorization

- [ ] `/platform/**`, `/admin/**`, `/tenant/**` exigent `authenticated()` au niveau HTTP.
- [ ] Les permissions métier sont évaluées par `TchPermissionEvaluator` + `UserAccessSnapshot` DB.
- [ ] `PLATFORM_ACCESS` fonctionne sans tenant courant.
- [ ] Un user disabled en DB est refusé même avec token provider valide.

### Context/RLS

- [ ] `TchContextFilter` charge access snapshot avant tenant override.
- [ ] RLS est bindé après résolution du contexte.
- [ ] `rawDataSource` n’est pas utilisé par les domaines métier.
- [ ] Les batchs tenant-scoped bindent un contexte RLS explicite.

### Web/mobile

- [ ] Angular appelle `/tenant/me/bootstrap` après login.
- [ ] Le frontend ne construit jamais les rôles depuis le token Firebase.
- [ ] Mobile POS appelle `/tenant/me/operational-context`.
- [ ] Sell/payout/offline sync revalident terminal/outlet/session/device.

### Architecture

- [ ] `identity` ne retourne plus de bootstrap UI complet.
- [ ] `accesscontrol` porte le snapshot d’accès.
- [ ] `context` reste technique.
- [ ] `features.*bootstrap` composent les payloads clients.

---

## 16. Résumé exécutif

Architecture cible :

```text
app.config.security
  Authentification externe stateless
  Pas d'autorisation métier depuis JWT

platform.identity
  AppUser + external identity + provider adapters + profile + provisioning

platform.accesscontrol
  Roles + permissions + UserAccessSnapshot + permission evaluator

common.context
  TchRequestContext + binding + RLS bridge + operational context primitives

features.publicbootstrap
  /public/bootstrap

features.tenantbootstrap
  /tenant/me/bootstrap
  /tenant/me/operational-context

core.terminal/outlet/session
  validation opérationnelle POS

core.sales/payout/offlinesync
  re-checks sensibles et invariants métier
```

Décision finale :

```text
Un token externe ne peut jamais donner directement accès à une capacité métier.
Il peut seulement permettre au backend de retrouver un AppUser Tchalanet.
```

