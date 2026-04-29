# Spec — Draw Execution Pipeline Hardening (draw-execution-pipeline-hardening)

## Status

**PROPOSED**

---

## ADDED Requirements

### Requirement: Runtime configuration for draw execution SHALL be single-source and bind correctly

The draw execution pipeline SHALL load its runtime configuration from a coherent and non-duplicated
set of YAML sources. Spring configuration imports, property prefixes, and bound configuration
classes SHALL resolve to the files and keys actually present in the repository.

#### Scenario: Specialized YAML imports resolve correctly

- **WHEN** the application context loads draw-related configuration
- **THEN** the specialized draw and us-lottery YAML files are imported using their actual filenames
- **AND** no required draw-results or us-lottery property set is silently skipped because of a wrong import path

#### Scenario: Duplicate runtime keys are removed or documented as compatibility aliases

- **WHEN** draw-results or us-lottery properties are defined
- **THEN** each runtime property has a single source of truth
- **AND** any temporary compatibility alias is explicitly documented and tested

---

### Requirement: Haiti projection configuration SHALL resolve from result_slot first

The draw-result ingestion pipeline SHALL resolve Haiti projection rules from the targeted
`result_slot.projection_cfg` when available and valid. A documented default projection MAY be used
only as a fallback.

#### Scenario: Slot-specific projection config is present

- **GIVEN** a `result_slot` with a valid `projection_cfg`
- **WHEN** external results are fetched or a manual result is recorded for that slot
- **THEN** the Haiti projection uses that slot-specific configuration

#### Scenario: Slot-specific projection config is absent

- **GIVEN** a `result_slot` without a valid `projection_cfg`
- **WHEN** external results are fetched or a manual result is recorded
- **THEN** the pipeline falls back to the documented default projection configuration

---

### Requirement: Draw results operations SHALL use dedicated batch gates

Each draw-results operation exposed to schedulers or ops SHALL use the gate that matches its own
behavior, so that fetch, apply, refresh, manual entry, and override can be controlled independently.

#### Scenario: Manual result operation gate

- **WHEN** a manual draw-result operation is triggered from ops
- **THEN** the operation checks the dedicated manual gate
- **AND** it does not reuse the refresh gate implicitly

#### Scenario: Override operation gate

- **WHEN** an override draw-result operation is triggered from ops
- **THEN** the operation checks the dedicated override gate
- **AND** it does not reuse the refresh gate implicitly

---

### Requirement: Provider resolution SHALL be driven by slot source configuration and active runtime providers

The external-results fetch pipeline SHALL derive provider matching from `result_slot.source_cfg`
and from the set of enabled runtime provider clients.

#### Scenario: Slot source configuration targets active provider channels

- **GIVEN** a `result_slot.source_cfg` containing active external game channel codes
- **WHEN** the fetch pipeline resolves provider inputs
- **THEN** it queries the enabled provider client matching the slot provider
- **AND** it requests only the configured external channel codes for that slot

#### Scenario: Slot has no usable provider game mapping

- **GIVEN** a `result_slot` without a usable active external mapping
- **WHEN** the fetch pipeline runs
- **THEN** the slot is reported as not fetchable
- **AND** no invalid provider request is issued

---

### Requirement: Draw execution documentation SHALL match the implemented pipeline

The backend domain documentation and the functional draw-execution documentation SHALL describe the
same pipeline, with the same scheduler responsibilities, event semantics, and cross-domain roles.

#### Scenario: Backend and functional docs describe the same phases

- **WHEN** the draw execution flow is documented
- **THEN** backend and functional docs describe the same phases:
  `generate`, `open`, `close`, `fetch`, `apply`, `settle`
- **AND** they identify the same owning domain for each phase

#### Scenario: Event semantics are documented consistently

- **WHEN** `DrawResultIngestedEvent` is documented
- **THEN** its payload and scope are described consistently with the implemented event contract
