# Task 05 — Manual Result Permission & Trust Routing

## Scope

`core.drawresult` internal + `platform.accesscontrol`

No new table. No new state machine. Extends `RecordManualDrawResultCommand` path.

## New permission

`DRAW_RESULT_MANUAL_PROPOSE` — assignable to tenant admin role.

Register in access control alongside existing draw-result permissions.

## Trust routing

When a caller with `DRAW_RESULT_MANUAL_PROPOSE` submits a manual result:

1. Load `result_slot` for the given slot key.
2. Read `source_cfg.trust_policy`.
3. Route:
   - `AUTO_CONFIRM_HIGH_CONFIDENCE` → `RecordManualDrawResultCommand` with `status=CONFIRMED`
   - `REQUIRE_PLATFORM_REVIEW` → `RecordManualDrawResultCommand` with `status=PROVISIONAL`
   - `MANUAL_ONLY` → `PROVISIONAL`
   - absent → `PROVISIONAL` (safe default)

## Conflict protection

Existing UPSERT key: `(result_slot_id, occurred_at)`.

- `status=CONFIRMED` already exists and `force=false` → reject with a domain exception (409-equivalent).
- `status=PROVISIONAL` already exists → overwrite (new proposal replaces pending one).

No `force=true` for tenant admin — only platform ops can force.

## Endpoint

Thin wrapper or extend existing:

```
POST /admin/draw-results/manual
```

Request: `slotKey`, `resultDate`, `occurredAt`, `numbers` (same shape as existing manual command).

Internally resolves trust_policy and sets status accordingly. Returns the created/updated `draw_result` id and final status.

## Platform confirmation path

If status lands as `PROVISIONAL`, platform admin confirms via:

```
POST /platform/ops/draw-results/{id}/confirm
```

New lightweight endpoint (preferred over `/override` which changes status to OVERRIDDEN):
- Flips `PROVISIONAL → CONFIRMED` in place.
- Audited.
- Requires permission `DRAW_RESULT_CONFIRM` (platform only).

This resolves the open question in design.md: use a dedicated `/confirm` endpoint, not `/override`.

## Tests

- Tenant admin with `DRAW_RESULT_MANUAL_PROPOSE` + `trust_policy=AUTO_CONFIRM_HIGH_CONFIDENCE` → `CONFIRMED`.
- Same + `REQUIRE_PLATFORM_REVIEW` → `PROVISIONAL`.
- `trust_policy` absent → `PROVISIONAL`.
- Already `CONFIRMED` → domain exception, no overwrite.
- Already `PROVISIONAL` → overwritten by new proposal.
- Caller without `DRAW_RESULT_MANUAL_PROPOSE` → 403.
- Tenant admin cannot call platform `/confirm` → 403.

## Acceptance

- No manual role check in controller.
- All writes audited.
- `force=true` never available to tenant admin.
- Confirmed `draw_result` enters existing apply / settle pipeline unchanged.
