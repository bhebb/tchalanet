# What is Tchalanet?

## What this page answers

What does Tchalanet do, and who uses it?

## Product

Tchalanet is a multi-tenant lottery and borlette platform. It enables organizations
to run lottery networks with multiple tenants, each operating their own terminal
network, draw schedule, promotions, and payout operations.

Ticket sales happen on Flutter mobile POS terminals operated by SellerTerminals,
online or offline. Draws are scheduled and results are ingested from external
providers. Winners claim payouts at field terminals.

## Who uses it

| Actor | What they do |
| --- | --- |
| **SellerTerminal** | Sells tickets from a POS device — authenticated via Firebase PIN |
| **Tenant admin** | Manages seller-terminals, draws, promotions, limits, and reporting |
| **Admin POS** | Tenant admin who selected a SellerTerminal to perform POS operations |
| **Super-admin** | Manages tenants and cross-tenant platform operations |
| **End customer** | Buys tickets, checks results publicly |

## Main capabilities

- **Ticket sales** — online and offline sales on Flutter terminals
- **Draws** — scheduled draw lifecycle; result ingestion from external providers
- **Payouts** — field payout by cashiers after ticket verification
- **Promotions** — campaign engine: free lines, odds boost, charge waiver
- **Multi-tenant isolation** — full data isolation via Postgres RLS
- **Reconciliation** — daily sales vs. draw result reconciliation
- **Analytics** — real-time stats and tenant reporting dashboards

## Where to go next

- [System map](system-map.md) — how the components connect
- [Sell ticket flow](../02-functional/flows/sell-ticket.md) — the most critical flow
- [Backend architecture](../server-docs/ARCHITECTURE.md) — how the backend is structured
