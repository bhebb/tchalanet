# CATALOG_PLAN — Plan Catalog V1

## Status

Normative for the `catalog.plan` refactoring and V1 plan mapping.

## Decision

`catalog.plan` remains the source of truth for commercial plan definitions.
It is a read-mostly catalog, not a subscription lifecycle module and not a runtime entitlement engine.

```text
catalog.plan = what a plan contains
core.subscription = which plan a tenant has and its lifecycle
platform.entitlement = what the tenant can do right now
```

## Responsibilities

`catalog.plan` owns:

- plan code, name, description, price and billing period;
- active/default/deleted flags;
- plan features JSON;
- plan limits JSON;
- read-only public API via `PlanCatalog`;
- internal admin CRUD and cache eviction.

`catalog.plan` does not own:

- tenant subscription state;
- tenant-specific overrides;
- runtime availability decisions;
- feature gating annotations;
- business enforcement in sales, payout, terminal, offline, promotion.

## Existing API kept

```java
public interface PlanCatalog {
  List<PlanView> listActive();
  Optional<PlanView> findByCode(String code);
  Optional<PlanView> findById(PlanId id);
}
```

Rules:

- `PlanCatalog` is the only public read API.
- `core.subscription` and `platform.entitlement` may consume `catalog.plan.api`.
- No module may consume `catalog.plan.internal`.
- `findByCode` may return inactive plans for admin/subscription validation use cases, but always filters soft-deleted rows.

## Plan codes V1

Public plans:

```text
STARTER
STANDARD
PRO
```

Special plan:

```text
DEMO
```

Not public in V1:

```text
ENTERPRISE
```

Enterprise remains a future/manual contract concept. Do not expose it as a public plan in V1.

## Features JSON shape

For V1, keep the JSON simple and cumulative.
Because the existing schema has no `inheritsFrom`, every plan stores its full effective feature set.

```json
{
  "sales.ticket.sell": true,
  "sales.ticket.lookup": true,
  "terminal.licensing": true,
  "promotion.rules.basic": false
}
```

Rules:

- Values are booleans.
- Missing key means `false`.
- Plans are cumulative by data, not by runtime inheritance for V1.
- Do not implement a DB inheritance model yet.

## Limits JSON shape

```json
{
  "limits.users.max": 15,
  "limits.outlets.max": 3,
  "limits.terminals.max": 10,
  "limits.mobile_devices.max": 2,
  "limits.promotion_rules.max": 0,
  "limits.offline_days.max": 0,
  "limits.exports.rows.max": 10000
}
```

Rules:

- Values are integers for V1.
- Missing limit means caller-provided default.
- `platform.entitlement` parses the limits into a stable snapshot.
- Business domains must not parse `PlanView.limitsJson()` directly.

## Core features included in all plans

These are present in STARTER, STANDARD, PRO and DEMO.

```text
tenant.profile.basic
auth.login.basic
user.self.profile
sales.ticket.sell
sales.ticket.lookup
sales.ticket.reprint
draw.active.list
drawresult.public.view
payout.basic
document.receipt.basic
pos.web.basic
reporting.daily.basic
security.role.basic
audit.basic
```

## STARTER features

STARTER includes core features plus:

```text
user.management.basic
outlet.management.basic
terminal.management.basic
session.cashier.basic
sales.ticket.cancel.basic
payout.session.basic
reporting.sales.summary
reporting.payout.summary
tenant.theme.logo
document.receipt.logo
```

STARTER limits:

```json
{
  "limits.users.max": 5,
  "limits.outlets.max": 1,
  "limits.terminals.max": 2,
  "limits.mobile_devices.max": 0,
  "limits.promotion_rules.max": 0,
  "limits.offline_days.max": 0
}
```

## STANDARD features

STANDARD includes STARTER plus:

```text
user.management.standard
user.role.assignment.basic
outlet.management.multi
terminal.licensing
terminal.device.binding
session.supervision
sales.ticket.void.admin
payout.admin.review
reporting.dashboard.standard
reporting.export.csv
document.receipt.pdf
notification.in_app
tenant.theme.basic_branding
limitpolicy.basic
```

STANDARD limits:

```json
{
  "limits.users.max": 15,
  "limits.outlets.max": 3,
  "limits.terminals.max": 10,
  "limits.mobile_devices.max": 2,
  "limits.promotion_rules.max": 0,
  "limits.offline_days.max": 0,
  "limits.exports.rows.max": 10000
}
```

## PRO features

PRO includes STANDARD plus:

```text
mobile.pos.basic
mobile.device.management
offline.sales.basic
offline.sync.review
offline.grant.basic
promotion.rules.basic
promotion.free_game
promotion.prize_multiplier
limitpolicy.advanced
payout.approval.workflow
reporting.dashboard.pro
reporting.export.excel
document.receipt.custom_template.basic
notification.email
audit.viewer
```

PRO limits:

```json
{
  "limits.users.max": 50,
  "limits.outlets.max": 10,
  "limits.terminals.max": 30,
  "limits.mobile_devices.max": 20,
  "limits.promotion_rules.max": 10,
  "limits.offline_days.max": 2,
  "limits.offline_tickets_per_device.max": 500,
  "limits.exports.rows.max": 100000
}
```

## DEMO features

DEMO is a sandbox plan. It should include PRO-level capabilities plus demo flags.

```text
demo.full_access
demo.seed_data
demo.external_delivery.mock
demo.expires
```

DEMO limits:

```json
{
  "limits.users.max": 20,
  "limits.outlets.max": 5,
  "limits.terminals.max": 10,
  "limits.mobile_devices.max": 10,
  "limits.promotion_rules.max": 20,
  "limits.offline_days.max": 3,
  "limits.offline_tickets_per_device.max": 200,
  "limits.exports.rows.max": 5000
}
```

DEMO must not bypass RLS, permissions, audit, idempotency, or domain invariants.

## Out of V1

Keep these keys documented but disabled/hidden for V1:

```text
tenant.theme.builder
tenant.theme.advanced_tokens
user.custom_roles
security.permission_customization
promotion.rule_editor
promotion.rules.advanced
promotion.segmented
promotion.rule_engine_ready
reporting.scheduled_exports
notification.sms
notification.push
integration.api.access
audit.export
billing.automated
terminal.fleet.advanced
offline.risk.controls.advanced
```

## Required fixes

### JSON parsing

`PlanMapper` must parse JSON strings into object `JsonNode`, not wrap strings as text nodes.

Target behavior:

```java
JsonNode limits = jsonUtils.readTreeOrEmptyObject(entity.getLimitsJson());
JsonNode features = jsonUtils.readTreeOrEmptyObject(entity.getFeaturesJson());
```

If helper names differ, create a small parsing helper in `catalog.plan.internal.mapper` or common JSON utils.

### Cache eviction

Admin writes may keep broad eviction for MVP, but after the entitlement cache exists they must also trigger eviction of affected entitlement snapshots.

MVP option:

- evict all plan caches;
- publish or call a platform-safe cache invalidation hook later;
- avoid direct dependency from `catalog.plan.internal` to `platform.entitlement.internal`.

### Validation

Admin write requests must validate:

- `code` required and normalized uppercase;
- `name` required;
- `featuresJson` valid JSON object;
- `limitsJson` valid JSON object;
- `billingPeriod` in allowed set;
- `currency` in allowed set for V1.

## PR checklist

- [ ] `PlanCatalog` remains read-only.
- [ ] No consumer imports `catalog.plan.internal`.
- [ ] JSON fields parse as JSON objects.
- [ ] V1 plans are seeded with cumulative features.
- [ ] Demo plan is sandbox-only and bounded.
- [ ] Plan caches are evicted after admin writes.
- [ ] Plan admin endpoints remain platform/SUPER_ADMIN scoped.
