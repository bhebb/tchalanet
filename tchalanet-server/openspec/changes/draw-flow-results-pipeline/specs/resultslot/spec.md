# Spec delta: catalog.resultslot

## ADDED Requirements

### Requirement: result_slot is external result expectation

`result_slot` SHALL describe an expected global external result for a provider/date/time.

### Requirement: source config maps provider games

`result_slot.source_cfg` SHALL contain provider-specific external game codes for pick3/pick4.

Example:

```json
{
  "pick3": { "external_game_code": "US_FL_PICK3_MID", "external_key": "PICK3", "active": true },
  "pick4": { "external_game_code": "US_FL_PICK4_MID", "external_key": "PICK4", "active": true }
}
```

### Requirement: projection config source

`result_slot.projection_cfg` SHALL be the source of Haiti projection configuration.

### Requirement: slot timezone is authoritative for results

`result_slot.timezone` and `result_slot.draw_time` SHALL be used to calculate deterministic `occurredAt` when provider does not provide a timestamp.
