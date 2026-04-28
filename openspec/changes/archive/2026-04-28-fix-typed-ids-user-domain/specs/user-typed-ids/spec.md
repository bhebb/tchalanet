## ADDED Requirements

### Requirement: approvedBy utilise UserId dans le domaine

`AppUser.approvedBy` et `ApproveUserCommand.approvedBy` SHALL utiliser le type `UserId` et non `UUID` brut. La couche JPA (`AppUserJpaEntity`) peut conserver `UUID` brut avec conversion dans le mapper.

#### Scenario: Approbation d'un utilisateur

- **WHEN** `ApproveUserCommand` est construit avec l'ID de l'approbateur
- **THEN** le champ `approvedBy` est de type `UserId` (valeur non-null)

#### Scenario: Lecture d'un utilisateur approuve

- **WHEN** un `AppUser` est chargé depuis la persistence
- **THEN** `appUser.approvedBy()` retourne un `UserId` ou null (jamais un `UUID` brut)

### Requirement: TenantId dans les query views utilisateur

Les records `TenantContext` et `UserDetails` retournés par les query handlers SHALL utiliser `TenantId` pour le champ `tenantId` et non `UUID` brut.

#### Scenario: Serialisation JSON de TenantId

- **WHEN** une query view contenant `TenantId` est sérialisée en JSON par le controller
- **THEN** la valeur JSON est une string UUID (ex. `"d3b4c5e6-..."`) et non un objet `{"value": "..."}`

#### Scenario: Typage fort dans le handler

- **WHEN** un query handler construit une `TenantContext` ou `UserDetails`
- **THEN** le champ `tenantId` est typé `TenantId` (pas de cast UUID explicite nécessaire par les consommateurs)
