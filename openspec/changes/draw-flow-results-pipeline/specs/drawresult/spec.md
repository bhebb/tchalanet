# Spec delta: core.drawresult

## ADDED Requirements

### Requirement: draw_result is global

`draw_result` SHALL be global and SHALL NOT be tenant-scoped.

#### Scenario: Persisting fetched external result

- WHEN external result is fetched for result_slot X
- THEN it is persisted once globally as draw_result
- AND tenant draws reference it via `draw.draw_result_id`.

### Requirement: draw_result uniqueness

`draw_result` SHALL enforce uniqueness on `(result_slot_id, occurred_at)`.

### Requirement: draw result writer is atomic

The draw result writer SHALL use atomic SQL upsert with `INSERT ... ON CONFLICT (result_slot_id, occurred_at)`.

### Requirement: force semantics are respected

When `force=false`, writer SHALL NOT overwrite `CONFIRMED` or `OVERRIDDEN` draw results.

#### Scenario: Provider correction on provisional result

- GIVEN an existing PROVISIONAL draw_result with source_hash A
- WHEN provider fetch returns source_hash B
- THEN update is allowed according to source hash rule.

#### Scenario: Existing confirmed result

- GIVEN an existing CONFIRMED draw_result
- WHEN fetch runs with `force=false`
- THEN the confirmed result is not overwritten.

### Requirement: fetch is slot-driven

Fetch SHALL be driven by `result_slot_key`, never by `draw_channel_code` or sold `game_code`.

### Requirement: source hash is central

`source_hash` SHALL be used for idempotency, change detection, and provider correction tracing.

### Requirement: Fetch command is transactional

`FetchExternalResultsWindowCommandHandler` SHALL be transactional because it writes draw_result.

### Requirement: Source result payload is null-safe

`SourceResultBuilder` SHALL handle null `main` and `extra` lists safely.

## MODIFIED Requirements

### Requirement: draw_result domain purity

Raw JSON payloads SHOULD live in persistence/application mapping. If current `DrawResult` carries JsonNode, it is treated as an application/read model until refactored.

### Requirement: statuses

Draw result statuses SHALL be standardized as `PROVISIONAL`, `CONFIRMED`, `OVERRIDDEN`, and optional `REJECTED`/`INVALID`.
