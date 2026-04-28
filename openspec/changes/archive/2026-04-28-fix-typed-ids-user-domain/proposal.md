## Why

`ApproveUserCommand.approvedBy` et `AppUser.approvedBy` utilisent `UUID` brut au lieu du type `UserId` — violation des conventions typed IDs du projet identifiée lors de l'audit du 2026-04-27. De même, les views `TenantContext.tenantId` et `UserDetails.tenantId` retournent `UUID` brut dans les réponses de queries au lieu de `TenantId`.

## What Changes

- `ApproveUserCommand.approvedBy` : `UUID` → `UserId`
- `AppUser.approvedBy` (entité domaine) : `UUID` → `UserId`
- `AppUserJpaEntity.approvedBy` (JPA) : rester en `UUID` brut + conversion dans le mapper — acceptable en infra
- `TenantContext.tenantId` : `UUID` → `TenantId`
- `UserDetails.tenantId` : `UUID` → `TenantId`
- Adaptation de tous les call sites, mappers web et tests impactés.

## Capabilities

### New Capabilities

<!-- aucune -->

### Modified Capabilities

<!-- aucun changement de contrat HTTP externe — changements internes uniquement -->

## Impact

- `core.user/application/command/model/ApproveUserCommand` — champ `approvedBy`
- `core.user/domain/model/AppUser` — champ `approvedBy`
- `core.user/application/query/model/TenantContext`, `UserDetails` — champ `tenantId`
- `core.user/infra/persistence/*/UserMapper` — conversions à mettre à jour
- `core.user/infra/web/model/*` — request/response si les UUIDs sont exposés en JSON (à vérifier)
- **Aucun breaking change HTTP** si la sérialisation JSON des typed IDs reste identique à celle des UUID bruts.
