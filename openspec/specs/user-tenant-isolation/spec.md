# User Tenant Isolation — Specification

## Context

`app_user` est une table d'identité **globale** — elle n'a pas de colonne `tenant_id` et n'est pas soumise aux policies RLS de V40. L'appartenance d'un user à un tenant est modélisée par la table `tenant_user` (many-to-many).

## Requirements

### Requirement: findByTenantId filtre par appartenance tenant_user

`AppUserPersistenceAdapter.findByTenantId(TenantId, Pageable)` SHALL retourner uniquement les users qui ont une entrée active dans `tenant_user` pour le tenant demandé. Le filtre est appliqué via un JOIN SQL sur `tenant_user`, pas via RLS (non applicable sur `app_user`).

> ❌ Jamais `WHERE tenant_id = ?` directement sur `app_user` (la table n'a pas cette colonne).  
> ✅ Le filtre tenant passe par `JOIN tenant_user ON user_id WHERE tenant_id = :tenantId AND status = 'ACTIVE' AND deleted_at IS NULL`.

#### Scenario: Liste des users d'un tenant — isolation par membership

- **WHEN** `findByTenantId(tenantA, pageable)` est appelée
- **AND** la base contient des users pour tenant-A, tenant-B, et les deux
- **THEN** seuls les users avec une entrée active dans `tenant_user` pour tenant-A sont retournés
- **THEN** les users de tenant-B uniquement sont exclus

#### Scenario: Tenant sans membres — liste vide

- **WHEN** `findByTenantId(tenantX, pageable)` est appelée pour un tenant sans aucune entrée dans `tenant_user`
- **THEN** la liste retournée est vide

#### Scenario: Membership soft-deleted exclu

- **WHEN** une entrée `tenant_user` est soft-deleted (`deleted_at IS NOT NULL`)
- **THEN** le user correspondant est exclu du résultat de `findByTenantId`

#### Scenario: Membership SUSPENDED exclu

- **WHEN** une entrée `tenant_user` a le statut `SUSPENDED`
- **THEN** le user correspondant est exclu du résultat de `findByTenantId`

#### Scenario: User soft-deleted exclu

- **WHEN** un `app_user` est soft-deleted (`deleted_at IS NOT NULL`)
- **THEN** il est exclu du résultat même s'il a une entrée active dans `tenant_user`

### Requirement: API UserReaderPort sans paramètre tenant redondant

`UserReaderPort.findAllActiveUsersByTenant(TenantId, Pageable)` ne SHALL pas exister — elle dupliquait `findAllActiveUsers(Pageable)` en ignorant silencieusement son paramètre `TenantId`.

#### Scenario: Aucune méthode avec TenantId ignoré

- **WHEN** le code de `UserReaderPort` et `AppUserPersistenceAdapter` est inspecté
- **THEN** aucune méthode n'accepte un `TenantId` sans l'utiliser dans la requête SQL

### Requirement: Aucun filtre SQL applicatif WHERE tenant_id sur app_user

Aucune requête dans `AppUserPersistenceAdapter` ne SHALL ajouter `WHERE tenant_id = ?` directement sur `app_user`.

#### Scenario: Absence de filtre tenant applicatif

- **WHEN** le code de `AppUserPersistenceAdapter` est inspecté
- **THEN** aucune clause `WHERE tenant_id = ?` ou prédicat Criteria `tenantId = ...` n'est présente dans les méthodes de lecture de `app_user`
