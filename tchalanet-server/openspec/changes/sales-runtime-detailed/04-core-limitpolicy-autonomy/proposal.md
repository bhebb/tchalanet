# Change: core.limitpolicy + core.autonomy

## Why

The current limit/autonomy code mixes raw UUIDs, direct handler calls, and overlapping responsibilities.

## What

Stabilize the MVP policy model:

- limitpolicy decides `ALLOW | WARN | REQUIRE_APPROVAL | BLOCK`
- autonomy decides who can approve / which role / autonomy level
- targets: TENANT, OUTLET, USER
- no TERMINAL target for MVP

## Non-goals

- Do not use terminal-level financial policy in MVP.
- Do not duplicate limit/autonomy logic in sales/payout/features.
