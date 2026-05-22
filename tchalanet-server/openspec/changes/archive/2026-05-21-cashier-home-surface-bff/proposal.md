# Proposal — Cashier Home Surfaces, Mobile POS Home BFF & PageModel Split

## Status

Proposed for implementation.

## Problem

The existing `private.dashboard.cashier` PageModel is useful for a web dashboard, but it is too long for a mobile/POS seller flow. It stacks many rows (`identity`, `overview`, `quick_sale`, `top_selections`, `recent_tickets`, `pending_approvals`, `next_draws`, `session`, `limits`) and naturally creates a long scrolling page.

For POS/mobile, the seller needs an action-first screen:

```text
Can I sell?
Which outlet/terminal/session?
Which draw is active?
[ Vendre un ticket ]
```

The POS home must not be a generic long dashboard.

## Decision

Split cashier landing into two surfaces:

```text
MOBILE_POS / POS cashier
  = short action-first Home BFF, not PageModel-driven

CASHIER_WEB
  = richer web dashboard, PageModel-compatible

TENANT_ADMIN_WEB
  = admin supervision dashboard, PageModel-compatible

PLATFORM_ADMIN_WEB
  = platform ops dashboard, PageModel-compatible
```

## Surface header

Clients SHOULD send a surface hint header:

```http
X-Tch-Surface: MOBILE_POS
```

Allowed values:

```text
MOBILE_POS
CASHIER_WEB
TENANT_ADMIN_WEB
PLATFORM_ADMIN_WEB
```

The header is a **presentation/context hint**, not an authorization source.

Rules:

- The backend MAY use `X-Tch-Surface` to choose compact vs web home response.
- The backend MUST validate the requested surface against profile/capabilities.
- The header MUST NOT grant access to a role/surface.
- If the header is absent, the backend infers the default from `profile.landing.preferredSurface`.
- If the header is unsupported for the user, return `403 surface.not_allowed` or downgrade only if explicitly documented.

## Natural login flow

```text
Login
  -> GET /tenant/me/profile
  -> route by preferredSurface and availableSurfaces

CASHIER / MOBILE_POS:
  -> GET /tenant/cashier/home
  -> if requiredStep=SELECT_OPERATIONAL_CONTEXT: show setup
  -> if requiredStep=OPEN_SESSION: show open session
  -> else show short POS home

CASHIER_WEB:
  -> GET /tenant/cashier/web-home
  -> optional PageModel `private.dashboard.cashier.web`

TENANT_ADMIN:
  -> GET /admin/home

SUPER_ADMIN:
  -> GET /platform/home
```

## Non-goals

- Replacing all PageModel dashboards.
- Making PageModel the runtime source of truth for POS status.
- Moving operational business decisions into PageModel.
- Showing pending approval workflow in mobile POS V1.
