# Data Lifecycle — Tchalanet Server

Defines how data grows, ages, and leaves the primary PostgreSQL database.

Related spec: `openspec/changes/data-lifecycle-archive-v1/proposal.md`

---

## Data classes

All tables belong to one of four lifecycle classes.

### CLASS A — Permanent operational master data

Keep online indefinitely. Soft-delete where appropriate. No monthly archive.

| Table                       | Envers | Notes                              |
|-----------------------------|--------|------------------------------------|
| tenant                      | YES    |                                    |
| app_user                    | YES    |                                    |
| app_user_external_identity  | NO     | CLASS A — provider UID ↔ app_user  |
| user_preference             | NO     |                                    |
| tenant_user / tenant_membership | YES |                                  |
| app_role / role_permission  | YES    |                                    |
| permission                  | YES    |                                    |
| user_permission_override    | YES    |                                    |
| tenant_subscription         | YES    |                                    |
| billing_plan                | NO     |                                    |
| address                     | NO     |                                    |

### CLASS B — Config/security data with historical versioning

Keep online. Envers selectively. Functional audit for admin actions. No cold archive in V1.

| Table                        | Envers | Notes                          |
|------------------------------|--------|--------------------------------|
| outlet                       | YES    |                                |
| terminal                     | YES    |                                |
| terminal_binding             | YES    |                                |
| terminal_assignment          | YES    |                                |
| terminal_capability          | NO     |                                |
| terminal_challenge           | NO     |                                |
| sales_zone                   | YES    |                                |
| limit_assignment             | YES    |                                |
| autonomy_policy_rule         | NO     |                                |
| draw_channel                 | YES    |                                |
| draw_channel_game            | YES    |                                |
| result_slot                  | YES    |                                |
| tenant_game                  | YES    |                                |
| app_setting                  | YES    |                                |
| i18n_override                | YES    |                                |
| tenant_theme / theme_preset  | YES    |                                |
| promotion_campaign           | YES    |                                |
| promotion_rule               | YES    |                                |
| promotion_rule_effect        | YES    |                                |
| promotion_rule_eligibility_line | NO  |                                |
| pricing_odds                 | YES    |                                |
| seller_commission_policy     | YES    |                                |

### CLASS C — High-volume transactional data

Archive after retention period. No Envers. Use immutable snapshots + functional audit.

| Table                        | Retention | Partition key | Notes                     |
|------------------------------|-----------|---------------|---------------------------|
| sales_ticket                 | P12M      | sold_at       |                           |
| sales_ticket_line            | P12M      | sold_at       |                           |
| sales_ticket_charge          | P12M      | sold_at       |                           |
| applied_promotion_snapshot   | P12M      | created_at    |                           |
| promotion_decision           | P12M      | created_at    |                           |
| payout                       | P24M      | created_at    |                           |
| payout_workflow_history      | P24M      | created_at    | when introduced           |
| sales_session                | P12M      | opened_at     |                           |
| audit_log                    | P12M      | occurred_at   | new partitioned table     |
| audit_event                  | P12M      | occurred_at   | legacy; migrate to audit_log |
| notification_delivery        | P6M       | created_at    |                           |
| outbound_message             | P6M       | created_at    |                           |
| provider_raw_payload         | P6M       | fetched_at    | when introduced           |
| ledger_entry                 | P24M      | occurred_at   |                           |
| offline_sync_batch           | P12M      | received_at   |                           |
| offline_submission           | P12M      | received_at   |                           |
| idempotency_record           | TTL       | expires_at    | TTL-cleanup job           |

### CLASS D — Derived/read-model data

No Envers. Can be deleted and rebuilt. No legal source of truth.

| Table             | Retention | Notes                               |
|-------------------|-----------|-------------------------------------|
| analytics_daily   | P24M      | rebuildable from tickets/payouts    |
| analytics_draw    | P24M      | rebuildable from draws/tickets      |
| stats_daily       | P24M      | legacy; superseded by analytics_daily |
| stats_draw        | P24M      | legacy; superseded by analytics_draw  |
| stats_event_log   | P12M      |                                     |
| processed_event   | P6M-P12M  | idempotency; no replay beyond window|
| draw_exposure     | P12M      | rebuildable from ticket lines       |

---

## Retention defaults

```yaml
tch:
  archive:
    hot-retention:
      ticket: P12M
      ticket-line: P12M
      payout: P24M
      audit-log: P12M
      notification-delivery-log: P6M
      provider-raw-payload: P6M
    rollover:
      max-rows-per-partition: 50000000
      max-bytes-per-partition: 100GB
      mode: MONITOR_ONLY
```

---

## Envers allowlist

Use `@Audited` only on the following:

```
tenant
app_user
tenant_user (tenant_membership)
app_role
role_permission
user_permission_override
outlet
terminal
terminal_binding
terminal_assignment
sales_zone
limit_assignment
draw_channel
draw_channel_game
result_slot
tenant_game
app_setting
i18n_override
tenant_theme
theme_preset
promotion_campaign
promotion_rule
promotion_rule_effect
pricing_odds
seller
seller_commission_policy
tenant_subscription
billing_plan
```

## Envers blocklist (never audit)

```
sales_ticket_line
sales_ticket_charge
audit_log
audit_event
notification_delivery
outbound_message
idempotency_record
processed_event
stats_daily / analytics_daily
stats_draw / analytics_draw
stats_event_log
draw_exposure
provider_raw_payload
ledger_entry
applied_promotion_snapshot   ← immutable snapshot; no Envers needed
promotion_decision           ← immutable; no Envers needed
```

---

## Archive tenant/RLS model

### Hot PostgreSQL tables

All tenant-scoped tables: `tenant_id` column + RLS via `public.current_tenant()` and
`public.allow_platform_cross_tenant_select()`.

### audit_log (partitioned)

- `tenant_id` + RLS on the root table applies to all partitions.
- Platform scope can read all rows via `allow_platform_cross_tenant_select()`.
- Tenant/admin scope sees only own rows.
- `actor_id` is always `app_user.id` (UUID), never a provider subject.

### archive_lookup_index

Scope-aware RLS — two separate policies:

```sql
-- tenant/admin: own rows only
CREATE POLICY archive_lookup_tenant_read ON archive_lookup_index
  FOR SELECT
  USING (
    current_setting('tch.scope', true) IN ('TENANT','ADMIN')
    AND tenant_id = current_setting('tch.tenant_id', true)::uuid
  );

-- platform: all rows including tenant_id IS NULL (global/platform metadata)
CREATE POLICY archive_lookup_platform_read ON archive_lookup_index
  FOR SELECT
  USING (current_setting('tch.scope', true) = 'PLATFORM');
```

`tenant_id IS NULL` rows are global/platform metadata; tenant/admin callers must never see them.

### Archive files (object storage)

No DB RLS. Rules enforced by the backend service:
- Every row in tenant-scoped files includes `tenant_id` in the payload.
- Object URIs are never returned to tenant/admin users.
- All access goes through `archive_lookup_index` → backend archive service → file.

### archive_restore_* tables

- SUPER_ADMIN only. Outside RLS by design.
- May contain cross-tenant investigation data.
- Mandatory `reason` + functional `ARCHIVE_RESTORE` audit.
- TTL-bound cleanup.

---

## Audit log action taxonomy

Canonical action format: `<DOMAIN>_<VERB>`. Actions are stable constants; do not create
lifecycle-status variants (e.g. `ARCHIVE_RUN_COMPLETED` is wrong — use `ARCHIVE_RUN` + status in
`details`).

See `platform/audit/api/model/AuditAction.java` for the full enum.

Key canonical actions per spec:

| Action             | Domain           |
|--------------------|-----------------|
| TICKET_SELL        | core.sales       |
| TICKET_VOID        | core.sales       |
| PAYOUT_REQUEST     | core.payout      |
| PAYOUT_APPROVE     | core.payout      |
| PAYOUT_REJECT      | core.payout      |
| PAYOUT_PAID        | core.payout      |
| LIMIT_UPDATE       | core.limitpolicy |
| PROMOTION_ACTIVATE | core.promotion   |
| PROMOTION_PAUSE    | core.promotion   |
| ROLE_ASSIGN        | platform.accesscontrol |
| OUTLET_LOCK        | core.outlet      |
| OUTLET_UNLOCK      | core.outlet      |
| TERMINAL_LOCK      | core.terminal    |
| TERMINAL_UNLOCK    | core.terminal    |
| OPS_FORCE_JOB      | ops/scheduler    |
| TENANT_OVERRIDE    | context/super-admin |
| CACHE_CLEAR        | ops              |
| ARCHIVE_RUN        | platform.archive |
| ARCHIVE_RESTORE    | platform.archive |

---

## Dashboards and reports

Dashboards must read `analytics_daily` / `analytics_draw` (CLASS D), not raw `sales_ticket_line`.

Historical report read order:
1. analytics projections
2. hot transactional tables if needed
3. `archive_lookup_index` + archive read-from-archive service only when the date range requires it

Archive is for lookup, dispute, compliance and rare historical extraction — not live dashboards.
