# Domaine Access Control — `core.accesscontrol`

> Type: Functional Domain Specification  
> Scope: `com.tchalanet.server.core.accesscontrol`  
> Criticalité: HIGH (Sécurité / Autorisation)  
> Related: `OpenSpec — Core Rules (80)`  
> Functional overview: `tchalanet-docs/docs/02-functional/domains/accesscontrol.md`

---

## 1. Scope & responsabilité

### 1.1 Responsabilité principale

Le domaine **Access Control** détermine les autorisations effectives d’un utilisateur dans un contexte tenant donné. Il est la source de vérité pour les décisions du type:

> « Cet utilisateur a-t‑il le droit d’effectuer cette action ? »

### 1.2 Ce que le domaine fait

- Gère l’appartenance `user ↔ tenant` (membership).
- Gère les rôles applicatifs (globaux ou tenant-scoped).
- Gère le catalogue de permissions (capabilités métier atomiques).
- Gère l’association `role → permissions`.
- Résout les permissions effectives d’un utilisateur.
- Expose une API applicative : `Check permission`, `List permissions`.

### 1.3 Ce que le domaine ne fait PAS

- Authentification / login (Keycloak).
- Résolution du contexte HTTP / JWT (fait en `common`).
- Gestion du profil utilisateur (→ `core.user`).
- Orchestration UI / BFF.
- Décisions métier hors autorisation.

---

## 2. Modèle de domaine & invariants

### 2.1 Concepts clés

- **Permission** — capacité métier atomique (ex: `ticket.sell`, `payout.execute`).
- **Role** — ensemble de permissions (possible héritage simple).
- **TenantUser** — relation `user ↔ tenant` (rôle principal, autonomie, statut).

### 2.2 Read model principal — `UserPermissions`

- `tenantId`
- `userId`
- `roles : Set<RoleKey>`
- `permissions : Set<PermissionKey>`
- `autonomyLevel`

Ce modèle est dérivé, jamais muté directement.

### 2.3 Enum — `AutonomyLevel` (V1)

```text
AutonomyLevel = NONE | PARTIAL | FULL

NONE    : toutes les actions nécessitent validation
PARTIAL : validations conditionnelles (seuils)
FULL    : autonomie complète dans les limites tenant
```

Les règles d'autonomie n'exécutent aucune action métier ici — elles informent seulement la décision d'accès.

### 2.4 Invariants

- Les clés de permission sont normalisées (snake.case).
- L'évaluation des permissions est déterministe et sans effets de bord.
- Aucune logique d'autorisation dans les controllers (seulement dans les handlers/domain).

---

## 3. Use cases (Application Layer)

**Emplacement** :

- `core.accesscontrol.application.query.model`
- `core.accesscontrol.application.query.handler`

### 3.1 Use cases V1

- `CheckUserPermissionsQuery`  
  Input: `(tenantId, userId, requiredPermissions[])`  
  Output: `PermissionDecision`

- `ListUserPermissionsQuery`  
  Input: `(tenantId, userId)`  
  Output: `Set<PermissionKey>`

### 3.2 Décision (PermissionDecision)

- `allowed : boolean`
- `missingPermissions : Set<PermissionKey>`
- `reason` (optionnel, pour debug / audit)

> Ne pas exposer de boolean brut dans le domaine public ; utiliser des types explicites.

---

## 4. Ports (Hexagonal)

**Emplacement** : `core.accesscontrol.application.port.out`

Ports requis (read-only) :

- `UserRoleReaderPort` — rôles associés à un utilisateur dans un tenant
- `RolePermissionReaderPort` — permissions associées à un rôle (hiérarchie possible)
- `TenantUserReaderPort` — niveau d’autonomie et statut d’accès

Les écritures (admin) appartiennent au catalogue ou à un module d'admin dédié.

---

## 5. Infrastructure & Adapters

### 5.1 Spring Security Adapter — `TchPermissionEvaluator`

- Traduit annotations (`@RequiredPermission`) en appels applicatifs vers `CheckUserPermissionsHandler`.
- L'adapter ne contient aucune logique d'autorisation métier.

### 5.2 Annotations utiles

- `@RequiredPermission("ticket.sell")`
- `@RequiredPermission("payout.execute")`

Ces annotations sont résolues via l'évaluator.

---

## 6. Notes techniques

- Domaine strictement multi-tenant ; utiliser des wrappers d'ID partout.
- RLS appliqué au niveau DB pour les données tenant-scoped.
- Aucun accès direct aux repositories depuis la couche web.

---

## 7. Performance & cache

V1 : pas de cache obligatoire ; l'évaluation doit être correcte même sans cache.

V2+ : cache local ou Redis possible pour :

- `ListUserPermissions`
- `CheckUserPermissions`

Cache strictement invalidé sur :

- changement de rôle
- changement de permission
- changement d'autonomie

---

## 8. Failure modes

- Utilisateur sans membership tenant → décision = DENIED.
- Rôle sans permissions → décision = DENIED.
- Permission inconnue → rejet explicite + log sécurité.

---

## 9. Tests — Definition of Done

- Évaluation permissions simple (happy path)
- Évaluation avec rôles multiples
- Héritage de rôle (si activé)
- AutonomyLevel respecté
- Integration : Repositories + RLS
- Tests multi-tenant isolation
- Cas utilisateur sans accès couvert

---

## 10. Mini-checklist

- [ ] Aucune logique d'autorisation dans controllers
- [ ] Aucun accès direct aux repos depuis web
- [ ] Pas de boolean brut exposé (utiliser PermissionDecision)
- [ ] Wrappers d'ID partout
- [ ] Permissions normalisées
- [ ] Domaine indépendant de `core.user` et `catalog`

---

_Document rédigé selon les conventions Tchalanet — voir `docs/conventions` pour la structure attendue._
