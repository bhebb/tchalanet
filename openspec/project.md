# Tchalanet — Project Context (OpenSpec)

This file is intentionally **short and stable**.

It defines the global project context and **routes AI and contributors**
to the correct detailed documentation.
It is **not** a full specification.

---

## Purpose

Tchalanet is a **multi-tenant lottery / borlette platform** composed of:

- Backend (Spring Boot / Java)
- Web frontend (Angular / Nx)
- Mobile app (Ionic / Capacitor)
- Infra & edge services (Docker, Traefik, Keycloak, etc.)

Core capabilities:

- Ticket sales & validation
- Draw scheduling & result ingestion
- Payouts & ledger
- Tenant configuration, theming & i18n
- Public & private dashboards

---

## Global non-negotiables

These rules apply to **all features**, **all modules**, and **all agents**.

- Backend architecture: **Hexagonal + CQRS** for critical domains
- **Multi-tenant by default**, enforced with PostgreSQL **RLS**
- Strict module boundaries:
  - `common/` → technical transversal
  - `catalog/` → shared referentials (read-mostly)
  - `core/` → critical business domains
  - `features/` → orchestration / BFF / vertical slices
- No business logic in controllers
- Strongly typed IDs (wrappers everywhere except persistence)
- Frontend:
  - Angular 20 + Nx
  - Mobile-first
  - Token-based theming (CSS vars)
- **No undocumented rules**
- **No deprecated APIs or features** when newer supported alternatives exist

Detailed rules are defined in context packs and near-code documentation.

---

## Context packs

Context packs are **loaded selectively**.
Do **not** load everything.

### Entry point

- Index & routing:  
  `openspec/context/00-index.md`

### Always required

- Global rules & invariants:  
  `openspec/context/10-non-negotiables.md`

### Technical scopes (load only if relevant)

- Backend rules (Java / Spring / persistence / API):  
  `openspec/context/20-backend-rules.md`
- Frontend rules (Angular / Nx / theming / i18n):  
  `openspec/context/30-frontend-rules.md`
- Mobile rules (Ionic / Capacitor):  
  `openspec/context/40-mobile-rules.md`
- Edge service rules:  
  `openspec/context/50-edge-service-rules.md`
- Infra & DevOps rules:  
  `openspec/context/60-infra-rules.md`
- Catalog / referentials rules:  
  `openspec/context/75-catalog-rules.md`

### Domain packs

- Domain-specific context:  
  `openspec/context/70-<domain>.md`

### Shared language

- Glossary:  
  `openspec/context/90-glossary.md`

---

## Detailed documentation (source of truth)

OpenSpec **never duplicates** detailed documentation.
It always points to the canonical source.

### Backend

- Architecture & conventions:  
  `tchalanet-server/docs/`
- Domain documentation:  
  `tchalanet-server/src/**/DOMAIN_*.md`
- Feature documentation:  
  `tchalanet-server/src/**/FEATURE_*.md`
- Global architecture reference:  
  `ARCHITECTURE.md`
- AI & contribution rules:  
  `AGENTS.md`
- Versions & dependencies:  
  `VERSIONS.md`

### Web

- App-level docs:  
  `apps/tchalanet-web/*.md`
- Libraries & UI components:  
  `libs/**/README.md`

### Infra

- Infra & ops documentation:  
  `tchalanet-infra/docs/`

### Central published documentation

- Human-oriented reference (MkDocs):  
  `tchalanet-docs/docs/`

---

## Source of truth hierarchy

1. **Near-code documentation** (server / web / infra)  
   → implementation truth
2. **Central documentation (MkDocs)**  
   → architecture, business, flows, ADRs
3. **OpenSpec context packs**  
   → routing + constraints for AI and SDD
4. **Feature specs (`openspec/specs/`)**  
   → temporary design & execution workspace

---

## Usage rule (mandatory)

Each feature specification **MUST**:

1. Reference **2–4 context packs max**
2. Always include:
   - `10-non-negotiables.md`
3. Then include **only what is relevant**:
   - backend / frontend / infra
   - one or more domain packs
4. Explicitly point to **near-code docs** when applicable

Example:

```text
Context packs:
- 10-non-negotiables.md
- 20-backend-rules.md
- 70-sales.md

Near-code references:
- tchalanet-server/src/main/java/.../DOMAIN_SALES.md
- tchalanet-server/docs/conventions/persistence.md
```
