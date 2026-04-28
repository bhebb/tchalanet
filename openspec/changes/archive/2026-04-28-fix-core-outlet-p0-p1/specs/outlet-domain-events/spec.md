## ADDED Requirements

### Requirement: OutletDayClosedEvent

Le systeme SHALL creer `core/outlet/domain/event/OutletDayClosedEvent.java`.
Record avec : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`, `LocalDate closedDate`.
Implements `DomainEvent`.

`CloseOutletDayCommandHandler` SHALL publier `OutletDayClosedEvent` after-commit via `DomainEventPublisher`.
Le handler SHALL injecter `DomainEventPublisher`, `IdGenerator`, `Clock`.

Consommateurs attendus (documentés, non implementes dans ce change) :

- `features/stats/aggregates` : mettre a jour les aggregats journaliers
- `features/pos` : invalider le cache de session POS

#### Scenario: CloseOutletDay publie l'event

- **WHEN** `CloseOutletDayCommandHandler` ferme un jour avec succes
- **THEN** `OutletDayClosedEvent` est publie after-commit
- **AND** l'event contient `tenantId`, `outletId`, et la date fermee

### Requirement: OutletDayReopenedEvent

Le systeme SHALL creer `core/outlet/domain/event/OutletDayReopenedEvent.java`.
Record avec : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`, `LocalDate reopenedDate`.
Implements `DomainEvent`.

`ReopenOutletDayCommandHandler` SHALL publier `OutletDayReopenedEvent` after-commit.

Consommateurs attendus : `features/stats/aggregates`, `features/pos`.

#### Scenario: ReopenOutletDay publie l'event

- **WHEN** `ReopenOutletDayCommandHandler` reouvre un jour avec succes
- **THEN** `OutletDayReopenedEvent` est publie after-commit

### Requirement: OutletConfigUpdatedEvent

Le systeme SHALL creer `core/outlet/domain/event/OutletConfigUpdatedEvent.java`.
Record avec : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`.
Implements `DomainEvent`.

`UpdateOutletConfigCommandHandler` SHALL publier `OutletConfigUpdatedEvent` after-commit.

Consommateurs attendus : `features/pos` (invalider cache config POS).

#### Scenario: UpdateOutletConfig publie l'event

- **WHEN** `UpdateOutletConfigCommandHandler` met a jour la config avec succes
- **THEN** `OutletConfigUpdatedEvent` est publie after-commit

### Requirement: Typed IDs dans Outlet et OutletView

`Outlet.addressId` SHALL etre de type `AddressId` (pas `UUID`).
`Outlet.withAddressId(UUID)` SHALL etre remplace par `withAddressId(AddressId)`.
`OutletView.id` SHALL etre `OutletId`. `OutletView.tenantId` SHALL etre `TenantId`.
La conversion `AddressId <-> UUID` SHALL se faire dans `OutletPersistenceAdapter`.

#### Scenario: Outlet construit avec AddressId

- **WHEN** un outlet est charge depuis la DB
- **THEN** `outlet.addressId()` retourne un `AddressId` non-null (ou null si absent)
- **AND** aucun `UUID` brut n'est expose par le domaine

#### Scenario: OutletView avec typed IDs

- **WHEN** `ListOutletsByTenantQueryHandler` retourne une liste d'outlets
- **THEN** `OutletView.id` est de type `OutletId`
- **AND** `OutletView.tenantId` est de type `TenantId`

### Requirement: CreateOutletCommand retourne OutletId

`CreateOutletCommandHandler` SHALL implementer `CommandHandler<CreateOutletCommand, OutletId>`.
Le handler SHALL retourner `OutletId.of(newId)`.
Le controller appelant SHALL etre adapte pour recevoir `OutletId`.

#### Scenario: Creation outlet retourne OutletId

- **WHEN** un outlet est cree via `CreateOutletCommand`
- **THEN** le handler retourne un `OutletId`
- **AND** le controller retourne cet `OutletId` dans `ApiResponse`
