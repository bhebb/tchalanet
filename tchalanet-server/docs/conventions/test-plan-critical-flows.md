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
  - `PromotionChargeApplier` `[unit-covered]` — `PromotionChargeApplierTest`: waives
    only the targeted charge type (amount kept, buyer-facing dropped), label
    fallback, strict no-op for non-WAIVE effects. (Was only hit by the Spring IT.)
  - `SalePromotionEffectApplier` / `PromotionOddsBoostApplier` `[remaining]`.
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

- **generate** `[covered]` — `GenerateDrawsForRangeCommandHandler`: range → draws in
  `SCHEDULED`. Covered by `DrawSellabilitySpringIntegrationTest` (generate→open→POS
  visibility). Idempotent-rerun assertion still worth adding.
- **open** `[covered]` — `OpenDueDrawsCommandHandler`: due SCHEDULED → `OPEN`. Same
  test.
- **close** `[covered]` — `CloseDueDrawsCommandHandler`: past-cutoff OPEN → `CLOSED`.
  `DrawLifecycleCloseSpringIntegrationTest` (green): dryRun no-op, directional
  OPEN↓/CLOSED↑, idempotent replay. "Sell after close refused" still to add.
- **apply** `[covered]` — `RecordManualDrawResultCommandHandler` (writes the
  draw_result) → `ApplyExternalResultsWindowCommandHandler` attaches it and moves
  `CLOSED → RESULTED`. Covered by `DrawResultOverrideSettleSpringIntegrationTest`.
  Recipe note: `RecordDrawTicketsResult` is NOT the transition step — it requires a
  result already attached ("Draw has no result attached"); the window-apply is what
  transitions the draw. Idempotent-replay assertion still worth adding.
- **settle** `[covered]` — `Draw.settle()` precondition proven at unit level by
  `DrawTest` (legal only from RESULTED). End-to-end `SettleDrawCommand` (RESULTED
  + CONFIRMED → SETTLED) covered by `DrawResultOverrideSettleSpringIntegrationTest`.
  Gotcha found: `SettleDrawCommandHandler` requires the `draw_result` to be
  **CONFIRMED** — a `force=true` override leaves it `OVERRIDDEN`, which is not
  settleable until re-confirmed. No-double-settle assertion still worth adding.

> **Reaching RESULTED is a multi-module orchestration**, not a single command:
> `RecordManualDrawResultCommandHandler` (drawresult) writes a `draw_result`
> (CONFIRMED), then a sales/apply step (`RecordDrawTicketsResultCommandHandler` /
> `DrawApplyJdbcRepository`) links it to the draw and transitions
> `CLOSED → RESULTED`. The apply→settle IT is a focused follow-up; the settle
> *decision* is already unit-proven.

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
- **[PARTLY-EXPLAINED] Result correction/override has no `DrawStatus` return edge.**
  `RESULTED → {SETTLED}` only, yet `CorrectAppliedDrawResultCommandHandler` /
  `OverrideDrawResultCommandHandler` / `MarkDrawResultOverriddenCommandHandler`
  exist. Reading the model: corrections operate on a **separate** sub-status
  `DrawResultStatus {PROVISIONAL, CONFIRMED, OVERRIDDEN, ERROR}` **in place** while
  `DrawStatus` stays `RESULTED` — so there is no lifecycle back-edge by design.
  **Still to verify**: a correction *after* `SETTLED` (money already applied) — the
  `DrawStatus` machine forbids leaving SETTLED, so how is a post-settlement
  correction represented and how does it reconcile the ledger? Pin this in the
  settle test.
- **[TO-VERIFY] Two status enums.** `DrawStatus` (lifecycle) vs `DrawResultStatus`
  (`core/drawresult`). → Their coupling (which `DrawStatus` values are valid for
  which `DrawResultStatus`) is implicit. **Action**: one test pinning the coupling,
  or a documented note if intentionally decoupled.
- **[TO-VERIFY] Many open/close variants.** `OpenDueDraws` vs `OpenTodayDraws` vs
  `OpenDraw`; `CloseDueDraws` vs `CloseDraw` vs `LockDraw`. → Confirm which the job
  actually schedules vs which are admin-manual, so the plumbing IT drives the real
  scheduled path and we don't test a dead entrypoint.

### Job-plumbing findings (from writing the close IT)

- **[RESOLVED — not an incoherence] `opened`=28 but close `due`=19.** Investigated
  with a throwaway diagnostic IT (raw `draw` vs `v_draw_summary`, in/out of tenant
  context). Findings: all **28** draws exist AND are visible in `v_draw_summary`
  (`viewOpen=28`, `viewJoinLoss=0`; `result_slot_id` is `NOT NULL` so the inner
  join never drops rows), and the view read is consistent bare vs in-context (RLS
  is not cutting it). The **19** is purely `findDueToClose`'s `cutoff_at <= now`
  filter: the 28 are Haiti (UTC-4) draws on 2026-07-09 with cutoffs 10:55–23:29
  local; the 9 evening ones cross **past 2026-07-10T00:00Z in UTC**, so they are
  legitimately not yet due at that instant. No draws are dropped. Lesson for the
  settle test: assert directional invariants, and remember cutoff is UTC — don't
  equate command-result counts to a fixed number.

### Domain findings (from writing DrawTest)

- **[RESOLVED — not dead code] `Draw.applyResult` inner "already has result" guard
  is a live defensive check.** Via the normal lifecycle it is unreachable (only
  RESULTED carries a resultId, and RESULTED→RESULTED throws in the transition check
  first). BUT the full constructor does **not** validate `status` vs `drawResultId`,
  so a reconstituted CLOSED draw carrying a resultId (data anomaly / migration) is
  representable — there the transition passes and the guard fires. `DrawTest`
  covers this reconstitution case → guard kept, not removed.
- **[FIXED] `OverrideDrawResultCommandHandler` re-applied via `applyResult`, which
  cannot replace an existing result.** Confirmed by reading the write path:
  `writer.upsert(slot,date,…)` **reuses the existing `draw_result` id** on override
  (`created=false`), so `findByDrawResultId(res.id())` returns the already-**RESULTED**
  draws; calling `draw.applyResult(...)` on them (first-result-only: `CLOSED→RESULTED`,
  `resultId==null`) throws `IllegalStateException` (RESULTED→RESULTED) — override of a
  resulted, non-settled draw was broken. `CorrectAppliedDrawResultCommandHandler`
  already used the right method. **Fix**: `applyOverrideToDraws` now calls
  `draw.overrideResult(res.id(), now, reason)` (requires RESULTED, replaces in place).
  Red→green proven at the aggregate level in `DrawTest` AND **end-to-end** in
  `DrawResultOverrideSettleSpringIntegrationTest` (generate→open→close→apply→override→
  settle): RED observed by temporarily reverting the handler (`override` threw
  `IllegalStateException: RESULTED -> RESULTED` over the real stack), GREEN with the
  fix. Note: this handler had **zero existing test coverage** (why the bug hid).

### Architecture-test findings (from reviewing `arch/` + `architecture/`)

- **[FIXED] `SecurityArchTest` did not require authorization on `void` handlers.**
  `HaveAuthorizationAnnotationCondition.check` filtered handlers with
  `.filter(m -> !m.getRawReturnType().isEquivalentTo(void.class))`, so a
  void-returning `@PostMapping`/`@DeleteMapping` (cancel, delete, …) in a protected
  scope (`/admin`, `/platform`, `/_sdr`, `/tenant/tickets`) escaped the
  `@PreAuthorize`/`@Secured` check. **Fixed** (JDK 25, red→green): removed the void
  filter; added test-only fixture `ProtectedVoidUnsecuredController` (outside
  `com.tchalanet.server`) + regression test `voidHandlerInProtectedScopeWithout
  AuthorizationIsFlagged`. Verified: regression failed before the fix, passes
  after; and `protectedScopeControllersMustHavePreAuthorize` still passes against
  the real codebase → no existing unsecured void endpoint.
- **[CONFIRMED] Split packages `arch/` and `architecture/`.** Same concern, two
  homes (`arch/`: PageModel, Security, Feature, Timezone, Flyway; `architecture/`:
  CleanArch, Modulith, PlatformGates, OperationalContext, Kernel). **Action**:
  consolidate into one package so the suite is discoverable and rules aren't
  duplicated across both.
- **[GAP] Non-negotiables without an ArchUnit rule.** `project.md` mandates
  strongly-typed IDs (except persistence) and CQRS-via-bus; no rule enforces them.
  **Action**: add rules (typed-ID usage outside `*persistence*`; handlers invoked
  only through `CommandBus`/`QueryBus`; carriers `*Command/*Query/*Request/*Response`
  stay logic-free — mirrors §1 exclusions).
