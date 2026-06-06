# PageModel Convention — Tchalanet

> **Status**: DRAFT v0.3
> **Scope**: dynamic page runtime payloads, layout renderer, widget renderer, widget actions
> **Living doc** — update in the same commit as any code that changes a rule here.

---

## 1. Rule

PageModel describes **page content structure**.

It does not carry:

* i18n bundles;
* full theme definitions;
* runtime settings payloads;
* authorization rules;
* backend binding details;
* frontend service orchestration.

Runtime bootstrap may load PageModel alongside settings, i18n and theme as separate concerns.

The web consumes only the **resolved `PageRuntimeResponse`** returned by the backend PageModel runtime API. The frontend must not mirror internal catalog/template definitions and must not resolve backend bindings such as `fileKey` or `jsonFile`.

---

## 2. Mental model

```text
PageRuntimeResponse
  meta
  theme hint
  shell
  content
  dynamic
```

Responsibility split:

```text
Shell       = page chrome: header/footer/top bar/sidenav
PageModel   = content layout: rows/columns/widget ids
WidgetHost  = widget id -> widget config + dynamic payload
Widget      = render known props/data
```

PageModel must stay a composition contract, not a CMS.

---

## 3. Runtime backend contract

Source of truth: backend `PageRuntimeResponse`.

Target shape:

```text
PageRuntimeResponse {
  meta {
    logicalId
    scope
    slug
    schemaVersion
  }

  theme {
    presetId
    mode
    density
  } // optional page hint; ThemeApi/bootstrap is authoritative

  shell {
    type
    header/footer OR topAppBar/navigationDrawer
  }

  content {
    layout {
      rows [
        {
          id
          labelKey
          columns [
            {
              span
              widgets: string[]
            }
          ]
        }
      ]
    }

    widgets Map<widgetId, WidgetConfig>
  }

  dynamic {
    widgets Map<widgetId, payload>
    errors WidgetDynamicError[]
  }
}
```

Critical shape facts:

* Layout references widget **ids** as strings.
* Widget definitions live in `content.widgets`, keyed by widget id.
* Runtime widget payloads live in `dynamic.widgets`, keyed by widget id.
* Widget-local dynamic failures live in `dynamic.errors`.
* Request-level `notices` and `services` belong to the `ApiResponse` envelope, not to PageModel.
* Dashboard-specific responses may add dashboard-level fields such as `notifications`, but the renderer must keep `content` and `dynamic` semantics unchanged.

---

## 4. Backend-only details

These are backend/internal details and must not be required to render Angular runtime pages:

```text
binding
provider source
jsonFile
fileKey
template metadata
storage details
catalog/template definition shape
```

The database may store:

```json
{
  "binding": {
    "mode": "dynamic",
    "source": "jsonFile"
  },
  "props": {
    "fileKey": "public_footer_links"
  }
}
```

But the frontend runtime response should contain the resolved fragment:

```json
{
  "shell": {
    "type": "public",
    "footer": {
      "brand": {},
      "descriptionKey": "public.footer.description",
      "columns": [],
      "social": []
    }
  }
}
```

Rule:

```text
DB/template model may be flexible.
Frontend runtime model must be resolved and ready to render.
```

---

## 5. Shell boundary

PageModel does not own the shell.

Public runtime shell:

```json
{
  "type": "public",
  "header": {
    "brand": {},
    "primary": [],
    "utilities": [],
    "actions": []
  },
  "footer": {
    "brand": {},
    "descriptionKey": "public.footer.description",
    "statusKey": "public.footer.status.operational",
    "copyrightKey": "app.footer.copyright",
    "columns": [],
    "social": []
  }
}
```

Private runtime shell:

```json
{
  "type": "private",
  "topAppBar": {},
  "navigationDrawer": {
    "brand": {},
    "primary": [],
    "sections": [],
    "secondary": []
  }
}
```

Non-negotiable:

```text
Do not put PrivateShell in shell.header.
Private sidenav comes from shell.navigationDrawer.
Public shell comes from shell.header + shell.footer.
```

---

## 6. Layout rules

V1 layout is intentionally small:

```text
rows
columns
span
widgets
```

Allowed:

```json
{
  "id": "hero",
  "labelKey": "layout.publicHome.hero",
  "columns": [
    {
      "span": 12,
      "widgets": ["home.hero"]
    }
  ]
}
```

Avoid in V1:

* nested layouts;
* arbitrary CSS grid config from backend;
* backend-controlled classes;
* frontend conditions embedded in layout;
* responsive behavior controlled by backend beyond simple composition.

The frontend owns responsive CSS.

---

## 7. Widget registry

Map backend `type` strings directly to Angular components through the registry.

Example backend types:

```text
HeroWidget
NewsTickerWidget
PlansWidget
FeatureGridWidget
PublicDrawResultsWidget
KpiGridWidget
AlertsWidget
QuickActionsWidget
```

Rule:

```text
Registry key = backend type string.
No normalization layer.
```

A widget component receives only:

```text
widget id
WidgetConfig { type, props }
dynamic.widgets[id]
local error for that id
```

A widget must not receive the full page object.

---

## 8. Supported widgets

Supported widgets are defined by the active frontend registry.

A slice may add one widget at a time.

Unknown backend widget types must render the unsupported-widget fallback and must not break the page.

Current public slices may support only a subset such as:

```text
HeroWidget
NewsTickerWidget
FeatureGridWidget
PlansWidget
```

Dashboard slices may add:

```text
KpiGridWidget
AlertsWidget
ReadinessSummaryWidget
QuickActionsWidget
```

The registry is the source of truth for what renders today.

---

## 9. Fallback behavior

The renderer must handle:

```text
Unsupported widget type
Invalid widget config
Missing widget config
Widget-local dynamic error
Missing dynamic payload
```

Rules:

* unsupported type → contained unsupported-widget fallback;
* invalid widget → contained invalid-widget fallback;
* widget-local dynamic error → error rendered inside that widget/card/section;
* one widget failure must never blank the page;
* app shell should remain usable when possible.

---

## 10. Text and i18n

Every user-facing string should be represented as an i18n key from the start.

PageModel carries key fields such as:

```text
titleKey
labelKey
descriptionKey
subtitleKey
ctaKey
reasonKey
```

Do not use snake_case fields in runtime JSON:

```text
title_key
label_key
description_key
```

i18n key values may still contain underscores:

```text
public.nav.check_ticket
home.check_ticket.title
dashboard.tenant_admin.kpis.title
```

Missing translations should render a stable fallback and keep the widget visible.

---

## 11. Theme

Widgets style through validated CSS variables.

Use:

```text
--tch-*
--comp-* with --tch-* fallback
```

The runtime `theme` object inside PageModel is only a page hint/fallback.

ThemeApi/theme bootstrap remains the source of truth that materializes tokens.

Do not:

* embed full theme objects inside PageModel;
* hardcode colors in widgets;
* import legacy preset palettes into widgets;
* make widget rendering depend on tenant-specific CSS logic.

A small `theme.presetId` hint is tolerated during migration, but it is not authoritative.

---

## 12. Feature flags and gating

A PageModel may reference a feature flag key, but never the current flag value.

Frontend runtime decides rendering using the owning feature-flag mechanism.

Paid/entitlement gating is separate from feature flags.

Do not let PageModel decide auth.

Authorization remains owned by:

* backend permissions;
* API guards;
* route guards;
* feature-specific access policies.

---

## 13. Actions

Actions are descriptors.

`ActionItem.kind` describes presentation/behavior:

```text
button
link
externalLink
```

`NavigationDestination.kind` describes target type:

```text
route
url
```

Example:

```json
{
  "id": "checkTicket",
  "kind": "button",
  "labelKey": "public.nav.check_ticket",
  "destination": {
    "kind": "route",
    "value": "/public/check-ticket"
  },
  "icon": "qr_code_scanner",
  "activeMatch": null,
  "disabled": false,
  "reasonKey": null
}
```

Sensitive mutations are never executed by the generic PageModel renderer.

Sensitive actions must go through explicit known handlers or owning endpoints.

Unknown action types render disabled or are omitted per widget contract.

Domain note:

```text
"onboard seller" = create a CASHIER user.
role = CASHIER.
outletId is required.
It is not a SELLER role.
```

---

## 14. Failure behavior by surface

Public routes:

```text
show fallback page or public error panel
keep public shell usable when possible
```

Private routes:

```text
show recoverable loading/error state
keep private shell/nav usable when possible
```

Widgets:

```text
contained error only
never blank the page
```

---

## 15. Anti-patterns

Do not:

* invent an abstract widget vocabulary that diverges from backend `type` strings;
* pass the full page object into widgets;
* let arbitrary backend widget types instantiate Angular classes outside the registry;
* put business mutations in generic widget renderers;
* let PageModel decide auth;
* carry translation maps in PageModel;
* carry full theme tokens in PageModel;
* carry settings values in PageModel;
* expose `fileKey/jsonFile/binding` as required frontend runtime data;
* create a widget library before a real page needs the widget;
* make layout a backend-controlled CSS engine.

---

## 16. PR checklist

Before merging PageModel changes:

* [ ] Runtime JSON fields are camelCase.
* [ ] Backend-only bindings are resolved before frontend rendering.
* [ ] Shell is separate from content layout.
* [ ] Private shell uses `navigationDrawer`.
* [ ] Public shell uses `header/footer`.
* [ ] Widgets are referenced by id in layout.
* [ ] Widget definitions live in `content.widgets`.
* [ ] Dynamic payloads live in `dynamic.widgets`.
* [ ] Widget receives only its config/data/error.
* [ ] Unsupported widget fallback exists.
* [ ] A widget failure cannot blank the page.
* [ ] No auth/theme/i18n/settings payload is embedded in PageModel.
