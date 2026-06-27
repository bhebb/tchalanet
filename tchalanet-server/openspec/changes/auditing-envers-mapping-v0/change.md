# Change: Auditing Envers Mapping V0

## Status

Proposed

## Goal

Define which tables Tchalanet audits with Hibernate Envers in V0.

Audit only fields that affect money, access, risk, and draw results.
Do not add Envers to high-volume transaction tables — `ticket`, `ticket_line`, `payout` — those use immutable rows and `audit_event` business events.

## Core principle

```text
audit_event = who did what, why, from where, with which request/trace id
Envers      = what critical field had which value at which revision
```

Envers does not replace `audit_event`. Business commands still write audit events.

## Audit classes

### Class A — Envers + audit_event

Both field history and business intention matter.

- `seller_terminal` — status and commission affect the right to sell and money
- `draw_result` — tamper-evident draw result history
- `limit_assignment` — cap risk per draw/entity assignment

### Class B — audit_event only

Event matters more than field-by-field history.

- ticket void / cancel
- reset PIN / access
- Firebase technical user provisioning
- tenant override
- login failures
- payout mark-paid / void
- odds/pricing updates until they are explicitly approved for Envers exposure

### Class C — no Envers

High-volume, immutable, append-only, or projection tables.

- `ticket` — immutable once written; void tracked via `audit_event`
- `ticket_line` — immutable snapshots
- `payout` — high-volume transaction; paid/void events in `audit_event`
- `audit_event` — append-only functional audit
- `app_user` — PII; lock/disable events in `audit_event`

## Revision table

Use the custom `revinfo` revision entity owned by `platform.entityhistory` so Envers revisions can
capture request context safely.

## Scope

- `tchalanet-server` — JPA entity annotations, `platform.entityhistory`, and controlled `revinfo`/`*_aud` migrations.
- Fresh-database Flyway creates only the allowlisted `_aud` tables; do not rely on browser access to
  raw Envers tables.
