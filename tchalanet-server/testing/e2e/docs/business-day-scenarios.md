# Canonical Server E2E Entry Point

**Status:** single canonical scenario entry point.
**Operational runner:** `testing/e2e/scripts_agent_run.sh`
**Canonical scenario:** business-day.
**Primary test:** `testing/e2e/tests/full_flow/test_business_day_scenarios.py`
**Reusable scenario data:** `testing/e2e/tch_e2e/business_day.py`

This file is the source of truth for server E2E scenario intent. There is
currently **one** canonical scenario: business-day. Do not create parallel
scenario matrices in `README.md`, `loadtest/README.md`, individual test comments,
or new docs. When a new scenario such as draw lifecycle is needed, add it here
first, then wire a named mode in `scripts_agent_run.sh`.

## 0. Entry Point

Agents should run:

```bash
cd tchalanet-server/testing/e2e
bash scripts_agent_run.sh agent
```

Named modes:

| Mode | Purpose |
|------|---------|
| `agent` | Default agent check: L0 smoke, reduced business-day, BetOptions support check |
| `smoke` | L0 only |
| `business-day` | Reduced canonical business-day run: two tenants and one draw |
| `full-business-day` | Canonical business-day with all configured tenants/draws |
| `bet-options` | Support check for option snapshots/settlement used by business-day products |
| `availability-gates` | Isolated support check for sale rejection gates; requires `TCH_E2E_ALLOW_CATALOG_MUTATION=true` |

Only `business-day` is a scenario. Other modes are either boot smoke or support
checks for the scenario. Do not add new pytest entry points to this table unless
they are first modeled as a new canonical scenario in this file.

## 1. Goal

This scenario creates enough realistic business data to validate stats and
reports with deterministic assertions. It is intentionally a happy path first:
the matrix is structured so more negative cases, variants, or a Locust flow can
reuse the same tenant plans, seller-terminal plans, draw selection, and ticket
basket.

The default run provisions **5 tenants** with **2 seller terminals each**:
**10 seller terminals total**. To run 20 seller terminals later, add two more
`SellerTerminalPlan` entries per tenant; the sales loop already iterates over
every configured seller terminal.

All money amounts in this document are expressed in tenant currency **minor
units** unless the value is explicitly formatted as an amount such as `50.00`.

## 2. Draw Scenario

| Item | Value |
|------|-------|
| Provisioning profile | `DEFAULT_HAITI_LOTTERY` |
| Draw channels | All active tenant draw channels returned by `GET /tenant/draw-channels?activeOnly=true` |
| Date range | `TCH_E2E_BUSINESS_DAY_START`, default `2026-07-09`, through the current day |
| Selection rule | One draw per active draw channel/result slot |
| Historical sale workaround | Selected draws are rescheduled/opened for selling while preserving the scenario date range |
| Optional cap | `TCH_E2E_BUSINESS_DAY_DRAW_COUNT`; unset or `0` means all active channels |

Manual result entry uses one result per `(drawDate, slotKey)` and then runs
result application. `TCH_E2E_RESULT_APPLY_MODE=force` is the default because the
current scheduler scans only today/yesterday slot dates. `scheduler` and
`scheduler_then_force` are kept to measure the real scheduler path on eligible
dates.

Historical replay modifies the selected draw lifecycle fields needed to make a
past official draw sellable: `scheduledAt`, `cutoffAt`, and `status=OPEN`.
The generated `drawDate` stays the business draw date selected from the
historical range. Report assertions are scoped by the draw ids and the
business/report date range; sale timestamps are real test execution timestamps.

Execution modes are intentionally separate:

- `force`: deterministic business correctness/backfill run.
- `scheduler`: scheduler eligibility validation for today/yesterday draws only.
- `scheduler_then_force`: give the scheduler a chance, then force apply so the
  data run can complete deterministically.

The happy path intentionally treats **result application** as the business
trigger. Applying a confirmed manual result evaluates the sold tickets,
auto-settles the winning ticket state, and emits `TicketPayoutPaidEvent` as the
single financial event used by analytics for both `winningsCalculated` and
`payoutsPaid`. The draw lifecycle `settle` operation can still mark a draw as
`SETTLED`, but the reporting assertions do not depend on that extra draw-state
transition for now.

If operations must correct the amount actually paid, the paid amount adjustment
API emits an audited payment-adjustment event that changes `payoutsPaid` only.
It does not mutate the ticket's calculated winning amount; a wrong calculated
winning amount means the settlement code/configuration must be fixed and
replayed.

## 3. Tenant Configuration Matrix

| Tenant key | Maryaj gratis config | Seller terminals | Limits | Overrides | Core assertions |
|------------|----------------------|------------------|--------|-----------|-----------------|
| `alpha` | `HT_MARYAJ_GRATIS` fixed amount, auto-generate | `main` at 10%; `override` at 17.50% | Block selection `44`; draw-channel exposure cap `100000` minor units | `override` has Bolet odds `65.0000` | Auto promo line exists; generated source is `PROMOTION_GENERATED`; blocked selection fails; override commission and Bolet payout apply |
| `beta` | `HT_MARYAJ_GRATIS` fixed amount, auto-generate | `main` at 10%; `commission` at 15.00% | None | Seller commission override only | Auto promo line exists; seller report commission uses 15.00% for `commission` |
| `gamma` | Maryaj gratis disabled | `main` at 10%; `backup` at 10% | Block selection `44` | None | No promotion line is created; blocked selection fails; normal winning tickets are calculated after result application |
| `delta` | `HT_MARYAJ_GRATIS` fixed amount, seller selects number | `main` at 10%; `override` at 12.50% | Draw-channel exposure cap `100000` minor units | `override` has Bolet odds `65.0000` | Promo line exists; `choiceMode=SELLER_SELECTS`; selected free Maryaj is `13-21`; duplicate paid/free selection rejection is avoided |
| `epsilon` | `HT_MARYAJ_GRATIS` fixed amount, auto-generate | `main` at 10%; `backup` at 10% | None | None | Control tenant for base totals and cross-tenant report isolation |

The multiplier Maryaj gratis case is intentionally excluded. Current
`HT_MARYAJ_GRATIS` pricing policy accepts fixed amount pricing for this game and
rejects stake multipliers, so this happy path documents only the supported
runtime behavior.

The draw-channel exposure cap is deliberately above the happy-path basket
exposure so the full basket completes first. Exposure rejection belongs to a
dedicated negative sale using a selection outside the basket, not to the normal
happy-path volume. The negative exposure assertion uses dedicated selection
`99`, which is absent from the basket, so its expected current exposure is `0`.
It temporarily lowers the selected draw-channel cap to `currentExposure + 100`
minor units, submits one dedicated `99` selection for `200` minor units,
verifies rejection before ticket creation, then restores the original cap in
`finally`.

## 4. Manual Result Plan

| Result field | Value | Winning selections covered |
|--------------|-------|----------------------------|
| `lot1` | `112` | Bolet `12`, Loto3 `112`, Loto5 lot1 segment |
| `lot2` | `21` | Maryaj exact `12-21`, Loto5 lot2 segment |
| `lot3` | `25` | Loto4 suffix, Loto5 lot3 segment |

| Game | Scenario selection | Expected reason |
|------|--------------------|-----------------|
| Bolet | `12` | Last two digits of `lot1=112` |
| Maryaj | `12-21` | Bolet number plus `lot2` |
| Loto3 | `112` | Exact lot1 |
| Loto4 | `2125` | `lot2 + lot3` |
| Loto5 lot1-lot2 | `11221` | `lot1 + lot2` |
| Loto5 lot1-lot3 | `11225` | `lot1 + lot3` |
| Loto5 mixed | `22125` | Last digit of lot1 plus lot2 plus lot3 |

## 5. Sale Basket

Each seller terminal sells the basket below on every selected draw. With the
default two sellers per tenant, each tenant sells **24 tickets per draw**.
`TCH_E2E_BUSINESS_DAY_BASKET_REPEATS` can multiply the basket when more volume
is needed.

| Ticket key | Lines | Paid stake | Expected win | Assertions |
|------------|-------|------------|--------------|------------|
| `short` | One non-winning Bolet `17` | `100` minor units | `0` | Prepared amount equals paid stake; no winner |
| `maryaj-*` | Paid Maryaj `12-21`; optional free Maryaj gratis line | `100` minor units | `95000` minor units for paid Maryaj | Maryaj gratis present/absent by tenant config; paid Maryaj win is included; seller-selected free line uses `13-21` because duplicate paid/free selection is currently rejected |
| `win-lot1-only` | Bolet `12` | `100` minor units | `5000` minor units, or `6500` minor units for seller Bolet odds override | Bolet payout uses seller-specific odds snapshot |
| `win-lot1-lot2` | Loto5 option 1, `11221` | `100` minor units | `2500000` minor units | Loto5 lot1-lot2 payout included |
| `win-lot1-lot3` | Loto5 option 2, `11225` | `100` minor units | `2500000` minor units | Loto5 lot1-lot3 payout included |
| `win-lot1-lot2-lot3` | Loto3 `112`, Loto4 `2125`, Loto5 mixed `22125` | `300` minor units | `3050000` minor units | Multiple winning games on one ticket are calculated together |
| `volume-nonwinner-00..05` | Non-winning Bolet, Maryaj, and Loto3 lines | `300` minor units each | `0` | Report totals get realistic volume without changing winnings |

Auto-generated Maryaj gratis validates that the free-game line is generated and
reported, and marked with `selectionSource=PROMOTION_GENERATED`. Because the
current runtime generator is random, auto-generated promotional winnings are
excluded from exact aggregate-winning assertions until an E2E seed or
deterministic generator override is available. The seller-selects tenant uses
`13-21` for now because the runtime rejects a free Maryaj selection that
duplicates the paid `12-21` line; promotional seller-selected winning payout
can be reintroduced when duplicate winning free lines are supported explicitly.

## 6. Assertions

| Area | Assertions |
|------|------------|
| Provisioning/config | Every tenant is provisioned from `DEFAULT_HAITI_LOTTERY`; active draw channels are non-empty; configured limits/promotions/overrides are applied before sales |
| Sale preparation | Prepared total equals the scenario paid stake; confirmed sale is accepted; promotion lines match the tenant Maryaj gratis mode |
| Maryaj gratis | Enabled tenants create exactly one promotion line per eligible ticket; auto tenants create `choiceMode=AUTO_GENERATE` and `selectionSource=PROMOTION_GENERATED`; seller-selects tenant creates `choiceMode=SELLER_SELECTS`, `selectionSource=CUSTOMER_SELECTED`, selection `13-21`; disabled tenant creates zero promo lines |
| Limits | Tenants with blocked selection `44` reject that sale; exposure-limit tenants complete the happy path, then a dedicated `99` selection sale proves the cap rejects without creating a ticket |
| Result processing | Manual results are recorded, then result application runs through the selected mode; force mode is used for historical backfill; draw lifecycle settle is not required for the reporting happy path |
| Admin reports | `ticketsSold`, `grossSales`, paid-line winnings, and `promotionLines` match exactly; seller-selected promotional lines are asserted exactly but non-winning while duplicate paid/free Maryaj selections are rejected; random auto-generated promotional winnings are asserted as a bounded optional amount until the generator is deterministic; selected draw IDs and channel identifiers appear in draw reports |
| Seller reports | Per-seller ticket count, gross sales, and commission match the seller-terminal plan; promotional/free lines do not add paid gross or commission |
| Seller × draw reports | `sellerTerminalDrawRows` contains one exact row per configured seller terminal and selected draw; every selected draw must show participation from all seller terminals with exact ticket count, gross sales, and commission |
| Stats/top selections | Winning selection `12` appears in top selections after sales and results |
| Isolation | Aggregated foreign-draw report queries return zero totals; draw reports do not expose foreign draw rows/metadata; unfiltered reports stay scoped to the current tenant |

Expected totals are computed in the test harness from immutable scenario inputs:
ticket count, paid gross, deterministic winnings, random auto-promo winning
ceiling, seller gross, seller commission, and promotion-line count. Pytest and
future Locust validation should use the same scenario builders instead of
duplicating these numbers.

## 7. Sale Availability Gates

`test_sale_availability_gates_block_unavailable_runtime_state` is separate from
the reporting happy path. It provisions one isolated tenant, proves a baseline
sale works, then flips one availability gate at a time and asserts the sale is
not accepted. Each gate captures the draw ticket count before the blocked
attempt and verifies it is unchanged afterwards. The tenant-suspension gate
captures the count before suspension and checks it only after platform-admin
restoration, so cleanup never depends on a tenant session that has just been
suspended. It also provisions a control tenant and sells a ticket on that
tenant while the target tenant is suspended, proving the lifecycle gate does not
spill into other tenants.

| Gate | Owner | Mutation | Impact scope | Expected rejection point | Restore |
|------|-------|----------|--------------|--------------------------|---------|
| Seller terminal not active | Tenant seller-terminal state | `PATCH /admin/seller-terminals/{id}/block` | Tenant seller terminal | Confirm, because prepare stores a draft before terminal sale validation is rechecked | `PATCH /admin/seller-terminals/{id}/unblock` |
| Tenant game not active | Tenant game settings | `POST /admin/games/HT_BOLET/disable` | Tenant only | Prepare | `POST /admin/games/HT_BOLET/enable` |
| Tenant not active | Platform tenant lifecycle | `POST /platform/tenants/{tenantId}/suspend` | One tenant | Prepare/confirm forbidden through platform access/runtime guards | `POST /platform/tenants/{tenantId}/activate` and verify `status=ACTIVE` |
| Tenant draw channel inactive | Tenant draw-channel config through platform endpoint with tenant override | `POST /platform/draw-channels/{id}/disable` | Tenant channel only | Prepare | Restore captured channel object with `PUT /platform/draw-channels/{id}` |
| Draw not open | Draw lifecycle | `POST /admin/draws/lifecycle/close` | One draw | Prepare | Not restored; terminal state is no longer used after this gate |
| Result slot inactive | Global result-slot catalog | `POST /platform/result-slots/{slotKey}/disable` | Global provider slot; test must run serialized | Prepare | Restore captured slot object with `PUT /platform/result-slots/{id}` and verify active |

The seed contains four active Texas result slots used by the default Haiti
draw-channel profile: `TX_1000`, `TX_1227`, `TX_1800`, and `TX_2212`. The
result-slot inactive gate uses one of these slots and restores the complete
captured slot object in a `finally` block. Global catalog game kill switches are
still not used against shared game rows; tenant-scoped game disable is used for
the game-inactive gate. The test is marked `serial_catalog_mutation` and should
run only in an isolated E2E environment with schedulers disabled by the runner.
It is skipped unless `TCH_E2E_ALLOW_CATALOG_MUTATION=true` is set.

## 8. Locust Reuse Notes

The tenant plans and sale basket live in `tch_e2e.business_day` instead of inside
the pytest body. A business-day Locust flow must reuse:

- `default_tenant_plans()` for tenant and seller-terminal provisioning shape,
- `ticket_basket(result, seller, plan)` for repeatable sale payloads,
- `sale_payload(draw, scenario)` for cashier API payload construction,
- `ManualResultPlan` for deterministic winning selections.

Locust should vary concurrency and repeat counts, not rewrite the business
truth. That keeps pytest and load tests comparable.

The current `loadtest/` package still contains the generic POS load harness. It
is not a second scenario matrix. The next Locust step is to add a business-day
user that consumes this registry's tenant plans, seller-terminal plans, selected
draws, and ticket basket.

## 9. Runtime Knobs

| Env var | Default | Use |
|---------|---------|-----|
| `TCH_E2E_BUSINESS_DAY_START` | `2026-07-09` | Start date for generated draws |
| `TCH_E2E_BUSINESS_DAY_DRAW_COUNT` | all active channels | Cap selected draws for shorter runs |
| `TCH_E2E_BUSINESS_DAY_TENANTS` | all five tenants | Comma-separated tenant keys for focused QA runs, for example `alpha,delta`; isolation assertions require at least two tenants |
| `TCH_E2E_BUSINESS_DAY_BASKET_REPEATS` | `1` | Multiply the basket per seller/draw |
| `TCH_E2E_BUSINESS_DAY_MIN_TICKETS_PER_DRAW` | `10` | Guardrail for report reliability |
| `TCH_E2E_TICKET_PRINT_MODE` | `none` | Optional receipt rendering: `none`, `sample` for one PDF per tenant/seller, or `all` |
| `TCH_E2E_TICKET_SLACK_MODE` | `none` | Optional ticket delivery to Slack: `none`, `sample` for one send per tenant/seller, or `all` |
| `TCH_TEST_SLACK_CHANNEL_KEY` | `delivery` | Slack channel key used by `/tenant/cashier/tickets/{id}/send` when Slack mode is enabled |
| `TCH_E2E_HOST_HEADER` | unset | Optional HTTP `Host` override, useful for Traefik routes such as `api.localtest.me` when local DNS is unavailable |
| `TCH_E2E_RESULT_APPLY_MODE` | `force` | `force`, `scheduler`, or `scheduler_then_force` |
| `TCH_E2E_RESULT_REPORT_MAX_SECONDS` | `20` | Max wait for report projections after forced result application; legacy `TCH_E2E_RESULT_SETTLE_MAX_SECONDS` is still accepted |
| `TCH_E2E_ALLOW_CATALOG_MUTATION` | unset | Required `true` for the serialized availability-gates test that mutates result-slot/draw-channel kill switches |

Useful QA commands:

```bash
# Default agent run: L0 + reduced business-day + BetOptions support check.
bash scripts_agent_run.sh agent

# Reduced canonical business-day only.
bash scripts_agent_run.sh business-day

# Full canonical business-day with all configured tenants/draws.
bash scripts_agent_run.sh full-business-day

# Isolated availability-gates run. Do not run in parallel with other E2E jobs.
TCH_E2E_ALLOW_CATALOG_MUTATION=true \
bash scripts_agent_run.sh availability-gates
```
