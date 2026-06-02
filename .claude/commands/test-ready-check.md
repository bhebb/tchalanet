# /test-ready-check

Review whether the current change has the right test coverage before PR.

## Required review sections

1. Risk summary
2. Test levels selected
3. Unit tests added/updated
4. Integration tests added/updated
5. E2E tests added/updated or explicitly deferred
6. Security/RLS/idempotency coverage
7. POS operational context coverage if relevant
8. Frontend/mobile contract coverage if relevant
9. Commands executed
10. Remaining gaps and whether they block PR

## Blocking gaps

Mark as blocking when:

- money/ticket/payout/settlement logic changed without unit tests;
- controller security/validation changed without web/security test;
- RLS-sensitive persistence changed without integration test;
- idempotent endpoint changed without replay/mismatch tests;
- POS sell/payout/offline flow changed without trusted operational context test;
- frontend/mobile API contract changed without consumer test or fixture update;
- no verification command was run.

## Output format

Use a concise checklist with PASS/WARN/BLOCK.
