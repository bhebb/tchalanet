## Context

Le projet enforce les typed IDs (Value Object autour de `UUID`) pour tous les identifiants dans le domaine. La couche infra (JPA) utilise `UUID` brut, convertie dans les mappers.

Anomalies dans `core.user` identifiées à l'audit :

| Classe                       | Champ        | Type actuel | Type attendu |
| ---------------------------- | ------------ | ----------- | ------------ |
| `ApproveUserCommand`         | `approvedBy` | `UUID`      | `UserId`     |
| `AppUser`                    | `approvedBy` | `UUID`      | `UserId`     |
| `TenantContext` (query view) | `tenantId`   | `UUID`      | `TenantId`   |
| `UserDetails` (query view)   | `tenantId`   | `UUID`      | `TenantId`   |

`AppUserJpaEntity.approvedBy` reste en `UUID` brut (acceptable en couche infra, conversion dans le mapper).

## Goals / Non-Goals

**Goals:**

- Les 4 champs listés utilisent les typed IDs corrects
- Les mappers assurent la conversion `UUID <-> TypedId` aux frontières infra
- La sérialisation JSON reste compatible (aucun breaking change HTTP)
- Tests mis à jour

**Non-Goals:**

- Correction de `GetUserDetailsQueryHandler.toDetails()` qui retourne `null` pour plusieurs champs (anomalie BASSE — change séparé)
- Correction des anomalies CRITIQUE d'isolation tenant (traité dans `fix-user-tenant-isolation`)

## Decisions

### D1 — approvedBy : UUID -> UserId dans le domaine

`AppUser.approvedBy` est un champ domaine : doit être `UserId`. L'entité JPA reste en `UUID` brut. Le mapper fait la conversion. Pattern identique aux autres domaines du projet.

### D2 — TenantId dans les query views

`TenantContext` et `UserDetails` sont sérialisés en JSON par les controllers. Vérifier que le `TenantId` est sérialisé via `value().toString()` (et non `{value: "..."}`) avant migration pour éviter tout breaking change HTTP.

### D3 — approvedBy nullable

`approvedBy` peut être null (non encore approuvé). Le type devient `@Nullable UserId approvedBy`.

## Risks / Trade-offs

- **[Risque] Sérialisation JSON TenantId** : si le serializer produit un objet plutôt qu'une string UUID, c'est un breaking change HTTP. → Vérifier la config Jackson des typed IDs avant de migrer les views.
- **[Risque] Call sites ApproveUserCommand** : probablement 1-2 mappers web à adapter.

## Migration Plan

1. Vérifier la sérialisation JSON des typed IDs (Jackson config)
2. Migrer `AppUser.approvedBy` : `UUID` → `@Nullable UserId`
3. Mettre à jour `UserMapper` : conversion `UUID <-> UserId` pour `approvedBy`
4. Migrer `ApproveUserCommand.approvedBy` : `UUID` → `UserId`
5. Migrer `TenantContext.tenantId` et `UserDetails.tenantId` : `UUID` → `TenantId`
6. Adapter les call sites web (web mappers, si nécessaire)
7. Tests mis à jour
8. `./mvnw clean verify`
