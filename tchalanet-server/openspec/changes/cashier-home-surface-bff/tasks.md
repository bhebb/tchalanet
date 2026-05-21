# Tasks — Cashier Home Surface BFF

## 1. Surface contract

- [x] Add `ClientSurface` enum:
  - `MOBILE_POS`
  - `CASHIER_WEB`
  - `TENANT_ADMIN_WEB`
  - `PLATFORM_ADMIN_WEB`
- [x] Add `X-Tch-Surface` header resolver.
- [x] Validate requested surface against profile `availableSurfaces`.
- [x] If header missing, use `preferredSurface`.
- [x] Return clean error `surface.not_allowed` if user requests an unavailable surface.

## 2. Profile

- [x] Add/confirm endpoint `GET /tenant/me/profile`.
- [x] Return user identity, tenant summary, locale, timezone.
- [x] Return `landing.preferredSurface`.
- [x] Return `landing.availableSurfaces`.
- [x] Return capabilities.
- [x] Return profile edit actions.

## 3. Mobile POS home

- [x] Add `GET /tenant/cashier/home`.
- [x] Return compact response for `MOBILE_POS`.
- [x] Include `requiredStep`:
  - `SELECT_OPERATIONAL_CONTEXT`
  - `OPEN_SESSION`
  - `null`
- [x] Include trusted operational context state.
- [x] Include session summary.
- [x] Include primary/open draw summary.
- [x] Include one primary action: `SELL_TICKET`.
- [x] Include max 3–4 quick actions.
- [x] Do not include long scrolling widget list.

## 4. Web cashier home

- [x] Add `GET /tenant/cashier/web-home`.
- [x] Return richer `widgets` list.
- [x] Keep primary action visible at top.
- [x] Include recent tickets, session, active draw.
- [x] Do not expose pending approval workflow in cashier POS V1.

## 5. PageModel reposition

- [x] Rename/copy existing `private.dashboard.cashier` to `private.dashboard.cashier.web`.
- [x] Remove `pending_approvals` from cashier POS.
- [x] Make `top_selections` web-only.
- [x] Move `limits` to tenant admin or secondary web area.
- [x] Keep PageModel as layout/config only, not runtime source of POS truth.

## 6. Tests

- [x] `cashier_home_mobile_ready_returns_sell_action`
- [x] `cashier_home_mobile_missing_context_returns_select_context_required_step`
- [x] `cashier_home_mobile_closed_session_returns_open_session_required_step`
- [x] `cashier_home_mobile_does_not_return_long_dashboard`
- [x] `cashier_web_home_returns_widgets`
- [x] `surface_header_not_allowed_returns_403`
- [x] `missing_surface_header_uses_preferred_surface`
- [x] `profile_returns_available_surfaces`
