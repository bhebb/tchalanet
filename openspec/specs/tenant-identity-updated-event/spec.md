## ADDED Requirements

### Requirement: TenantIdentityUpdatedEvent domain event

Le système SHALL publier un `TenantIdentityUpdatedEvent` after-commit lorsque
`UpdateTenantIdentityCommandHandler` persiste un changement d'identité.

Le record SHALL contenir :

- `eventId: EventId` — généré via `IdGenerator.newUuid()`
- `occurredAt: Instant` — horodatage de l'opération
- `tenantId: TenantId` — identifiant du tenant modifié
- `changedFields: Set<String>` — ensemble des champs modifiés (`"name"`, `"timezone"`, `"currency"`)

Le event SHALL implémenter `DomainEvent`.

#### Scenario: Événement publié après mise à jour d'identité

- **WHEN** `UpdateTenantIdentityCommandHandler` persiste un changement
- **THEN** `TenantIdentityUpdatedEvent` est publié after-commit via `DomainEventPublisher`
- **AND** `changedFields` contient exactement les champs qui ont changé

#### Scenario: Événement non publié si aucun changement

- **WHEN** la command n'apporte aucune modification effective (valeurs identiques)
- **THEN** aucun event n'est publié

### Requirement: Aucun consommateur requis initialement

Aucun listener de `TenantIdentityUpdatedEvent` ne SHALL être créé dans ce change.
Le event est publié en anticipation de besoins futurs (invalidation cache, search index).
La documentation SHALL indiquer les consommateurs attendus.

#### Scenario: Consommateurs documentés mais non implémentés

- **WHEN** un développeur cherche les consommateurs de `TenantIdentityUpdatedEvent`
- **THEN** le Javadoc du event liste les consommateurs attendus (cache, search)
- **AND** aucun `@EventListener` ou `@TransactionalEventListener` n'est créé dans ce change
