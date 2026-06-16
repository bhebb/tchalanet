# Change: Auditing Envers Mapping V0

## Status

Proposed

## Goal

Define which tables Tchalanet audits with Hibernate Envers in V0.

Audit only fields that affect money, access, risk, and draw results.
Do not add Envers to high-volume transaction tables — `ticket`, `ticket_line`, `payout` — those use immutable rows and `audit_log` business events.

## Core principle

```text
audit_log  = who did what, why, from where, with which request/trace id
Envers     = what critical field had which value at which revision
```

Envers does not replace `audit_log`. Business commands still write audit events.

## Audit classes

### Class A — Envers + audit_log

Both field history and business intention matter.

- `seller_terminal` — status and commission affect the right to sell and money
- `odds_rule` / `odds_profile` — affect financial exposure per draw
- `limit_rule` / `limit_profile` — cap risk per draw
- `manual_draw_result` — tamper-evident draw result history
- `tenant_sales_policy` — commission and flags affect settlement

### Class B — audit_log only

Event matters more than field-by-field history.

- ticket void / cancel
- reset PIN / access
- Firebase technical user provisioning
- tenant override
- login failures
- payout mark-paid / void

### Class C — no Envers

High-volume, immutable, append-only, or projection tables.

- `ticket` — immutable once written; void tracked via audit_log
- `ticket_line` — immutable snapshots
- `payout` — high-volume transaction; paid/void events in audit_log
- `audit_log` — append-only
- `app_user` — PII; lock/disable events in audit_log

## Revision table

Use standard `revinfo`. No custom revision entity in V0.

## Scope

- `tchalanet-server` — JPA entity annotations and `revinfo` configuration only.
- No Flyway migration for `_aud` tables — Envers manages them via `hibernate.envers.auto_ddl` or a controlled Flyway script per entity.
