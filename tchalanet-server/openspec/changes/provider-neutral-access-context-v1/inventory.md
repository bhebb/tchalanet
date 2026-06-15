# Inventory: Provider-Neutral Access Context V1

Produced during Slice 0 (DB inventory). Read-only. All findings reference
`tchalanet-app/src/main/resources/db/migration/`.

---

## 1. Table Existence and Name Mapping

Spec names (from tasks §2.2) vs. actual table names in migrations:

| Spec name | Actual DB name | Migration | Exists? |
|---|---|---|---|
| `app_user` | `app_user` | V100 | YES |
| `app_user_external_identity` | `app_user_external_identity` | V100 | YES |
| `tenant_user` | `tenant_user` | V100 | YES |
| `tenant_user_role` | `tenant_user_role` | V100 | YES |
| `platform_user_role` | — | — | **MISSING** |
| `app_role` | `app_role` | V100 | YES |
| `permission` | `permission` | V100 | YES |
| `app_role_permission` | `role_permission` | V100 | YES — **name differs** |
| `tenant_user_permission_override` | `user_permission_override` | V100 | YES — **name differs** |

### Name conflict decisions

**`role_permission` vs `app_role_permission`:** Reuse `role_permission`. Rename is unnecessary;
the spec name was aspirational. All spec references to `app_role_permission` resolve to
`role_permission`.

**`user_permission_override` vs `tenant_user_permission_override`:** Reuse
`user_permission_override`. Rename is unnecessary. The table already has `(tenant_id, user_id,
permission_code)`. All spec references to `tenant_user_permission_override` resolve to
`user_permission_override`.

---

## 2. Structural Differences

### tenant_user

V100 schema:
```sql
CONSTRAINT uq_tenant_user__tenant_user UNIQUE (tenant_id, user_id)
```
Field name is `user_id` (not `app_user_id`).

Spec requires (tasks §2.4):
```sql
CREATE UNIQUE INDEX uq_tenant_user_one_active_per_app_user
ON tenant_user(user_id)
WHERE status = 'ACTIVE';
```

`status` column exists but is nullable. Needs:
1. New partial unique index (`WHERE status = 'ACTIVE'`) on `user_id`.
2. No rename of `user_id` → `app_user_id` required (field name is internal).

### tenant_user_role

V100 schema: links `(tenant_id, user_id, role_id UUID)` — role referenced by UUID FK to
`app_role.id`. Unique index: `(tenant_id, user_id, role_id) WHERE deleted_at IS NULL`.

Spec describes `unique(tenant_user_id, role_code)` but the actual table does not have a
`tenant_user_id` FK column, and uses role UUID not role code.

**Decision:** Keep existing structure. The spec constraint description was aspirational. The
effective uniqueness is already enforced by the existing partial unique index. No migration
needed for this constraint.

### user_permission_override

V100 schema: links `(tenant_id, user_id, permission_code)`. Unique index:
`(tenant_id, user_id, permission_code) WHERE deleted_at IS NULL`.

Spec describes `unique(tenant_user_id, permission_key)` — no `tenant_user_id` FK in actual
table; `permission_code` is used (not `permission_key`). Constraint is effectively equivalent.

**Decision:** Keep existing structure. No migration needed for this constraint.

---

## 3. Missing Table: platform_user_role

No table for platform-scoped role assignments exists. The `tenant_user_role` table includes
both tenant-scoped and platform-scoped role assignments (SUPER_ADMIN is assigned via
`tenant_user_role` in V202, scoped by `app_role.scope = 'PLATFORM'`).

V202 seed: SUPER_ADMIN assigned to the seed user via `tenant_user_role` with the platform role id.

**Decision:** Do not create a separate `platform_user_role` table in this change. Resolve
platform roles from `tenant_user_role JOIN app_role ON role_id WHERE app_role.scope = 'PLATFORM'`.
`AccessResolutionFilter` must distinguish platform vs tenant roles by `app_role.scope`, not by
a separate table. Document this in the design.

---

## 4. Existing Roles (V202 seed)

| Role code | Scope | UUID | Status |
|---|---|---|---|
| `SUPER_ADMIN` | PLATFORM | `...000301` | EXISTS |
| `TENANT_ADMIN` | TENANT | `...000302` | EXISTS |
| `OPERATOR` | TENANT | `...000303` | EXISTS (legacy) |
| `CASHIER` | TENANT | `...000304` | EXISTS (legacy) |

**Missing:** `TENANT_OWNER`. Must be added in Slice 2 seed migration.

---

## 5. Existing Permissions (V202 seed)

Permissions relevant to this change that already exist:

```
terminal.read, terminal.create, terminal.update, terminal.disable, terminal.bind, terminal.unbind
session.read, session.open, session.close, session.force-close
ticket.sell, ticket.read, ticket.print, ticket.resend, ticket.verify, ticket.cancel-own
payout.read, payout.review, payout.execute
limit.read, limit.manage
promotion.read, promotion.manage
report.read, audit.read
platform.access, platform.ops.read, platform.ops.execute
tenant.override
```

**Missing permissions** (required by tasks §4.2) that do not exist in V202:

```
terminal.manage         (tasks §4.2 — admin surface)
terminal.block          (tasks §4.2 — sensitive admin action)
terminal.reset_pin      (tasks §4.2 — sensitive admin action)
sales.read              (tasks §4.2)
sales.report.read       (tasks §4.2)
ticket.void             (tasks §4.2 — sensitive action)
odds.read               (tasks §4.2)
odds.manage             (tasks §4.2 — sensitive action)
draw_result.read        (tasks §4.2)
draw_result.manage      (tasks §4.2)
draw_result.confirm     (tasks §4.2 — sensitive action)
payout.mark_paid        (tasks §4.2 — sensitive action; maps to existing payout.execute?)
billing.read            (tasks §4.2)
platform.tenant.manage  (tasks §4.2 — maps to existing tenant.* permissions?)
platform.ops.run        (tasks §4.2 — maps to existing platform.ops.execute?)
terminal.me.read        (tasks §4.2 — terminal-derived)
terminal.sell           (tasks §4.2 — terminal-derived; distinct from ticket.sell)
terminal.ticket.read_own  (tasks §4.2 — terminal-derived)
terminal.ticket.reprint_own (tasks §4.2 — terminal-derived)
```

**Mapping notes:**
- `payout.mark_paid` — semantically equivalent to `payout.execute`. Add `payout.mark_paid` as
  a new distinct permission; `payout.execute` (cashier) and `payout.mark_paid` (admin) serve
  different surfaces. Do not remove `payout.execute`.
- `platform.ops.run` — similar to `platform.ops.execute`. Add `platform.ops.run` as a
  human-readable alias or replace. Confirm with access-control-v1 before writing the migration.
- `platform.tenant.manage` — overlaps with `tenant.create`, `tenant.update`, etc. Add as a
  coarse-grained platform permission in addition to the fine-grained ones.
- `terminal.sell` — distinct from `ticket.sell`. `ticket.sell` is a cashier-role permission.
  `terminal.sell` is derived from SellerTerminal actor resolution, not a seeded role permission.

---

## 6. RLS Classification

### Bootstrap tables — RLS exception candidates

These tables are read before tenant context exists and must not have tenant RLS policies:

| Table | Currently has RLS? | Action needed |
|---|---|---|
| `app_user` | NO | None |
| `app_user_external_identity` | NO | None |
| `tenant_user` | **YES** | **Remove RLS policies** |
| `tenant_user_role` | NO | None |
| `app_role` | NO | None |
| `permission` | NO | None |
| `role_permission` | NO | None |
| `user_permission_override` | NO | None |

`tenant_user` has two RLS policies in V105:
- `tenant_user_rls_all` — FOR ALL with tenant_id check
- `tenant_user_rls_select` — FOR SELECT with platform override

These must be dropped. `tenant_user` is protected by:
- `unique(tenant_id, user_id)` constraint
- Module boundaries and controlled repository
- `status` and `deleted_at` filters in queries
- Audit on mutations

Migration: one `ALTER TABLE tenant_user DISABLE ROW LEVEL SECURITY` + drop policies.

### Business/control tables — RLS required

These must remain tenant-scoped and RLS-protected:

```
terminal, terminal_capability, terminal_assignment, terminal_binding, terminal_challenge
seller, seller_outlet_assignment, seller_commission_policy
sales_ticket, sales_ticket_line, sales_ticket_charge
sales_session, payout
draw, draw_channel, draw_channel_game
outlet, address
offline_grant, offline_sync_batch, offline_code_batch, offline_submission, offline_code
promotion_campaign, promotion_rule, promotion_rule_effect, promotion_decision
limit_assignment, draw_exposure, ledger_entry
audit_event (already scoped)
```

### Global tables — no tenant_id, no RLS

```
app_role, permission, role_permission  (global catalog — no tenant_id column)
app_user, app_user_external_identity  (global identity — no tenant_id column)
game, result_slot, result_slot_calendar_override  (global config)
tchala_entry, tchala_entry_number     (global catalog)
theme_preset                          (global catalog)
```

---

## 7. access-control-v1 Conflict Check

The `access-control-v1` change uses the same underlying tables (`app_role`, `permission`,
`role_permission`, `user_permission_override`, `tenant_user_role`). V202 was produced by
`access-control-v1`.

Confirmed: no structural conflicts between the two changes. The new permissions and
`TENANT_OWNER` role in this change are additive to V202.

Permission codes must not duplicate existing codes. Checked: none of the missing permissions
listed in §5 conflict with existing V202 codes.

---

## 8. Next Migration Number

Last migration: `V230__seed_archive_permissions.sql`.

Next available: **V231**.

Slice 2 migrations:
- `V231__access_context_v1_schema.sql` — `platform_user_role` removal note + `tenant_user`
  partial index + `tenant_user` RLS removal
- `V232__access_context_v1_seeds.sql` — `TENANT_OWNER` role + missing permissions + role-permission matrix
