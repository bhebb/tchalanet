
# Platform Capability `platform.identity` — User & Tenant Identity

## Rôle

Gérer les profils utilisateurs dans le contexte tenant, le mapping Keycloak ↔ utilisateur applicatif, et exposer le contexte utilisateur aux autres modules.

**Ce module fait** :
- Résolution et bootstrap d'un `UserId` depuis JWT (`bootstrapCurrentUser`)
- Lecture profil utilisateur courant et lookup par UUID
- Exposition des infos utilisateur (`CurrentUserView`, `AppUserView`, `UserProfileView`)
- Résolution de la surface client (`ClientSurface`) depuis les rôles
- **Appartenance tenant** (`tenant_user` / membership) — membership-only
- **Orchestration du provisioning** d'un utilisateur tenant (app_user + membership + sync KC),
  en déléguant l'affectation de rôle à `platform.accesscontrol`

**Ce module ne fait pas** :
- Authentification (→ Keycloak, `common.security`)
- Gestion des rôles/permissions, affectation effective, overrides (→ `platform.accesscontrol`)
- Configuration tenant (→ `platform.tenantconfig`)
- Contexte opérationnel (terminal/outlet/session) — reste hors identity

---

## Enums

### `UserStatus` : `PENDING_APPROVAL` · `ACTIVE` · `SUSPENDED` · `INVITED`
### `TenantUserStatus` : `INVITED` · `PENDING_APPROVAL` · `ACTIVE` · `SUSPENDED`
### `AutonomyLevel` : `NONE` · `PARTIAL` · `FULL`

### `ClientSurface`

| Valeur | Sens |
|---|---|
| `MOBILE_POS` | Application mobile POS (vendeur terrain) |
| `CASHIER_WEB` | Interface cashier web |
| `TENANT_ADMIN_WEB` | Interface admin tenant |
| `PLATFORM_ADMIN_WEB` | Interface super-admin |

`ClientSurfacePolicy.preferredSurface(roles)` détermine la surface principale selon les rôles.

---

## API — `IdentityApi`

```java
CurrentUserView       getCurrentUser(GetCurrentUserRequest)
BootstrapUserResult   bootstrapCurrentUser(BootstrapCurrentUserRequest)
  // isNew=true si premier bootstrap — idempotent
UserProfileView       getUserProfile(GetUserProfileRequest)
Optional<AppUserView> findAppUser(UUID keycloakSub)
long                  countTenantUsers()
```

**`CurrentUserView`** : id, keycloakSub, username, email, firstName, lastName, displayName, tenantId, tenantCode, tenantTimeZone, tenantCurrency, themeMode, density, locale, timeZone, currency

**`AppUserView`** : id, keycloakSub, username, email, phone, firstName, lastName, displayName, status (`UserStatus`)

---

## Provisioning utilisateur & sync Keycloak

`POST /admin/identity/users` (TENANT_ADMIN / SUPER_ADMIN) crée l'utilisateur applicatif
**et** son compte Keycloak (`KeycloakUserProvisionService`), pour qu'il puisse s'authentifier
immédiatement (mot de passe par défaut `Changeme1!`, email vérifié, required-actions vidées).

Deux invariants critiques pour qu'un utilisateur créé via l'API soit **opérationnel** :

1. **Rôle applicatif → realm role Keycloak.** Les autorités du JWT sont dérivées des realm
   roles Keycloak (`SecurityConfig`), *pas* du `tenant_user_role` applicatif. Après
   l'affectation du rôle applicatif (`platform.accesscontrol`), le rôle est donc miroité dans
   Keycloak (`KeycloakUserProvisionService.assignRealmRole` via
   `TenantUserAdministrationService.syncKeycloakRealmRole`, best-effort, ne jette jamais).
   Sans ça, un cashier/admin créé par l'API porte un JWT sans autorité → 403 sur ses endpoints.

2. **Propagation `tenant_code`.** Le nouvel utilisateur reçoit l'attribut Keycloak
   `tenant_code` = code du tenant courant, résolu par `resolveTenantCodeOrNull` :
   code du contexte (`effectiveTenantCode` / `originalTenantCode`) pour une requête normale ;
   **lookup DB par UUID** quand un SUPER_ADMIN agit via override `X-Tenant-Id` (le contexte ne
   porte alors qu'un UUID dans les champs « code »). Le mapper KC `TchJsonClaimProtocolMapper`
   émet le claim ; il retombe sur `"default"` si l'attribut est absent.
   **Prérequis realm** : `unmanagedAttributePolicy=ENABLED` (cf. `tchalanet-infra/.../realm.base.json`),
   sinon Keycloak supprime à la création les attributs non déclarés (`tenant_code`, `plan`,
   `featureSetId`) — seul `locale` survit et l'utilisateur tombe dans le tenant `"default"`.

## Structure interne (convention « API adapter »)

`internal/model/` (records domaine) · `internal/service/` (services fins) ·
`internal/service/keycloak/` (provisioning + sync KC) ·
`internal/adapter/IdentityApiAdapter` (impl de l'API publique, délègue uniquement) ·
`internal/persistence` · `internal/web/{me,admin,ops}`.

**Séparation des responsabilités** (réorg `identity-user-provisioning-reorg`) :

- `TenantMembershipService` est **membership-only** : il ne fait pas `setRole`, n'affecte ni
  ne calcule de permission, et n'injecte pas `AccessControlApi`.
- `TenantUserProvisioningService` **orchestre** la création d'un utilisateur tenant : app_user +
  membership (identity), puis `AccessControlApi.assignRoleToUser(...)` (accesscontrol), puis sync KC.
  Aucune permission n'est copiée dans la ligne utilisateur — elles dérivent du rôle + overrides.
- `TenantUserAdministrationService` compose la vue admin (profil + membership + rôles/permissions
  effectives via accesscontrol + statut invitation/sync) — sorti du controller, qui reste mince.

## Intégration

- Consommé par `TchContextFilter` pour bootstrapper le contexte de requête
- `platform.accesscontrol` utilise `IdentityApi` pour la résolution d'actor
- RLS actif sur toutes les tables utilisateur

## Règles

- `core` ne doit pas écouter les events de ce module directement
- `bootstrapCurrentUser` est idempotent — crée l'utilisateur si absent
