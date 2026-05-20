# Spec — ops-manual-controls

## ADDED Requirements

### Requirement: Manual Ops controls for scheduler steps

Every automatic scheduler step MUST have an Ops/manual control path.

#### Scenario: Ops endpoints available

- **THEN** the system MUST expose authorized Ops controls for:
  - generate draws
  - open today draws
  - close due draws
  - fetch draw results
  - apply draw results
  - settle tickets

### Requirement: Force requires reason

Forced operations MUST require a non-blank reason.

#### Scenario: Force without reason

- **WHEN** an Ops request has `force=true` and no non-blank reason
- **THEN** the system MUST reject the request

#### Scenario: Force with reason

- **WHEN** an authorized Ops request has `force=true` and a non-blank reason
- **THEN** the system MAY bypass automatic time windows and retry intervals
- **AND** MUST still enforce critical business invariants

### Requirement: Forced operations audited

Forced Ops operations MUST be audited.

#### Scenario: Forced fetch

- **WHEN** an authorized Ops user triggers `force=true` fetch
- **THEN** the system MUST record an audit log with action, target criteria, actor, reason, and outcome

### Requirement: Dry run support

Manual Ops commands MUST support dry-run where feasible.

#### Scenario: Dry run close

- **WHEN** an Ops user calls close due with `dryRun=true`
- **THEN** the system SHOULD return the candidate draws that would be closed
- **AND** MUST NOT mutate state

### Requirement: Targeting by draw date, slot keys, or draw ids

Ops commands MUST support practical targeting.

#### Scenario: Target by draw ids

- **WHEN** `drawIds` are provided
- **THEN** the command SHOULD prioritize the explicit draw ids

#### Scenario: Target by draw date and slot keys

- **WHEN** `drawIds` are not provided
- **AND** `drawDate` and `slotKeys` are provided
- **THEN** the command SHOULD target matching draws or result slots

### Requirement: Fetch optional draw time override

Ops fetch MAY support a temporary `drawTimeOverride` for provider/debug cases, and when supported it MUST be request-scoped.

#### Scenario: Draw time override

- **WHEN** an Ops fetch request includes `drawTimeOverride`
- **THEN** the system MAY use it to calculate the external result instant for that request
- **AND** MUST NOT persist it back to `result_slot`
- **AND** MUST audit the forced/debug action when used with force
