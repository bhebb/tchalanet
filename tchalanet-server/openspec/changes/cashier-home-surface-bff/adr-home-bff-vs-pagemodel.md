# ADR - Cashier Home BFF vs PageModel

## Status

Proposed for `cashier-home-surface-bff`.

## Context

The existing `private.dashboard.cashier` PageModel is a good fit for a rich web dashboard, but it is too broad for a mobile/POS landing screen. It contains long dashboard rows such as identity, overview, quick sale, top selections, recent tickets, pending approvals, next draws, session, and limits.

A mobile cashier home needs dynamic operational truth instead:

- is the POS context trusted?
- is an outlet and terminal selected?
- is the cashier session open?
- which draw can be sold now?
- what is the next action?

Those answers must come from backend feature/core queries at request time, not from static layout configuration.

## Decision

Use two mechanisms with separate responsibilities:

- `GET /tenant/cashier/home` is the mobile/POS Home BFF. It returns compact, action-first runtime state for `MOBILE_POS`.
- `GET /tenant/cashier/web-home` is the richer cashier web home for `CASHIER_WEB`.
- PageModel remains layout/configuration for web dashboards and is not the runtime source of POS readiness.
- The existing cashier dashboard is copied/repositioned as `private.dashboard.cashier.web`.

Clients may send `X-Tch-Surface`, but the backend validates the requested surface against the authenticated user's available surfaces. The header is a presentation hint, not an authorization source.

## Consequences

- Mobile POS can render an action-first screen without scrolling through a generic dashboard.
- Pending approvals and limits are not exposed on mobile POS home V1.
- PageModel remains useful for web composition without owning operational decisions.
- Feature code must compose public core/platform/catalog APIs only; it must not import core internals or repositories.
