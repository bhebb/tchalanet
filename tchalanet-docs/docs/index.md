# Tchalanet Docs

Tchalanet is a multi-tenant lottery and POS platform for selling tickets,
managing draws, settling results, paying winners, and operating tenant networks.

## Start here

- **Nouveau sur le projet ?** Lire [What is Tchalanet](00-overview/what-is-tchalanet.md)
- **Besoin d'une vue système ?** Lire [System map](00-overview/system-map.md)
- **Travailler sur le backend ?** Lire [Backend architecture](server-docs/ARCHITECTURE.md)
- **Travailler sur le POS / cashier ?** Lire [Sell ticket flow](02-functional/flows/sell-ticket.md)
- **Chercher les règles canoniques ?** Lire [Where truth lives](00-overview/where-truth-lives.md)

## Reading paths

=== "New to the project"

    1. [What is Tchalanet](00-overview/what-is-tchalanet.md)
    2. [System map](00-overview/system-map.md)
    3. [Sell ticket flow](02-functional/flows/sell-ticket.md)
    4. [Where truth lives](00-overview/where-truth-lives.md)

=== "Working on backend"

    1. [Backend architecture](server-docs/ARCHITECTURE.md)
    2. [Backend playbook](server-docs/PLAYBOOK.md)
    3. [Command / Query handlers](server-docs/conventions/command_query_handlers.md)
    4. [Backend conventions index](99-reference/backend-conventions.md)

=== "Working on POS / cashier"

    1. [Operational context](server-docs/conventions/context/request-context.md)
    2. [Session opening](02-functional/flows/session-opening.md)
    3. [Sell ticket flow](02-functional/flows/sell-ticket.md)
    4. [Payout flow](02-functional/flows/payout-field-flow.md)

## Main areas

### Overview
Product description, system map, and where to find authoritative information.

### Architecture
Component maps and deep-dive architecture docs for backend, web, mobile, and infra.

### Flows
Business flows — sell ticket, verify ticket, draw execution, payout, and onboarding.

### Operations
Run the local stack and deploy to staging / production.

### Reference
Backend convention index, component AGENTS maps, and canonical links.
