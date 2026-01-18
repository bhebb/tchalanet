# OpenSpec — Context Packs Index

This file is a **router** for context packs.

Purpose:

- Load only what is needed for a feature
- Avoid flooding AI or developers with irrelevant context
- Point to the real sources of truth (docs near code & MkDocs)

---

## Mandatory

These packs are required for **every feature**.

- `05-version-guard.md` — Runtime & framework enforcement (MUST)
- `10-non-negotiables.md`  
  Global invariants, architecture rules, hard constraints.

---

## Technical packs (pick ONE if relevant)

Choose **only the technical scope you are touching**.

- `20-backend-rules.md`  
  Java 25, Spring Boot 4, persistence, CQRS, APIs, RLS.

- `30-frontend-rules.md`  
  Angular 20, Nx, theming, i18n, widgets, layout rules.

- `40-mobile-rules.md`  
  Ionic, Capacitor, mobile constraints.

- `50-edge-service-rules.md`  
  Edge services, Node, routing, lightweight APIs.

- `60-infra-rules.md`  
  Docker, Traefik, Keycloak, CI/CD, environments.

- `75-catalog-rules.md`  
  Referential data, read-mostly models, catalog boundaries.

---

## Domain packs (pick ONE, optional)

Load a domain pack **only if the feature touches domain logic**.

Examples:

- `70-domain-draw.md`
- `71-domain-sales.md`
- `72-domain-payout.md`
- `73-domain-ledger.md`
- `74-domain-limitpolicy.md`

Domain packs:

- describe business rules
- do NOT duplicate implementation details
- always point to DOMAIN\_\*.md files in the backend

---

## Glossary (optional)

- `90-glossary.md`  
  Load only if vocabulary clarification is needed.

---

## Usage rule (strict)

For each feature spec:

- Load **2–4 packs max**
- MUST include:
  - `10-non-negotiables.md`
- THEN:
  - at most **one technical pack**
  - at most **one domain pack**
- Never load everything

❌ Loading unnecessary context is considered an error.

---

## Source of truth

Context packs summarize rules.  
They never replace the real documentation.

Canonical sources:

- Backend: `tchalanet-server/docs/` + `src/**/DOMAIN_*.md`
- Web: `apps/tchalanet-web/*.md` + `libs/**/README.md`
- Infra: `tchalanet-infra/docs/`
- Central docs (published): `tchalanet-docs/docs/`
