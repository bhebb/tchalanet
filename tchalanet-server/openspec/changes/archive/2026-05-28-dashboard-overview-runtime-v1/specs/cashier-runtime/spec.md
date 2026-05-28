# Spec: Cashier Runtime V1

## ADDED Requirements

### Requirement: POS/mobile cashier uses compact home endpoint

POS/mobile cashier SHALL use:

```http
GET /tenant/cashier/home
```

It SHALL NOT use the full dashboard PageModel.

### Requirement: POS/mobile home content

| Category | Displayed on POS home |
|---|---|
| Identity | seller, tenant, outlet, terminal labels |
| Operational context | ready, trusted, source, missing fields |
| Session | open/closed, openedAt, ticket count, sales total |
| Primary draw | draw label, cutoff, status |
| Primary action | sell, open session, configure operational context |
| Navigation | sell, tickets, session, profile |
| Notices | operational blockers/warnings |

### Requirement: Cashier web dashboard uses PageModel source

Cashier web dashboard SHALL use provider source:

```text
cashier_dashboard
```

Dashboard table:

| Category | Displayed on cashier web dashboard | Source |
|---|---|---|
| Identity | cashier/outlet/terminal | `cashier_dashboard` |
| Readiness | operational readiness/trusted status | `cashier_dashboard` |
| Session | session summary | `cashier_dashboard` |
| Primary action | sell ticket | `cashier_dashboard` |
| Draws | next draws / primary draw | `cashier_dashboard` |
| Tickets | recent tickets bounded list | `cashier_dashboard` |
| Alerts | operational warnings | `cashier_dashboard` |

### Requirement: Cashier has no overview V1

Cashier V1 SHALL NOT introduce a generic cashier overview.

#### Scenario: Cashier needs detailed session

- **WHEN** cashier clicks session
- **THEN** frontend opens `/app/cashier/session`
- **AND** the session page calls owner/session endpoints.
