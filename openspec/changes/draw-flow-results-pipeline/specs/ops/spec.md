# Spec delta: features.ops

## ADDED Requirements

### Requirement: Ops debug endpoints exist for draw flow

Ops SHALL expose manual endpoints for generate, open, close, fetch, apply, refresh, manual result, and override result.

### Requirement: Fetch endpoint is global

`POST /platform/ops/draw-results/fetch` SHALL run with `tenantId = null` and gate tenant null.

### Requirement: Apply endpoint is tenant-scoped

`POST /platform/ops/draw-results/apply` SHALL require tenantId.

### Requirement: Refresh endpoint is tenant-scoped

`POST /platform/ops/draw-results/refresh` SHALL require tenantId and orchestrate fetch global then apply tenant.

### Requirement: Apply belongs under draw-results Ops

Apply endpoint SHALL live under `/platform/ops/draw-results/apply`, not `/platform/ops/draws/apply`.

### Requirement: Ops controllers use Clock

Ops controllers SHALL inject `Clock` for default time/date values.

### Requirement: Ops defaults come from config

Ops controllers SHALL use configured defaults for max slots/items rather than hardcoded values.

### Requirement: Sensitive Ops actions are audited

Manual result, override, force refresh, and other sensitive operations SHALL be audited.

### Requirement: Slot keys validation

Ops requests SHALL validate `slotKeys` explicitly or support an explicit `allActiveSlots` mode.
