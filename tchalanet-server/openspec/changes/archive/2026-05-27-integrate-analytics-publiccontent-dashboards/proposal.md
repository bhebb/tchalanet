# Change: integrate-analytics-publiccontent-dashboards

## Status

Proposed.

## Summary

Integrate `platform.publiccontent` and `core.analytics` into public home, tenant admin dashboard, platform admin/superadmin dashboard, cashier POS dashboard and web/mobile PageModel surfaces.

This change keeps features as BFF/PageModel consumers and avoids feature-to-feature dependencies.

## Why

The UI needs useful, fast and trustworthy dashboards:

- public home needs public content/news;
- cashier/POS needs immediate operational KPIs and actions;
- tenant admin needs business and operational KPIs;
- platform admin/superadmin needs platform health/onboarding/subscription/network content;
- reporting needs analytics-backed exports.

## Scope

### In scope

- Update PageModel dynamic providers to consume `platform.publiccontent.api` and `core.analytics.api.query`.
- Define V1 widgets and payloads for public, tenant admin, platform admin and cashier POS.
- Add placeholders only where source queries are not yet implemented, clearly marked.
- Add optional 7-day comparison for tenant admin.
- Keep user personalization/hide-public-content as V2.

### Out of scope V1

- Drag/drop dashboard personalization.
- Per-user hide/show content preferences.
- Advanced BI charts.
- Full notification center redesign.

## Dashboard decisions

### Cashier/POS dashboard V1

Purpose: immediate action.

Widgets:

```text
dashboard.cashier.identity
dashboard.cashier.session_summary
dashboard.cashier.draw_sales
dashboard.cashier.payout_attention
dashboard.cashier.alerts
dashboard.cashier.quick_actions
```

KPIs/actions:

- operational identity: seller, outlet, terminal, session, business date;
- session status;
- session ticket count;
- session sales total;
- cancelled tickets;
- sales by open draw;
- payable winning claims/amount;
- old unpaid claims attention;
- offline sync attention;
- actions: sell, verify, reprint/resend, payout, sync, close session.

### Tenant admin dashboard V1

Purpose: business/operational steering.

Widgets:

```text
dashboard.tenant_admin.header
dashboard.tenant_admin.business_kpis
dashboard.tenant_admin.operations
dashboard.tenant_admin.sales_by_outlet
dashboard.tenant_admin.sales_by_seller
dashboard.tenant_admin.draws
dashboard.tenant_admin.readiness
dashboard.tenant_admin.attention
dashboard.tenant_admin.public_content
dashboard.tenant_admin.quick_actions
```

KPIs:

- sales today;
- tickets today;
- calculated winnings;
- payouts paid;
- estimated net;
- paid-basis net;
- open sessions;
- top outlets;
- top sellers;
- pending result/settlement draws;
- readiness;
- operational attention.

Optional comparison:

- today vs yesterday;
- last 7 days vs previous 7 days.

### Platform admin / superadmin dashboard V1

Purpose: platform oversight.

Widgets:

```text
dashboard.superadmin.health
dashboard.superadmin.tenants
dashboard.superadmin.subscriptions
dashboard.superadmin.onboarding
dashboard.superadmin.alerts
dashboard.superadmin.public_content
dashboard.superadmin.quick_actions
```

KPIs:

- health snapshot;
- total tenants;
- active/suspended/onboarding tenants;
- action required;
- subscription stats;
- onboarding DRAFT tenants;
- platform public/internal content;
- ops alerts placeholder until notification/reconciliation integration.

### Public home V1

Uses `platform.publiccontent.api` for public home news/content.

Widgets:

```text
home.news
home.plans
public_hero via json_file
public_features via json_file
public_tchala via json_file
```

## Provider rules

- Providers MAY load grouped payload once per request using `PageModelResolutionContext`.
- Providers SHOULD dispatch by widgetId.
- Providers SHOULD lock `supports(...)` to the expected source and optionally logicalId to avoid accidental reuse.
- Dynamic errors should include logicalId, widgetId, widgetType and source.

## Rollout

1. Migrate public home from `features.news` to `platform.publiccontent.api`.
2. Migrate tenant/platform dashboard stats to `core.analytics` queries.
3. Add content widgets to tenant/platform dashboards using `platform.publiccontent.api`.
4. Add cashier POS analytics query and provider.
5. Remove old placeholders gradually as core queries become available.
