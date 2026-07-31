# Error Management Weekend Test Plan

## Objective

Reproduce the main failure classes in a disposable local environment, then verify that the
backend, web, mobile contract, browser E2E, and Locust behavior agree on the same error rules.
This plan is for local or explicitly isolated staging only. Never stop a production service,
modify production data, or run destructive SQL against a shared tenant.

## Expected contract

| Failure | Expected behavior |
|---|---|
| Required page request fails | Blocking error panel, retry, correlation id when available |
| Optional section fails | Other sections stay visible; local section error or notice |
| Mutation fails | Action stays actionable; no duplicate shell error; normalized feedback |
| Validation/data problem | Field or section feedback; no raw server stack/prose |
| Auth/session failure | Session is invalidated or re-authentication is requested; no retry loop |
| Recovery after restart | Retry or refresh restores data without a full browser reload when supported |

## Preparation

1. Work from a disposable local stack and record the commit under test.
2. Start the normal stack and save a baseline screenshot, browser console, API logs, and health:

```bash
cd /Users/bhebb/Developer/tchalanet/tchalanet-infra
make local-product-up
curl -sk -H 'Host: api.localtest.me' https://127.0.0.1/api/v1/actuator/health
make ps
make logs-api
```

3. Use one seeded tenant with at least one configured game, channel, draw, result, seller,
   subscription, limit, commission, and financial report. Keep the tenant id and a correlation
   id for every scenario.
4. Before each scenario capture the initial state. After each scenario restore the service or
   fixture and rerun the baseline flow.

## Failure injection matrix

### A. Stop the API service

This verifies transport errors and the blocking behavior of a required request.

```bash
docker stop tchl-api-dev
# Open or refresh the page under test, then click its retry action.
docker start tchl-api-dev
```

Run this first on admin setup/configuration, generated draws, draw detail, result detail,
financials, subscription, and the public results page. Expected result: a visible translated
blocking error, no infinite spinner, and successful recovery after the API is started again.

### B. Stop one dependency

This verifies that a backend dependency outage becomes a stable API error rather than a leaked
exception. Use one dependency at a time and collect API logs.

```bash
docker stop tchl-redis-dev
# Exercise one read and one mutation, then inspect the API response and logs.
docker start tchl-redis-dev
```

Repeat with the local Postgres container only if the API is configured to reconnect cleanly:

```bash
docker stop tchl-postgres-dev
# Exercise a read, record status/code/trace id, then restore the database.
docker start tchl-postgres-dev
```

Expected result: the web never shows a Java exception, raw SQL, or endless retry storm; the
backend emits a stable `ProblemDetail` or an explicitly documented transport failure.

### C. Inject an HTTP error in web E2E

Use Playwright interception so the test is deterministic and does not require a broken service.
Extend `tchalanet-web/apps/web-e2e/src/support/api-stub.ts` with a small `fail(path, status,
body)` helper if the scenario needs a reusable fixture. The body must remain an
`ApiResponse`/`ProblemDetail` envelope, for example:

```ts
await page.route('**/api/v1/**/draws/**', (route) =>
  route.fulfill({
    status: 503,
    contentType: 'application/problem+json',
    body: JSON.stringify({
      type: 'https://tchalanet.com/problems/service-unavailable',
      title: 'Service unavailable',
      status: 503,
      code: 'PLATFORM_DEPENDENCY_UNAVAILABLE',
      category: 'TRANSIENT',
      retryPolicy: 'RETRYABLE',
      detail: 'Temporary test failure',
      traceId: 'e2e-fault-503',
    }),
  }),
);
```

Exercise each required and optional slice. Expected result: required failures block only the
page; optional failures leave useful data visible; retry removes the route override and reloads
the affected resource.

### D. Inject malformed or problematic data

Prefer deterministic response fixtures over direct database corruption:

- remove a required field from a tenant-game payload;
- return an unknown draw status or an impossible date range;
- return a draw without optional activity/result metadata;
- return a validation `ProblemDetail` with `violations` for a limit, commission, or result form;
- return a successful envelope with a non-empty `notices` array for games setup.

Run the same cases through the feature store/service tests and one browser E2E. If a database
case is necessary, create an isolated fixture tenant through the E2E factory, use a transaction
or disposable database, and restore it immediately. Do not update shared staging rows by hand.

## Feature-by-feature manual pass

Record one row per page in the result log below:

| Area | Blocking case | Non-blocking/action case |
|---|---|---|
| Tenant setup and general configuration | setup/config request down | one section save fails |
| Games setup and limits config | required list down | notice or dialog validation |
| Draw channels | provider list down | provider save fails |
| Generated draws and draw detail | draw request down | activity/result save fails |
| Draw result detail | result lookup down | not-found behavior |
| Financials, commission, subscription | page read down | action/dialog fails |
| Maryaj gratis | campaign read down | game enrichment/action fails |
| Seller terminal/POS | session or sell request down | optional catalog/receipt request fails |
| Public results and analytics | required page model/read down | optional widget/query fails |

For each case verify: translated title/message, no raw exception, correct retry ownership,
stable layout, no duplicate toast/shell feedback, and recovery after the fault is removed.

## Web E2E execution

Run the deterministic stubbed suite first, then the browser against the disposable Docker API:

```bash
cd /Users/bhebb/Developer/tchalanet/tchalanet-web
pnpm e2e:web
pnpm runtime:dev-docker-emulator
pnpm e2e:web:api
```

Add focused specs for the matrix above under `apps/web-e2e/src/admin-portal/` or the owning
portal directory. Assertions must be UI-observable: error panel/section/field visibility,
retry, route recovery, and absence of raw server text. Backend status/body assertions belong in
server integration tests or the Python E2E harness.

## Backend and mobile contract checks

For each injected backend failure, save the response headers/body and API log line. Verify:

- stable HTTP status, `code`, `category`, `retryPolicy`, and correlation id;
- safe `params` only, with no secrets or stack trace;
- `ApiResponse.notices` remains non-blocking when data is usable;
- mobile maps the same response to `ApiException`/`ApiNotice`, localized copy, and the correct
  blocking or degraded widget state.

Run the focused server tests described in
`tchalanet-server/docs/conventions/error-management.md`, then the mobile error-mapping tests
from `tchalanet-mobile/docs/conventions/error-management.md`. Do not use a real production
Firebase or database for fault injection.

## Locust resilience run

Locust is for recovery, error rate, and latency under controlled load, not for asserting UI
copy. Use `local-perf`/`local-jwt`, never Firebase Auth Emulator and never production.

Baseline read run:

```bash
cd /Users/bhebb/Developer/tchalanet/tchalanet-server/testing/e2e
TCH_BASE_URL='https://127.0.0.1/api/v1' \
TCH_E2E_HOST_HEADER='api.localtest.me' \
TCH_E2E_VERIFY_SSL=false \
TCH_E2E_AUTH_PROVIDER=local-perf \
TCH_LOCAL_JWT_ISSUER=tchalanet-local \
TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters \
locust -f loadtest/locustfile.py --headless -u 2 -r 1 -t 30s \
  --host 'https://127.0.0.1/api/v1' --csv target/locust/error-baseline \
  --html target/locust/error-baseline.html --basket-min 1 --basket-max 2 --tags read
```

During a second run, stop the API for 10-20 seconds and restart it. During a third run, inject
one deterministic 503 on a read route in the local proxy/test fixture. Compare p50/p95/p99,
failure percentage, recovery time, and whether repeated retries create duplicate mutations.
Keep the run small (`1-5` users) for fault injection; use the normal read/sales smoke only after
the service is healthy again.

## Result log

Copy this table for each scenario:

| Field | Value |
|---|---|
| Scenario id / commit | |
| Environment / tenant | local disposable / isolated staging |
| Page or endpoint | |
| Fault injected and start/end time | |
| HTTP status / code / category / retry policy | |
| traceId/requestId/errorId | |
| UI state and translation checked | |
| Recovery action and result | |
| API/web/mobile logs attached | |
| E2E/Locust artifact path | |
| Finding or follow-up issue | |

## Exit criteria

- No critical page is stuck in a spinner or blank screen after any tested failure.
- Optional failures preserve usable data and are visibly distinct from empty data.
- Mutations do not double-submit after retry and do not emit duplicate shell feedback.
- Problem details are stable, safe, localized at the client, and correlated in logs.
- Web E2E passes for the injected failure cases and recovery paths.
- Locust shows a bounded failure window and recovery after the service returns.
- Every unexpected result is recorded as a defect or an explicit contract update.
