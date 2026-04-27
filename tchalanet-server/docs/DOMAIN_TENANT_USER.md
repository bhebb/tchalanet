# Domaine Tenant User — core.tenantuser

Status: NORMATIVE (v1)

## Objectif

Isoler et gérer l'appartenance (membership) d'un `AppUser` à un Tenant donné sans polluer les domaines `core.accesscontrol` (RBAC) ni `core.user` (identité globale).

Ce document définit le périmètre, le modèle de domaine, les commands/queries, les ports hexagonaux, la persistence et les règles de sécurité pour le module `core.tenantuser`.

1. Scope

---

Responsabilité principale

- Gérer la relation `tenant_user` (membership) : joindre / quitter un tenant.
- Assigner un rôle principal au sein du tenant (via `role_id` référencé en lecture depuis RBAC).
- Gérer le niveau d'autonomie (`autonomy_level`).
- Gérer le statut de membership tenant‑scopé (optionnel : `status`).
- Marquer l'ownership (is_owner).

Ce que le domaine fait

- Créer / mettre à jour / supprimer (soft) une membership.
- Lister et rechercher les users d'un tenant (JOIN `tenant_user` + `app_user`).
- Fournir des read models tenant‑scopés pour UI admin.

Ce que le domaine ne fait pas

- Authentification (Keycloak).
- Profil utilisateur global (core.user).
- Définition des rôles/permissions (core.accesscontrol).
- Évaluation fine des permissions (accesscontrol/evaluator).

2. Tables & ownership

---

Table appartenant à `core.tenantuser` : `tenant_user`.

Colonnes (v1)

- tenant_id UUID NOT NULL (FK tenant)
- user_id UUID NOT NULL (FK app_user)
- role_id UUID NOT NULL (FK app_role) — référence RBAC en lecture uniquement
- autonomy_level VARCHAR(16) NOT NULL — (`none`|`partial`|`full`)
- status VARCHAR(32) [OPTIONNEL] — ex: `INVITED|PENDING_APPROVAL|ACTIVE|SUSPENDED`
- is_owner BOOLEAN NOT NULL DEFAULT false
- deleted_at TIMESTAMP NULL (soft delete)
- created_at, updated_at, version (audit/optimistic lock)

Contraintes / Indexes

- UNIQUE (tenant_id, user_id) WHERE deleted_at IS NULL
- INDEX ix_tenant_user_tenant (tenant_id)
- INDEX ix_tenant_user_user (user_id)

Note : `tenant_user` est considéré comme core (security‑critical).

3. Modèle de domaine (Aggregate)

---

Option A (recommandée v1)

- Aggregate root : `TenantUserMembership`
  - Identifiant : composite (tenantId + userId) ou `TenantUserId` (si tu veux PK surrogate)
  - Champs : tenantId, userId, roleId, autonomyLevel, isOwner, status, createdAt, updatedAt
  - Méthodes / Commandes métier : `assignRole(roleId)`, `changeAutonomy(level)`, `suspend(reason)`, `reactivate()`, `remove()` (soft-delete)

Option B (CQRS simple)

- Pas d'aggregate riche : handler + ports + validations transactionnelles. OK si invariants simples.

Décision v1 : implémenter Option A minimal (aggregate thin) ou B si tu veux commencer rapide — le document utilise la terminologie `TenantUserMembership`.

4. Commands (write)

---

Package : `core.tenantuser.application.command.model` et `.handler`

Principes

- 1 intention = 1 command (pas de command générique `UpdateTenantUser`)
- Les Commands sont `record` et utilisent les typed IDs (see NAMING.md)

Liste v1

- `AssignUserToTenantCommand(TenantId tenantId, UserId userId, RoleId roleId, AutonomyLevel autonomyLevel, boolean isOwner)` → `AssignUserToTenantResult(tenantUserId)` ou Void.
- `ChangeTenantUserRoleCommand(TenantId tenantId, UserId userId, RoleId roleId)` → Void / Result.
- `ChangeTenantUserAutonomyCommand(TenantId tenantId, UserId userId, AutonomyLevel autonomyLevel)` → Void.
- `SuspendTenantUserCommand(TenantId tenantId, UserId userId, String reason)` → Void.
- `ReactivateTenantUserCommand(TenantId tenantId, UserId userId)` → Void.
- `RemoveUserFromTenantCommand(TenantId tenantId, UserId userId)` (soft delete) → Void.

Résultats

- `AssignUserToTenantResult(TenantUserId membershipId)` ou `Pair<TenantId,UserId>` si tu préfères.

5. Queries (read)

---

Package : `core.tenantuser.application.query.model` et `.handler`

Read models (application)

- `TenantUserRow` (liste / paged) :
  - userId, username, displayName, email, membershipStatus, autonomyLevel, roleId, roleCode (optionnel), createdAt
- `TenantUserDetails` : détails full pour UI tenant admin

Queries v1

- `GetTenantUserDetailsQuery(TenantId tenantId, UserId userId) → TenantUserDetails`
- `PagedListTenantUsersQuery(TenantId tenantId, TchPageRequest pageReq) → TchPage<TenantUserRow>`
- `SearchTenantUsersQuery(TenantId tenantId, criteria..., TchPageRequest) → TchPage<TenantUserRow>`

6. Ports (hexagonal)

---

Package : `core.tenantuser.application.port.out`

Reader port

- `TenantUserReaderPort` :
  - `Optional<TenantUserMembership> findByTenantAndUser(TenantId tenantId, UserId userId)`
  - `Page<TenantUserRow> pagedListByTenant(TenantId tenantId, Pageable page)`
  - `Page<TenantUserRow> searchByTenantCriteria(TenantId tenantId, SearchCriteria criteria, Pageable page)`

Writer port

- `TenantUserWriterPort` :
  - `TenantUserMembership upsertMembership(TenantUserMembership m)`
  - `void softDeleteMembership(TenantId tenantId, UserId userId, Instant when)`

Ports externes (lecture only)

- `RoleReaderPort` (core.accesscontrol) : `boolean exists(RoleId roleId)` ou `Optional<RoleRow> findById(RoleId)`

7. Infra (persistence + web)

---

Persistence

- Package infra : `core.tenantuser.infra.persistence`
- JPA entity : `TenantUserJpaEntity` (suffixe `JpaEntity` selon NAMING.md)
- Spring Data repository : `TenantUserJpaRepository`
- Persistence adapter : `TenantUserPersistenceAdapter implements TenantUserReaderPort, TenantUserWriterPort`

Remarque implémentation

- Les listes/searchs DOIVENT faire un JOIN `tenant_user` + `app_user` pour enrichir la ligne (username, email...).
- Ne pas implémenter la recherche tenant‑scoped dans `core.user` adapter : ownership here.

Web

- Controllers :
  - `core.tenantuser.infra.web.admin.TenantUserAdminController` — routes `/admin/tenant-users` (tenant-scoped, tenantId depuis ctx)
  - `core.tenantuser.infra.web.platform.PlatformTenantUserController` — routes `/platform/tenant-users` (superadmin, peut passer tenantId)
- Responses : `TenantUserItemResponse`, `TenantUserDetailsResponse`.
- Endpoint responses: `ApiResponse<TchPage<TenantUserItemResponse>>` pour les listes.

8. Sécurité (admin vs superadmin)

---

- TENANT_ADMIN : accès en lecture/écriture limité au `tenantId` effectif du contexte. Controller `/admin` doit utiliser ctx. Aucune possibilité de passer un tenantId arbitraire.
- SUPER_ADMIN : peut opérer via `/platform` et choisir `tenantId` (query param ou header selon politique).
- Règle : séparation de routes (`/admin/*` vs `/platform/*`) pour éviter `if` partout.
- RLS / DB policies : si tu utilises RLS, `TenantUserPersistenceAdapter` doit respecter la visibility; bootstrap/admin adapters peuvent bypasser RLS si explicitement nécessaire.

9. Indexes & Migrations

---

- SQL create table v1 :

  - créer `tenant_user` avec colonnes ci‑dessus
  - ajouter constraint unique (tenant_id, user_id) WHERE deleted_at IS NULL
  - créer indexes listés

- Migration notes :
  - `tenant_user` appartient à core — inclure la migration dans module `tchalanet-server/tchalanet-infra/db/migration` (Flyway)
  - Règle : ne pas supprimer les FK vers `app_user` ni `app_role` sans coordination cross‑team.

10. Observabilité & audit

---

- MDC / logs : `tenantId`, `tenantCode` (si disponible), `appUserId` (si disponible), `requestId`, `actorUserId` (admin who performed the change)
- Auditing : JPA Envers ou audit columns (`created_by`, `created_at`, `updated_by`, `updated_at`) sur `tenant_user`.

11. Tests recommandés

---

- Unit :
  - `AssignUserToTenantCommandHandlerTest` (happy path + already exists + uniqueness violation)
  - `ChangeTenantUserRoleCommandHandlerTest`
- Integration :
  - `TenantUserPersistenceAdapterIT` (upsert → read join app_user)
  - `TenantUserAdminControllerIT` (security contexts: TENANT_ADMIN vs SUPER_ADMIN)
- Query tests : paged list + search criteria sanity.

12. Exemple rapide Java (naming)

---

- Command: `core.tenantuser.application.command.model.AssignUserToTenantCommand` (record)
- Handler: `core.tenantuser.application.command.handler.AssignUserToTenantCommandHandler`
- Entity: `core.tenantuser.infra.persistence.TenantUserJpaEntity`
- Adapter: `core.tenantuser.infra.persistence.TenantUserPersistenceAdapter`
- Controller admin: `core.tenantuser.infra.web.admin.TenantUserAdminController`
- Read model: `core.tenantuser.application.query.model.TenantUserRow` / `TenantUserDetails`

13. Anti-drift / Naming checklist

---

Conserver les conventions de `docs/NAMING.md` :

- Commands/Queries = `record`
- Read model = `TenantUserRow | TenantUserDetails` (pas `Dto`)
- Web DTOs = `TenantUserItemResponse | TenantUserDetailsResponse`
- Entity = `TenantUserJpaEntity`
- Adapter = `TenantUserPersistenceAdapter`

14. Next steps (pratique)

---

- Créer `TenantUserJpaEntity` si requis (utiliser `TenantUserEntity` attachée comme référence et renommer en `TenantUserJpaEntity`).
- Implémenter `TenantUserPersistenceAdapter` et `TenantUserJpaRepository`.
- Écrire handlers/commands minimum (Assign, Remove, ChangeRole).
- Ajouter controllers `/admin/tenant-users` and `/platform/tenant-users` (secure with roles).

## Annexes

- Référence d'entité fournie par exemple : `TenantUserEntity.java` (à renommer et adapter si nécessaire)
- NAMING guidelines : `docs/NAMING.md`

---

Si tu veux, je peux :

- A) générer `TenantUserJpaEntity` (rename and align with the provided `TenantUserEntity`),
- B) créer le scaffold `TenantUserPersistenceAdapter` et `TenantUserJpaRepository`,
- C) ajouter les `AssignUserToTenantCommand` + handler + tests minimal,
- D) créer les controllers admin/platform + DTOs.

Dis quelle option exécuter et j'applique les fichiers correspondants (je peux tout implémenter étape par étape).
