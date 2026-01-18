# Domaine core.accesscontrol — Permissions & Rôles

> Décide des autorisations d’accès pour un utilisateur donné dans un contexte tenant/role. Domaine critique de sécurité.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/accesscontrol.md`

---

## 1) Scope & responsabilité (fusion)

### Responsabilité principale

Déterminer ce qu’un utilisateur a le droit de faire dans un tenant donné, via :

- appartenance (tenant_user)
- rôle principal (app_role)
- hiérarchie des rôles
- permissions associées (permission, role_permission)

### Ce que le domaine fait

- Relation user ↔ tenant ↔ rôle (membership).
- Rôles applicatifs (globaux ou spécifiques tenant).
- Catalogue de permissions (capacités métier atomiques).
- Association rôle → permissions.
- Résolution des permissions effectives.
- API applicative simple : “ce user peut-il faire X ?”.

**Ne fait pas**

- Authentification (Keycloak).
- Contexte HTTP (créé en common).
- Profil utilisateur (core.user).

---

## 2) Modèle & invariants

- `UserPermissions`: tenantId, userId, roles, permissions.
- Concepts clés:
  - Permission (ex: `ticket.sell`, `payout.execute`)
  - Role (ensemble de permissions, héritage possible)
  - TenantUser (membership: rôle principal, autonomie, statut)
- Enum:

```java
AutonomyLevel = NONE | PARTIAL | FULL
```

- Invariants:
  - Permissions normalisées (`snake.case`).
  - Évaluation déterministe; pas d’effets côté contrôleur.

---

## 3) Use Cases

- `CheckUserPermissionsHandler(tenantId, userId, perms)` → boolean/decision.
- `ListUserPermissionsHandler(tenantId, userId)` → set.

---

## 4) Ports (out)

- `UserRolesRepoPort`
- `UserPermissionsRepoPort`

---

## 5) Adaptateurs & Web

- `TchPermissionEvaluator` (adapter Spring Security): utilise `CheckUserPermissionsHandler`.
- Meta-annotations `@RequiredPermission("perm.key")` possibles.

---

## 6) Notes techniques

- Multi-tenant strict; wrappers d’ID.
- Pas de logique d’évaluation dans controller.

---

## 7) Incohérences / TODO

- Confirmer modèle de permission (static vs DB-driven).
- Ajouter caching pour évaluations fréquentes.
