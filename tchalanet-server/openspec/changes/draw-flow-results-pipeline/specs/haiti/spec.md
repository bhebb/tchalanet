# Spec delta: core.haiti

## ADDED Requirements

### Requirement: Haiti projection domain is pure

`core.haiti.domain.*` SHALL NOT depend on Spring, Jackson, JPA, or logging.

### Requirement: Projection requires complete external bundle in MVP

For MVP, Haiti projection SHALL require both pick3 and pick4.

### Requirement: Projection config comes from result_slot

Haiti projection config SHALL be resolved from `result_slot.projection_cfg`.

### Requirement: Haiti result JSON is versioned

`draw_result.haiti_result` SHALL be stored as versioned structured JSON.

Example:

```json
{
  "version": 1,
  "rule_set": "DEFAULT",
  "lots": {
    "lot1": "123",
    "lot2": "12",
    "lot3": "23",
    "lot4": "12"
  }
}
```

## MODIFIED Requirements

### Requirement: Haiti adapter maps domain to contract

`HaitiLotteryAdapter` SHALL convert domain projection result and flags into the application/common contract.

## REMOVED Requirements

### Requirement: draw_channel flags projection

References to `draw_channel.flags.projection` SHALL be removed for draw result projection.
