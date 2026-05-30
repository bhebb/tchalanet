
# Platform Capability `platform.identity` — User & Tenant Identity

## Rôle

Gérer les profils utilisateurs dans le contexte tenant, le mapping Keycloak ↔ utilisateur applicatif, et exposer le contexte utilisateur aux autres modules.

**Ce module fait** :
- Résolution d’un `UserId` depuis JWT ou contexte
- Cycle de vie utilisateur tenant (création, désactivation, profil)
- Exposition des infos utilisateur (`UserView`, `UserSummaryView`)

**Ce module ne fait pas** :
- Authentification (Keycloak, common.security)
- Gestion des rôles/permissions (voir `platform.accesscontrol`)
- Configuration tenant (voir `platform.tenantconfig`)

## Surface API

- `IdentityApi` (Java) : `resolveUser`, `getCurrentUser`
- Modèles : `UserView`, `UserSummaryView`

## Intégration

- Consommé par `platform.accesscontrol` pour la résolution d’actor
- RLS actif (`tenant_id` sur toutes les tables utilisateur)

## Règles et limitations

- `core` ne doit pas écouter les events de ce module
