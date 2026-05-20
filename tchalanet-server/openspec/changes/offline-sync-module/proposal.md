# Proposal — Offline Sync Module

## Problem

Offline sales must be accepted from unreliable POS environments without polluting core sales with technical sync concerns. The previous design needs to be re-aligned with the new `platform` layer and operational-context rules.

## Goals

- Create/standardize `core.offlinesync` as the owner of offline submission lifecycle.
- Keep technical validation in offline sync: signatures, grants, device/session envelope, idempotency keys, sequence, payload hash.
- Keep sales business validation in `core.sales`.
- Use operational context resolution before promoting an offline submission into a real sale/ticket.
- Provide explicit specs per controller/handler.

## Non-goals

- Do not allow offline sync to write sales tables directly without going through sales commands/API.
- Do not move ticket pricing, draw cutoff, limits, or payout rules into offline sync.
- Do not use platform notification/communication for core decision making.
