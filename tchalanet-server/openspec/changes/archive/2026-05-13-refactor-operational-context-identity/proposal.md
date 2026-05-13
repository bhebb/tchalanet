# Proposal — Refactor Operational Context and Identity after Platform migration

## Problem

The previous `usercontext` wording is no longer correct after introducing `platform` and migrating user-related modules. It risks confusion with `TchRequestContext`, request context, actor context, and operational context.

We need explicit boundaries for:

- runtime request context;
- persistent identity/profile/membership;
- access-control decisions;
- operational context resolution for seller/admin workflows.

## Goals

- Replace the former user-context platform concept with `platform.identity`.
- Keep request/runtime context in `common.context` and web argument resolver wiring in `common.context.web`.
- Define operational context as a runtime/application resolution concern.
- Prepare POS/seller flows to validate terminal/outlet/session before sales/offline sync actions.

## Non-goals

- Do not redesign Keycloak itself.
- Do not put operational context into persistence by default.
- Do not move business rules from `core.sales`, `core.outlet`, `core.terminal`, or `core.session` into `platform.identity`.
