# Test Plan — Critical Flows (write-before-coding)

> **Status**: WORKING PLAN (companion to `testing.md`)
> **Rule**: we do **not** write tests for coverage. We understand the code, list
> the cases that matter, prove them at the right layer (`testing.md` §0), and
> **every incoherence found while writing a test is logged in §5, not papered
> over.** A test that has to fight the code is a finding.

## 1. Working method (per area, before writing anything)

1. **Read the code** until you can state, in one sentence, what breaks if it's wrong.
2. **List the cases** — the branches that carry risk (below/at/above, present/absent,
   allowed/illegal transition), not one-happy-path-per-method.
3. **Pick the layer** per the weight table (`testing.md` §0). Permutations → Unit;
   one wired composition → Integration; whole journey → E2E.
4. **Write the case.** If the code makes the case awkward, ambiguous, or impossible
   to assert cleanly → **stop and log an incoherence (§5)**. Do not weaken the test
   to make it pass.
5. **Never** re-assert what a lower layer already proved.

## 2. Priority areas & cases (unit-first)

Levels reference `testing.md`. Only the risk-carrying cases are listed.

### Limits (`core/limitpolicy`) — Unit-heavy
- Engine: no rules → allow; missing evaluator → fail-fast; severity ranking
  WARN < REQUIRE_APPROVAL < BLOCK; null-breach safety.
- Each evaluator: below / at-boundary / above; multi-line aggregation; zero prior.
- **Integration (1)**: configured limit blocks sell → **no ticket persisted**.

### Sales decision & charge (`core/sales`) — Unit core, IT+E2E decisive
- `SaleAcceptanceEvaluator`: accept / block / warn. `SaleChargeCalculator`: with/
  without promotion. `PosSaleContextResolver`: context edge cases.
- **Integration (1)**: prepare→confirm pipeline + idempotency-store + persistence.

### Pricing (`core/pricing`) — Unit for resolution, IT decisive (money+cache)
- Odds/payout resolution + **override precedence** (terminal vs promo boost;
  active/inactive/expired).
- **Integration (1)**: rule upsert → money snapshot persisted + **cache
  invalidated** + RLS; Ensure-Haiti-rules seeds expected rules.

### Promotion / Maryaj gratis (`core/sales/.../promotion`) — Unit matrix, IT decisive
- Effect appliers (charge waived/unchanged; odds boosted/base; snapshot; hash
  stable/differing) — the {maryaj × limit × override} matrix lives here.
- **Integration (1)**: active campaign → promotional line persisted (+ auto-select TTL).

### Draw lifecycle + result/settlement → see §3 (job plumbing).

### Base framework classes (`tchalanet-common`, `app/config`) — see `testing.md` §2
- Bus dispatch branches (unit) + registration completeness (1 `@SpringBootTest`).
- `CombinedCache` two-tier logic (unit, mocked local/remote) + 1 `@Cacheable` IT.
- Domain event publish (unit) + 1 delivery IT.

### Filters (`TchContextFilter`) — see `testing.md` §1b
- Branch cases as unit (override-403, act-as-terminal gate, malformed UUID, null
  ctx, finally-clear) + 1 IT for chain order + RLS activation.

### Auth flow — Unit for pure policies, IT decisive (Spring Security chain)
- Scope/surface/actor/verification policies (unit).
- **Integration (1)**: JWT `iss`/`aud` validation + scope routing + provider select.

## 3. Job plumbing — `generate → open → close → apply → settle`

The draw lifecycle, driven by Spring Batch jobs. Canonical states
(`DrawStatus`): `SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED` (+ `CANCELED`,
`ARCHIVED`). Two concerns to test separately: **the state logic** (unit) and **the
job plumbing that drives it** (integration).

### 3a. State logic — Unit

- **`DrawStatusTransition`** (state machine) — assert the full legal matrix and
  that every illegal edge throws:
  - legal: SCHEDULED→OPEN/CANCELED · OPEN→CLOSED/CANCELED · CLOSED→RESULTED/CANCELED
    · RESULTED→SETTLED · SETTLED→ARCHIVED · CANCELED→ARCHIVED · ARCHIVED→∅
  - illegal (sample): OPEN→SETTLED, CLOSED→OPEN, RESULTED→CANCELED, SETTLED→RESULTED,
    ARCHIVED→anything; null from/to → NPE.
- **`DrawLifecycleCommandGuard`** — each lifecycle command allowed only from its
  legal source state; wrong state → rejected (not silently no-op).
- **Schedule/due calculators** (`ResultSlotScheduleCalculator`,
  `DrawProcessingDuePolicy`, draw cutoff) — due/not-due boundaries.

### 3b. Job plumbing — Integration (one wired case per rung, + guards)

Each rung = **one** `@SpringBootTest`/`@DataJpaTest` wired case; do NOT re-run the
transition matrix here (3a owns it).

- **generate** — `GenerateDrawsForRangeCommandHandler`: range → draws persisted in
  `SCHEDULED`, idempotent on re-run (no duplicates), respects the draw limit.
- **open** — `OpenDueDrawsCommandHandler`: only *due* SCHEDULED draws → `OPEN`;
  not-yet-due untouched.
- **close** — `CloseDueDrawsCommandHandler`: past-cutoff OPEN → `CLOSED`; sell after
  close is refused.
- **apply** — `FetchExternalResultsWindowCommandHandler` →
  `ApplyExternalResultsWindowCommandHandler` → `ConfirmDrawResultCommandHandler`:
  CLOSED + results → `RESULTED`; **idempotent replay = one result, one settlement
  basis**, stats persisted.
- **settle** — `SettleDrawCommandHandler`: RESULTED → `SETTLED`, ticket settlement
  statuses derived, payout basis persisted; replay does not double-settle.

### 3c. Batch infrastructure — bulletproof once (shared plumbing)

Owners: `app/batch`, `app/job`, `common/job`.

- **`BatchGate`** (`app/batch/gate`) — gate **off** → job is a no-op (nothing runs);
  gate on → runs. Unit the resolver; 1 IT that a gated job skips.
- **ShedLock** (`ShedLockRuntimeConfig`) — a scheduled tick holds the lock so **two
  instances don't both run** the rung. 1 IT (or documented manual proof).
- **Job registry** (`SpringTchJobRegistry`/`RegisteredJob`) — every registered job
  key resolves to exactly one job (mirror of the bus registration test).
- **`DrawLifeCycleTickScheduler`** — 1 IT: a single tick advances a due draw one
  rung (SCHEDULED→OPEN) and is safe to run again (no double advance).
- **`TchJobAspect`** — job context binding/params applied; unit with a fake join point.

## 4. Multi-tenant & money guards (cross-cutting, never skip)

- Every lifecycle IT asserts **tenant isolation** (a job for tenant A never touches
  tenant B's draws) — RLS active during batch context.
- Settlement/pricing ITs assert **idempotency** (replay = no double money effect).

## 5. Incohérences remontées (live log)

Append as found. Format: **[state]** claim — evidence — proposed action.
`state ∈ {CONFIRMED, TO-VERIFY}`.

- **[TO-VERIFY] `LockDraw`/`UnlockDraw` have no state and no transition.**
  `DrawStatusTransition` has no `LOCKED` and no lock/unlock edge, yet
  `LockDrawCommandHandler` / `UnlockDrawCommandHandler` exist. → Is "lock" an
  orthogonal flag on an OPEN/CLOSED draw, or dead/legacy? A test can't assert a
  lock transition because there is none. **Action**: confirm the lock semantics
  before testing; if orthogonal, test it as a flag, not a status; if dead, remove.
- **[TO-VERIFY] Result correction/override has no return transition.**
  `RESULTED → {SETTLED}` only (no `RESULTED→RESULTED`, no `SETTLED→RESULTED`), yet
  `CorrectAppliedDrawResultCommandHandler`, `OverrideDrawResultCommandHandler`,
  `MarkDrawResultOverriddenCommandHandler` exist. → How is a correction *after*
  RESULTED (or after SETTLED) reconciled with the state machine and with money
  already settled? **Action**: map the correction path before writing the settle
  test; it likely mutates `DrawResultStatus` in place — assert that explicitly.
- **[TO-VERIFY] Two status enums.** `DrawStatus` (lifecycle) vs `DrawResultStatus`
  (`core/drawresult`). → Their coupling (which `DrawStatus` values are valid for
  which `DrawResultStatus`) is implicit. **Action**: one test pinning the coupling,
  or a documented note if intentionally decoupled.
- **[TO-VERIFY] Many open/close variants.** `OpenDueDraws` vs `OpenTodayDraws` vs
  `OpenDraw`; `CloseDueDraws` vs `CloseDraw` vs `LockDraw`. → Confirm which the job
  actually schedules vs which are admin-manual, so the plumbing IT drives the real
  scheduled path and we don't test a dead entrypoint.
