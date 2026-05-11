# common.context spec delta

## ADDED Requirements

### Requirement: Trust level on operational context source

`OperationalContextSource` SHALL expose a `TrustLevel` (`NONE`, `WEAK`, `STRONG`) and a helper `isTrustedForSensitiveOperation()` returning `trustLevel == STRONG`.

#### Scenario: Strong sources are trusted

- **GIVEN** an `OperationalContextSource` of `SERVER_BOOTSTRAP`, `SIGNED_DEVICE_BINDING`, or `ADMIN_SELECTION`
- **WHEN** `isTrustedForSensitiveOperation()` is evaluated
- **THEN** it SHALL return `true`

#### Scenario: Weak source is not trusted

- **GIVEN** an `OperationalContextSource` of `CLIENT_CLAIM`
- **WHEN** `isTrustedForSensitiveOperation()` is evaluated
- **THEN** it SHALL return `false`

#### Scenario: No source is not trusted

- **GIVEN** an `OperationalContextSource` of `NONE`
- **WHEN** `isTrustedForSensitiveOperation()` is evaluated
- **THEN** it SHALL return `false`

### Requirement: Operational request context shape

`OperationalRequestContext` SHALL carry exactly `(terminalId, outletId, salesSessionId, source)` and SHALL NOT carry a `selectedByAdmin` flag — admin selection is expressed by `source == ADMIN_SELECTION`.

#### Scenario: Admin selection is expressed by source

- **GIVEN** an admin selects a POS context
- **WHEN** the resolver builds the `OperationalRequestContext`
- **THEN** `source` SHALL be `ADMIN_SELECTION`
- **AND** no separate `selectedByAdmin` boolean SHALL exist on the record

### Requirement: trustedOperationalContextRequired helper

`TchRequestContext` SHALL expose `trustedOperationalContextRequired(): OperationalRequestContext` that throws when the context is missing, the source is not trusted, or required ids (`terminalId`, `outletId`) are missing.

#### Scenario: Missing operational context

- **GIVEN** a `TchRequestContext` with `operationalContext == null`
- **WHEN** `trustedOperationalContextRequired()` is called
- **THEN** the helper SHALL throw `OperationalContextNotTrustedException`

#### Scenario: Weak source

- **GIVEN** a `TchRequestContext` with `source == CLIENT_CLAIM`
- **WHEN** `trustedOperationalContextRequired()` is called
- **THEN** the helper SHALL throw `OperationalContextNotTrustedException`

#### Scenario: Trusted context

- **GIVEN** a `TchRequestContext` with `source == SIGNED_DEVICE_BINDING`, `terminalId` set, `outletId` set
- **WHEN** `trustedOperationalContextRequired()` is called
- **THEN** the helper SHALL return the operational request context unchanged

## REMOVED Requirements

### Requirement: selectedByAdmin field

**Reason**: Redundant with `source == ADMIN_SELECTION`. Trust is a property of the source, not a side flag.

**Migration**: Replace every read of `operationalContext.selectedByAdmin()` with `operationalContext.source() == OperationalContextSource.ADMIN_SELECTION`.
