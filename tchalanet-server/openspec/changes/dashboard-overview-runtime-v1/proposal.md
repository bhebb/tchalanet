# Change: dashboard-overview-runtime-v1

## Status

Merged Draft V1 — ready for implementation planning and agent challenge.

## Why

Tchalanet has several UI surfaces and roles:

- anonymous public visitors;
- cashier POS/mobile;
- cashier web;
- tenant admin;
- platform / super admin.

The PageModel system is a mini-CMS:

```text
catalog.pagemodeltemplate
  = versioned reference structure

core.pagemodel
  = tenant materialized instance, draft/publish/merge lifecycle
```

The `model` JSON in a PageModel instance is structure-only:

- shell;
- layout;
- widget slots;
- binding sources;
- static props.

It never contains volatile runtime data.

Without strict runtime rules, the system becomes heavy and ambiguous:

- one dynamic provider per widget;
- one DB/query call per widget;
- dashboards that preload all management pages;
- feature controllers that duplicate core/catalog/platform ownership;
- unclear dashboard vs overview boundaries;
- silent empty widgets when a `widgetId` drifts from provider code.

This change freezes V1 runtime rules.

## Core decisions

1. The template -> instance mini-CMS model is already acquired. This change consolidates runtime providers and page boundaries; it does not redesign the CMS itself.
2. Dashboard = PageModel runtime. It displays KPI, alerts, notifications, short readiness, short summaries and quick actions.
3. Overview is not a dashboard. It is a feature endpoint for structural diagnosis and navigation.
4. Overview never repeats dashboard KPI.
5. Management pages are owned by core/catalog/platform and are loaded only on click.
6. Providers are grouped by source. No one-provider-per-widget pattern.
7. POS/mobile cashier remains a compact endpoint outside full PageModel dashboard.
8. Readiness is computed once and exposed through different projections: dashboard summary, overview view, provisioning result.
9. PageModel widget coupling is protected by a `widgetId` registry scoped by `schema_version`.
10. Provider failures are visible in `dynamic.errors`; no silent `catch -> empty`.

## Allowed provider sources V1

```text
json_file
public_home
public_draw_results
tenant_admin_dashboard
cashier_dashboard
platform_admin_dashboard
```

## Prerequisites

No implementation of downstream runtime specs should start before these are completed:

- re-seed `PageModelTemplate` documents with consolidated sources;
- keep existing `widgetId` values stable;
- create `widgetId` registry by `schema_version`;
- promote public PageModel API/read models needed by features;
- remove `core.pagemodel.internal.*` imports from feature code;
- add ArchUnit guard against `core.pagemodel.internal..` imports outside `core.pagemodel`.

## Scope

In scope:

- PageModel runtime provider grouping;
- static `json_file` fragments for shell/navigation/actions;
- public home and public draw results runtime;
- tenant admin dashboard runtime source;
- tenant overview endpoint;
- cashier web dashboard runtime source;
- cashier POS/mobile compact home rule;
- platform admin dashboard runtime source;
- platform overview endpoint;
- widget registry;
- shared tenant readiness;
- tenant provisioning V1 framing.

Out of scope:

- complete frontend implementation;
- full report engine;
- complete tenant provisioning implementation;
- `CUSTOM_FROM_TENANT` clone flow;
- materialized dashboard snapshots;
- replacing existing owner CRUD controllers;
- redesigning the PageModel CMS lifecycle.

## Expected result

A stable first UI/runtime layer:

- public home/results/check-ticket flow;
- post-login dashboards by role/surface;
- clear overview pages;
- clear ownership of management pages;
- E2E-testable navigation and dashboard payloads;
- fewer providers and fewer backend grouped reads.
