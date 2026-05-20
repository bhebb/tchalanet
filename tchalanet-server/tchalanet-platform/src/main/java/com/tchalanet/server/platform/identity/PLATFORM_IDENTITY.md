# Platform Capability `platform.identity` — User & Tenant Identity

> Archetype : Application Service Module. Migré depuis `core.tenantuser` → `core.usercontext`.

## 1. Rôle

Gérer les profils utilisateurs dans le contexte tenant, le mapping Keycloak ↔ utilisateur applicatif, et exposer le contexte utilisateur aux autres modules.

**Ce module fait** :
- Résoudre un `UserId` depuis un token JWT / `TchRequestContext`.
- Gérer le cycle de vie des utilisateurs tenant (création, désactivation, profil).
- Exposer les informations utilisateur en lecture (`UserView`, `UserSummaryView`).

**Ce module ne fait pas** :
- Authentification (→ Keycloak, `common.security`).
- Gestion des rôles et permissions (→ `platform.accesscontrol`).
- Configuration tenant (→ `platform.tenantconfig`).

## 2. Structure

```text
platform/identity/
  api/
    IdentityApi.java          ← resolveUser(UserId), getCurrentUser()
    model/
      UserView.java
      UserSummaryView.java
  internal/
    service/
    persistence/              ← UserJpaEntity, UserRepository
    web/                      ← UserAdminController (/api/v1/admin/users)
    config/
```

## 3. Règles

- RLS actif (`tenant_id` sur toutes les tables utilisateur).
- `platform.accesscontrol` consomme `IdentityApi` pour résoudre l'actor.
- `core` ne doit pas écouter les events de ce module.
