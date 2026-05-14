# event-contract Spec

## ADDED Requirements

### Requirement: Domain events remain domain-specific

Domains SHALL publish domain-specific facts, not generic notification/message events.

#### Scenario: Payout rejected

- **GIVEN** payout rejection is committed
- **WHEN** the domain publishes an event
- **THEN** it publishes `PayoutRejectedEvent`
- **AND** it does not publish `SendEmailEvent` or `NotificationRequestedEvent` as the primary event

### Requirement: Base event metadata

Events consumed by notification/communication SHALL expose or be adaptable to:

- event id;
- occurred at;
- tenant id when tenant-scoped;
- actor id when actor-sensitive;
- stable business IDs;
- rendering facts.

#### Scenario: Communication rule evaluates event

- **GIVEN** a supported event is received
- **WHEN** the communication rule evaluates it
- **THEN** the rule can build a stable `correlationKey` from event facts

### Requirement: Optional notifiable contract

The system MAY define `NotifiableDomainEvent` for events that naturally expose notification facts.

#### Scenario: Event implements optional contract

- **GIVEN** an event implements `NotifiableDomainEvent`
- **WHEN** platform rules consume it
- **THEN** they may read key, severity, audience hint and facts from the contract

### Requirement: Typed adapters remain allowed

The system SHALL allow platform rules to adapt specific event classes without requiring the event to implement `NotifiableDomainEvent`.

#### Scenario: Legacy event without contract

- **GIVEN** `TenantUserRoleChangedEvent` does not implement `NotifiableDomainEvent`
- **WHEN** `TenantUserCommunicationRule` supports it
- **THEN** the rule may map it to communication intents using typed accessors

### Requirement: Platform intents are normalized

`platform.notification` and `platform.communication` SHALL use normalized intent objects internally.

#### Scenario: Multiple event sources

- **GIVEN** payout and batch events are consumed
- **WHEN** they are mapped by platform rules
- **THEN** notification persistence receives `NotificationIntent`
- **AND** communication enqueue receives `CommunicationIntent`
