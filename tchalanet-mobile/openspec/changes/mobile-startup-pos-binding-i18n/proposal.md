# Change: Mobile startup, POS binding, and i18n guardrails

## Status
Proposed

## Context
Tchalanet is starting the Flutter Android app for both handheld Android POS devices and Android phones. The same application must support:

- POS handheld transactional usage;
- Android phone seller usage;
- tenant admin bootstrap and operational selection flows;
- device/terminal binding security;
- backend-driven texts for tickets, PDF/print, PageModel, actions and blockers;
- local Flutter i18n for stable UI labels.

This change formalizes the initial mobile guardrails so implementation does not drift.

## Problem
Without clear rules, the app risks:

- deciding POS/mobile mode only from screen pixels;
- allowing a seller to claim or change terminal/outlet/session from the client;
- relying on `profile/current` or UI flow as security;
- mixing Haitian Creole, French, and English texts in POS, web, ticket PDF, and printed receipts;
- duplicating home/profile/security logic in multiple endpoints;
- letting `cashier/home` become the security bootstrap endpoint;
- letting backend actions return untranslated labels that clients cannot resolve consistently.

## Goals

1. Define Flutter runtime surface guardrails for POS vs mobile.
2. Define the post-login bootstrap flow using `profile/me/current` and `cashier/home`.
3. Define device enrollment, terminal binding, operational context, and backend security checks.
4. Define mobile/backend i18n responsibilities and ticket/PDF/print text resolution.
5. Define test expectations for direct backend calls that bypass the mobile UI.

## Non-goals

- Implement Flutter UI widgets.
- Implement Keycloak login/theme changes.
- Redesign catalog.i18n persistence from scratch.
- Define every ticket game rule.
- Define complete PageModel rendering.

## Design summary

### Endpoint roles

- `GET /tenant/profile/me/current` is the bootstrap endpoint after login.
  - It returns identity, tenant, roles/permissions, subscription/capabilities summary, device state, operational context, startup state, blockers, and allowed next actions.
  - It may return `200` with blockers for diagnostic/bootstrap states.

- `GET /tenant/cashier/home` is an écran/data endpoint.
  - It is called only when `profile/current` indicates readiness for cashier mode.
  - It returns cashier home content, not the canonical security state.

### Seller actions

Seller ticket selling has two major actions:

- `PREVIEW_TICKET`: validates and prices a local draft before final sale.
- `SELL_TICKET`: creates the ticket, is idempotent, and revalidates all critical checks even if preview was skipped.

### Surface model

Flutter must use both:

- MediaQuery/LayoutBuilder/SafeArea for screen constraints;
- `TchRuntimeProfile.surface` for the business surface: `POS_HANDHELD`, `MOBILE`, `TABLET`.

POS is a business/device mode, not only a width breakpoint.

### Device binding

For POS mode, the user can authenticate with Keycloak, but cannot enter cashier mode or sell unless the device is enrolled, active, signed, and bound to a valid terminal/outlet.

### I18n model

- Flutter ARB handles stable local UI texts.
- Backend `catalog.i18n` handles backend/system/document texts with scope-aware resolution.
- Ticket/PDF/print texts are resolved by backend, not by Flutter.
- For sold tickets, critical labels are snapshotted for reprint/audit consistency.

## Affected modules

- `tchalanet-mobile`
- `features/profile/current`
- `features/cashier/home`
- `features/seller/ticket`
- `platform.identity`
- `platform.accesscontrol`
- `catalog.i18n`
- `platform.document`
- `core.terminal`
- `core.outlet`
- `core.session`
- `core.sales`
- `core.subscription` / `core.entitlement`

## Rollout

1. Add specs and guardrail tests.
2. Introduce `TchRuntimeProfile` in Flutter.
3. Move/enrich `profile/me/current` as BFF bootstrap if currently too deep in `platform.identity`.
4. Add device binding checks to context resolution and critical handlers.
5. Add `PREVIEW_TICKET` and reinforce `SELL_TICKET`.
6. Add backend i18n dictionary/bundle resolution for mobile and ticket documents.
