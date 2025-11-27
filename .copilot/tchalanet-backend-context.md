# Tchalanet – Backend Architecture Context (for Copilot)

## Project Overview

Tchalanet is a **multi-tenant lottery / borlette platform** with:

- Public page (B2B SaaS marketing + public draws/results widgets)
- Private dashboards:
  - **Super Admin (platform)**
  - **Tenant Admin (operator)**
  - **Vendor / Cashier**
- Batch jobs for:
  - Generating upcoming draws per tenant
  - Fetching & applying external results (US lotteries like NY / Florida)
- Dynamic theming per tenant
- Future POS + cashier sessions, offline support, public ticket verification, etc.

### Tech Stack

- **Backend**: Java 21, Spring Boot 3.x, PostgreSQL, Flyway, Redis, Meilisearch
- **Auth**: Keycloak (OIDC)
- **Feature flags**: Unleash
- **Architecture**: Hexagonal / Ports & Adapters + DDD-lite + CQRS

This file gives high-level context. Detailed code rules are in:
- `tchalanet-backend-code-style.md`
- `tchalanet-frontend-angular-guide.md`