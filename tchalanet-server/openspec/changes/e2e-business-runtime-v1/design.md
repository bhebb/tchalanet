# Design — E2E Business Runtime V1

## 1. Test levels

| Level | Purpose | Frequency |
|---|---|---|
| L0 boot smoke | API/auth/public basics | every run |
| L1 daily smoke | main happy paths | daily |
| L2 business critical | POS/sales/limits/promotions/idempotency | nightly / before merge |
| L3 concurrency correctness | small parallel race scenarios | nightly / on demand |
| L4 performance/load | throughput/latency/stress | separate future suite |

## 2. E2E is not performance

E2E concurrency tests use small controlled parallelism, typically 2 to 10 requests.

Allowed:
- idempotency race;
- limit race;
- session close vs sell race;
- parallel tenant A/B sales.

Forbidden in E2E V1:
- throughput assertions;
- p95/p99 latency;
- max-user load;
- DB benchmark;
- high-volume stress.

## 3. Python structure

```text
testing/e2e/
  pyproject.toml
  pytest.ini
  .env.example

  tch_e2e/
    config.py
    auth.py
    client.py
    api_response.py
    scenario_world.py
    assertions.py
    data_factory.py
    ticket_matrix.py
    concurrency.py

  tests/
    public/
    auth_context/
    onboarding/
    dashboard/
    overview/
    cashier_pos/
    business_critical/
    multitenant/
    concurrency/
```

## 4. ScenarioWorld

Every run creates a unique `run_id`.

### Tenant A

Normal tenant:

- profile `DEFAULT_HAITI_LOTTERY`;
- tenant admin A;
- cashier A1/A2;
- active outlet;
- bound active terminal;
- open session;
- normal limits/promotions.

### Tenant B

Different configuration:

- profile `DEFAULT_HAITI_LOTTERY`;
- tenant admin B;
- cashier B1;
- active outlet/terminal/session;
- limits/promotions different from A.

### Tenant C

Incomplete tenant:

- profile `MINIMAL`;
- tenant admin only;
- no outlet;
- no terminal;
- no cashier.

Tenant C validates readiness/onboarding issues.

## 5. Ticket matrix

| Scenario | Size | Games | Lines | Stakes | Purpose |
|---|---:|---|---:|---|---|
| SHORT_SINGLE_GAME_LOW_STAKE | short | 1 | 1 | low | smoke sell |
| SHORT_SINGLE_GAME_HIGH_STAKE | short | 1 | 1 | high | limit line/block |
| MEDIUM_MULTI_LINE_MIXED_STAKE | medium | 1-2 | 3-5 | mixed | totals/payout |
| MEDIUM_MULTI_GAME | medium | 2-3 | 3-6 | mixed | pricing per game |
| LONG_ALL_GAMES | long | all enabled V1 games | 8-20 | mixed | all games + print |
| LIMIT_EXPOSURE_RACE | medium | 1 | concurrent | near limit | race correctness |
| PROMO_BOOST_ODDS | short/medium | eligible game | 1-3 | eligible | odds snapshot |
| PROMO_FREE_GAME_LINE | medium | paid + free | 2+ | eligible | free line snapshot |
| PROMO_WAIVE_CHARGE | short | any | 1 | with SMS | charge waived |

Rules:
- selections must differ across lines/scenarios;
- stakes must differ across lines/scenarios;
- long ticket should include every enabled V1 game where pricing exists;
- expected potential payout is computed from returned line snapshots.

## 6. Potential payout

For every successful sale, assert:

- line snapshots;
- line count;
- stake total;
- money breakdown;
- potential payout;
- promotion snapshots when applicable.

Tests should compute expected payout from response snapshots, not from hard-coded assumptions.

## 7. Promotions V1

Effects:
- FREE_GAME_LINE;
- BOOST_ODDS;
- WAIVE_CHARGE.

Assertions:
- BOOST_ODDS uses odds override snapshot and potential payout uses boosted odds.
- FREE_GAME_LINE materializes the free/promotional line or snapshot and paid total remains correct.
- WAIVE_CHARGE changes money breakdown, not line pricing/payout.

## 8. Limits V1

Cases:
- below limit success;
- exactly at limit documented behavior;
- above line limit blocked;
- above ticket total limit blocked;
- per-selection/exposure limit if implemented;
- tenant A/B different limit configs;
- controlled race where not all sells can succeed.

## 9. POS cashier flow

Main path:
1. login cashier;
2. GET /tenant/cashier/home;
3. select/bind operational context if needed;
4. open session;
5. list available draws;
6. sell ticket;
7. print ticket;
8. send ticket notification if configured;
9. list recent tickets;
10. close session.

Negative cases:
- missing context;
- untrusted context;
- locked terminal;
- revoked/unregistered terminal;
- blocked outlet;
- sales disabled outlet;
- closed session;
- terminal/outlet/session mismatch;
- cross-tenant ids.

## 10. Multi-tenant isolation

Required:
- Admin A does not see B data.
- Cashier A cannot use B outlet/terminal/session.
- Dashboard A does not contain B identifiers.
- Tenant A/B limits apply independently.
- Tenant A/B promotions apply independently.
- Super admin sees platform-level summaries through platform endpoints.
