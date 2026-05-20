# Proposal — Sales Sell with Operational Context

## Problem

Sales sell flow must validate seller operational context after the `platform` refactor and before offline sync promotion is allowed to create real tickets.

## Goals

- Add/standardize a seller operational context resolution step before sell.
- Keep sell business rules in `core.sales`.
- Integrate outlet/terminal/session validation cleanly through APIs/queries, not internals.
- Prepare offline sync to promote through the same sales path.

## Non-goals

- Do not put sales business logic in `platform.identity` or `platform.accesscontrol`.
- Do not make controllers resolve domain rules manually.
