# Spec 01 — BFF endpoints (`features.pos`)

## Domain placement

```
features.pos
├── infra.web.api
│   ├── PosBootstrapController
│   ├── PosSessionBffController
│   ├── PosDashboardController
│   ├── PosSaleBffController
│   ├── PosHistoryController
│   ├── PosResultsController
│   ├── PosSettingsBffController
│   └── PosOpsController
├── application
│   ├── PosBootstrapService
│   ├── PosSessionBffService
│   ├── PosDashboardService
│   ├── PosSaleBffService
│   ├── PosHistoryService
│   ├── PosResultsService
│   ├── PosSettingsBffService
│   └── PosOpsService
└── infra.web.model     (request & response DTOs as records)
```

All paths under `/api/v1/tenant/pos/...` per `routing_and_path.md`.

## ADDED Requirements

### Requirement: BFF design contract enforced

All BFF controllers and services SHALL follow these rules:

- **One screen, one mount call**: each screen needs at most 2 BFF calls.
- **`ApiResponse<T>`** with `notices` and `serviceStatus` on all JSON responses.
- **No business logic in BFF services**: only `commandBus.send` / `queryBus.send` + DTO mapping.
- **`editable: bool`** per field is computed server-side; Flutter never duplicates authorization.
- **`Idempotency-Key` header** mandatory on all POST endpoints that create state.
- **`X-Tch-Platform: MOBILE | TERMINAL`** header used by BFF to choose lazy-virtual provisioning vs require physical terminal.
- **All amounts as decimal strings** (`"14250.00"`) in JSON to avoid float precision loss.

#### Scenario: BFF controller contains no domain logic

- **WHEN** `PosSaleBffController.place(...)` is called
- **THEN** the controller delegates entirely to `PosSaleBffService` which calls `commandBus.send(PlaceTicketCommand)`; no business rule is evaluated in the controller or service

#### Scenario: Missing Idempotency-Key on POST /sale/place is rejected

- **WHEN** `POST /tenant/pos/bff/sale/place` is called without `Idempotency-Key` header
- **THEN** 400 is returned

---

### Requirement: Endpoint catalogue — 17 endpoints implemented

| #   | Method | Path                                    | Description                                                                  |
| --- | ------ | --------------------------------------- | ---------------------------------------------------------------------------- |
| B1  | GET    | `/tenant/pos/bff/bootstrap`             | User + outlet + terminal (lazy virtual if MOBILE) + currentSession + uiHints |
| B2  | POST   | `/tenant/pos/bff/session/open`          | Open session, return enriched dashboard                                      |
| B3  | GET    | `/tenant/pos/bff/session/close/preview` | Snapshot for z-report preview                                                |
| B4  | POST   | `/tenant/pos/bff/session/close`         | Close session, return z-report                                               |
| B5  | GET    | `/tenant/pos/bff/dashboard`             | Full dashboard payload                                                       |
| B6  | GET    | `/tenant/pos/bff/sale/context`          | Active draws + bet types + sale settings                                     |
| B7  | POST   | `/tenant/pos/bff/sale/place`            | Place multi-entry ticket                                                     |
| B8  | POST   | `/tenant/pos/bff/sale/{id}/approve`     | Approve blocked ticket                                                       |
| B9  | POST   | `/tenant/pos/bff/sale/{id}/cancel`      | Cancel ticket                                                                |
| B10 | POST   | `/tenant/pos/bff/sale/{id}/reprint`     | Return printable receipt payload                                             |
| B11 | GET    | `/tenant/pos/bff/sale/{id}`             | Full ticket detail + actions                                                 |
| B12 | GET    | `/tenant/pos/bff/history`               | Paged ticket list with filters                                               |
| B13 | GET    | `/tenant/pos/bff/results`               | Settled draws with sales/payout/margin                                       |
| B14 | GET    | `/tenant/pos/bff/settings`              | Settings sections with editable flags                                        |
| B15 | PUT    | `/tenant/pos/bff/settings/{ns}/{key}`   | Upsert setting, return refreshed map                                         |
| B16 | POST   | `/tenant/pos/bff/sync/trigger`          | Force sync action                                                            |
| B17 | POST   | `/tenant/pos/bff/print/test`            | Test print (PHYSICAL only)                                                   |

#### Scenario: B1 bootstrap provisions virtual terminal on MOBILE

- **WHEN** `GET /tenant/pos/bff/bootstrap` is called with `X-Tch-Platform: MOBILE` and no ACTIVE VIRTUAL terminal exists for the agent
- **THEN** `ProvisionVirtualTerminalCommand` is dispatched and the response includes the newly provisioned terminal

#### Scenario: B1 bootstrap routes by session state

- **WHEN** `currentSession != null` in the bootstrap response
- **THEN** the response contains the session view; Flutter routes to Dashboard

#### Scenario: B5 dashboard withholds marginPct

- **WHEN** `GET /tenant/pos/bff/dashboard` is called by a caller without `sales.results.margin.read`
- **THEN** `marginPct` is `null` in `recentResults` items

#### Scenario: B7 place ticket returns PENDING_APPROVAL

- **WHEN** `POST /tenant/pos/bff/sale/place` triggers an autonomy block
- **THEN** response `state = PENDING_APPROVAL` with `block` and `approval` sections populated

#### Scenario: B10 reprint rejected on VIRTUAL terminal

- **WHEN** `POST /tenant/pos/bff/sale/{id}/reprint` is called on a VIRTUAL terminal
- **THEN** 422 `PRINTER_NOT_AVAILABLE` is returned

#### Scenario: B17 print test rejected on VIRTUAL terminal

- **WHEN** `POST /tenant/pos/bff/print/test` is called on a VIRTUAL terminal
- **THEN** 422 `PRINTER_NOT_AVAILABLE` is returned

#### Scenario: PARTIAL response on degraded call

- **WHEN** one internal query call in B5 dashboard fails with a non-critical error
- **THEN** the response has `serviceStatus` populated and the rest of the payload is returned

---

### Requirement: BFF-level cache

The following named caches SHALL be maintained at BFF level:

| Cache                   | Key                                         | TTL   | Eviction                                      |
| ----------------------- | ------------------------------------------- | ----- | --------------------------------------------- |
| `pos.bff.dashboard`     | `(tenantId, agentId)`                       | 30 s  | `TicketPlacedEvent`, `DrawSettledEvent`       |
| `pos.bff.sale_context`  | `(tenantId, outletId)`                      | 60 s  | `SettingChangedEvent`, `DrawClosedEvent`      |
| `pos.bff.ticket_detail` | `ticketId`                                  | 60 s  | `TicketCancelledEvent`, `TicketApprovedEvent` |
| `pos.bff.results`       | `(tenantId, date, lotteryCode)`             | 5 min | `DrawSettledEvent`                            |
| `pos.bff.settings`      | `(tenantId, outletId, terminalId, agentId)` | 60 s  | `SettingChangedEvent`                         |

#### Scenario: Dashboard cache evicted on ticket placed

- **WHEN** `TicketPlacedEvent` is received for `tenantId=X, agentId=Y`
- **THEN** the cache entry for `(X, Y)` is invalidated

---

### Requirement: Error codes uniformly used

All error responses SHALL use `application/problem+json` with a `code` field from the canonical list. No raw exception message leaks to the client.

Canonical codes: `NOT_ALLOWED_TO_SELL`, `OUTLET_NOT_ASSIGNED`, `TERMINAL_NOT_AVAILABLE`, `SESSION_ALREADY_OPEN`, `SESSION_NOT_OPEN`, `SESSION_EXPIRED`, `SETTING_OVERRIDE_DISABLED`, `SETTING_LEVEL_NOT_ALLOWED`, `SETTING_INVALID_VALUE`, `SALE_BLOCKED`, `SALE_PENDING_APPROVAL`, `APPROVAL_INSUFFICIENT_ROLE`, `CANCEL_WINDOW_EXPIRED`, `IDEMPOTENCY_PAYLOAD_MISMATCH`, `PRINTER_NOT_AVAILABLE`.

#### Scenario: Domain exception mapped to problem code

- **WHEN** `OpenSalesSessionCommand` throws `SESSION_ALREADY_OPEN`
- **THEN** the BFF controller returns HTTP 409 with `code: "SESSION_ALREADY_OPEN"` in the problem body
