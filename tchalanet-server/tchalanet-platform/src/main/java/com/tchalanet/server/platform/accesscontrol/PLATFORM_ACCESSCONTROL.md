# Platform Capability `platform.accesscontrol` — Permissions & Access Control

> Archetype : Application Service Module. Migré depuis `core.accesscontrol`.

## 1. Rôle

Évaluer les permissions applicatives, gérer l'assignation des rôles par tenant, et exposer les politiques d'accès.

**Ce module fait** :
- Vérifier qu'un actor a une permission donnée dans le contexte du tenant courant (`assertPermission`, `hasPermission`).
- Gérer les rôles et leurs assignations (CRUD admin).
- Exposer la liste des permissions disponibles.

**Ce module ne fait pas** :
- Authentification JWT / Keycloak (→ `common.security`).
- Règles métier sur ce qu'on peut faire avec les tickets (→ `core.sales`, `core.limitpolicy`).

## 2. Structure

```text
platform/accesscontrol/
  api/
    AccessControlApi.java     ← assertPermission / hasPermission
    model/
      Permission.java         ← enum ou record
      RoleView.java
  internal/
    service/
    persistence/              ← RoleJpaEntity, PermissionAssignmentRepository
    web/                      ← RoleAdminController (/api/v1/platform/roles)
    config/
```

## 3. Consommation

```java
// Dans un CommandHandler core
private final AccessControlApi acl;

acl.assertPermission(cmd.actorId(), Permission.APPROVE_TICKET);
```

## 4. Règles

- RLS actif sur les tables de rôles/permissions.
- Résultat incorrect ici peut créer des accès non autorisés — vigilance.
- `core` ne doit pas écouter les events de ce module.
