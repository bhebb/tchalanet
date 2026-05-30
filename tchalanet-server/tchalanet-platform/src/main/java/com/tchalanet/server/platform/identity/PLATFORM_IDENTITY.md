
# Platform Capability `platform.identity` — User & Tenant Identity

## Rôle

Gérer les profils utilisateurs dans le contexte tenant, le mapping Keycloak ↔ utilisateur applicatif, et exposer le contexte utilisateur aux autres modules.

**Ce module fait** :
- Résolution et bootstrap d'un `UserId` depuis JWT (`bootstrapCurrentUser`)
- Lecture profil utilisateur courant et lookup par UUID
- Exposition des infos utilisateur (`CurrentUserView`, `AppUserView`, `UserProfileView`)
- Résolution de la surface client (`ClientSurface`) depuis les rôles

**Ce module ne fait pas** :
- Authentification (→ Keycloak, `common.security`)
- Gestion des rôles/permissions (→ `platform.accesscontrol`)
- Configuration tenant (→ `platform.tenantconfig`)

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

## Intégration

- Consommé par `TchContextFilter` pour bootstrapper le contexte de requête
- `platform.accesscontrol` utilise `IdentityApi` pour la résolution d'actor
- RLS actif sur toutes les tables utilisateur

## Règles

- `core` ne doit pas écouter les events de ce module directement
- `bootstrapCurrentUser` est idempotent — crée l'utilisateur si absent
