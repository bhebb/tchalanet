# Tasks — test-strategy-separation-v1

This change is a contract. Tasks = adopt it + align the four existing changes so
nothing is tested twice.

## 1. Publish the contract

- [ ] Land `proposal.md` + `design.md` (boundary rule + the three lists).
- [ ] Add a short "Test layers" section to `tchalanet-server/docs/conventions/testing.md`
      pointing to this change's boundary rule (§0) and checklist (§5).
- [ ] Reference this change from each of the four owner changes' proposals.

## 2. Align UNIT (`unit-coverage-critical-domains-v1`)

- [ ] Confirm the Unit list (design §1) matches the change's tasks A–F.
- [ ] Ensure no plumbing classes are in scope (evaluators/appliers/calculators only).

## 3. Align INTEGRATION (`spring-integration-business-flows-v1`)

- [ ] Reduce limit-blocked + maryaj tests to **one wired composition** each
      (per-rule permutations now belong to Unit).
- [ ] Confirm remaining integration flows match design §2 (persistence / RLS /
      idempotency / contract), one representative each.
- [ ] Add the missing draw-result-apply idempotent integration flow (§2) if absent.

## 4. Align E2E (`e2e-business-runtime-v1`)

- [ ] Confirm E2E asserts scenarios, not ProblemDetail shapes or idempotency-store
      internals (those are Integration).
- [ ] Keep the sell matrix out of E2E — one happy path + idempotent replay only.

## 5. Align LOAD (`perf-load-testing-locust-v1`)

- [ ] Confirm Locust carries **no** business assertions (capacity/latency only).

## 6. Web (out of pyramid, tracked separately)

- [ ] Note that `web-e2e` Playwright critical flows are specced in a dedicated
      web change (`web-e2e-critical-flows-v1`), not here.

## 7. Verify no duplication

- [ ] For each domain in design §2, confirm the same assertion is not repeated in
      Unit and Integration and E2E — apply the §5 checklist, record exceptions.
