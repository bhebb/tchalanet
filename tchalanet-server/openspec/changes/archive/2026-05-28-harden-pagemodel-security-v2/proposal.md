# OpenSpec Change — harden-pagemodel-security-v2

## Status
Draft / ready for implementation

## Scope
This change hardens `features.pagemodel` around four decisions:

1. Public PageModels must be truly public.
2. Private PageModel endpoints must be secured server-side.
3. Cashier is an operational private surface, not an admin surface.
4. PageModel contracts must be predictable and typed across public, cashier, tenant admin, and superadmin.

## Problem

The current PageModel approach can expose too much flexibility to the client:

- A private caller may request a PageModel intended for another role/surface.
- Dynamic JSON fragments may produce inconsistent shapes.
- Free `Map<String, Object>` payloads make Angular integration fragile.
- Header/sidenav/footer fragments are missing or inconsistent.
- Private top app bar and navigation drawer may duplicate navigation.

## Decisions

### D1 — Public must be truly public

Public PageModels must expose only public-safe content. Public providers must be explicitly safe for anonymous/public access. Public content must not include admin hints, tenant admin routes, superadmin routes, operational context, private notifications, or sensitive readiness information.

### D2 — Private endpoint must resolve server-side

For private pages, the client must not freely choose a private PageModel id/context/file_key for admin or superadmin.

The client asks for an intention, for example:

```http
GET /private/page-model/dashboard
```

The server resolves the concrete PageModel from `TchRequestContext`:

```text
SUPER_ADMIN  -> private.dashboard.superadmin
TENANT_ADMIN -> private.dashboard.tenant_admin
CASHIER      -> private.dashboard.cashier.web
```

Unauthorized access returns 403. There must be no silent fallback.

### D3 — Cashier is operational, not admin

Cashier is a private operational surface for selling, verifying, printing, payout flow entry, session work, and operational readiness.

Cashier must not receive tenant admin navigation, superadmin navigation, tenant configuration controls, platform operations, or unrestricted management endpoints.

### D4 — Predictable PageModel contract

PageModel is dynamic by composition, not dynamic by shape.

All four surfaces must share stable typed models for:

- images
- brand
- destinations
- actions
- badges
- alerts
- top app bar
- navigation drawer
- footer
- hero
- widgets

Free maps are forbidden in the main rendering contract. Free-form maps are allowed only in `meta` or `ext`, which the frontend may ignore.

### D5 — Private top app bar does not duplicate navigation

On private surfaces, the navigation drawer is the single source of truth for main navigation.

Private top app bar is light:

- left: menu toggle and optional page title/context
- right: notifications, language selector, light/dark toggle, avatar/profile menu
- search can be added later
- no main navigation destinations in top app bar

The logo and main app/tenant name live at the top of the navigation drawer.

## Target package impact

```text
features.pagemodel
  public/
  private/
  security/
  contract/
  providers/

platform.publiccontent
  api/                     # public/internal news/content source

frontend Angular
  PageModel renderer
  Shell renderer
  widget contracts
```

## Non-goals

- Build a full CMS.
- Allow arbitrary widget payloads.
- Let frontend choose private role/surface.
- Add global search V1.
- Add deep personalization of drawer visibility V1.

## Migration notes

- Existing `header`, `sidenav`, `footer` fields should migrate toward explicit `topAppBar`, `navigationDrawer`, and `footer` for private shell.
- Public shell keeps `header` + `footer`.
- Existing `primary` in sidenav should migrate to `topDestinations`.
- Existing `secondary` in sidenav should migrate to `footerDestinations`.
