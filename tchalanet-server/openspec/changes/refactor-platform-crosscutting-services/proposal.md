# Change Proposal — Consolidate Platform Cross-cutting Services

## Change ID

`refactor-platform-crosscutting-services`

## Summary

Consolidate the already-existing platform cross-cutting capabilities and add guardrails where the
risk is real:

- `platform.accesscontrol` decides whether an actor may attempt an action.
- `platform.audit` records functional audit while Envers remains technical revision audit.
- `platform.idempotence` protects HTTP client retries and event consumer replay.

This change is not a large package extraction anymore. The packages already exist after the platform
modulith work. The goal is to align behavior, documentation, tests, migrations and architecture
rules so these services stay secure and predictable.

## Why

These capabilities sit on sensitive boundaries:

- Access control mistakes become privilege escalation or tenant data exposure.
- Audit confusion can produce duplicate logs or miss the actual business action.
- Idempotency bugs can create duplicate tickets, payouts, notifications or event projections.

They also look similar structurally, which makes it easy to overgeneralize them. They must share
platform conventions without becoming one generic framework.

## What Changes

- Reframe the OpenSpec around consolidation and security guardrails.
- Create canonical specs for access control, audit and idempotence.
- Ensure docs distinguish:
  - permission checks vs business invariants;
  - functional audit vs Envers/technical revision audit;
  - HTTP idempotency vs processed-event idempotence.
- Add or reinforce architecture rules for:
  - `platform.*.internal` privacy;
  - no business domain logic in access control;
  - no functional audit persistence in `common`;
  - no idempotency persistence in `common`.
- Add focused tests for critical behavior instead of broad rewrites.
- Update tasks to track only real remaining gaps.

## Out of Scope

- Rewriting all controllers and handlers.
- Creating a generic `platform.crosscutting` module.
- Moving technical JPA base classes out of `common`.
- Replacing domain state transitions, locks or constraints with idempotency.
- Using access control as resource validity/business eligibility logic.
- Treating Envers as a substitute for functional audit.

## Impact

- `tchalanet-platform`: docs, tests, small implementation fixes if gaps are found.
- `tchalanet-app`: ArchUnit rules and Flyway alignment checks.
- `tchalanet-common`: boundary documentation only, unless misplaced persistence is discovered.
- `tchalanet-core` / `tchalanet-features`: consume public platform APIs only.

## Success Criteria

- The three canonical specs exist and match current architecture.
- All sensitive endpoints/use cases have an explicit owner for permission, audit and idempotency.
- Audit writes do not rollback successful business transactions.
- Duplicate event delivery is safely skipped through stable handler keys.
- HTTP idempotency rejects missing keys, payload mismatch and in-progress duplicates where required.
- Architecture tests fail on internal package leaks and misplaced cross-cutting persistence.
