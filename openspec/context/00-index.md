# OpenSpec — Context Packs Index

This file is a router for context packs.

Purpose:

- Load only the context needed for a feature.
- Avoid flooding AI agents or developers with irrelevant context.
- Point to the real sources of truth: docs near code, backend domain docs, infra docs, and MkDocs.

---

## Mandatory packs

Load these for every feature.

- `05-version-guard.md` — Runtime and framework version enforcement.
- `tchalanet-server/openspec/context/10-non-negotiables.md` — Architecture layers, dependency graph, hard constraints (backend). Source unique — ne pas dupliquer ici.

---

## Technical packs

Pick at most one technical pack when relevant.

- `20-backend-rules.md` — Java 25, Spring Boot 4, persistence, CQRS, APIs, RLS.
- `30-frontend-rules.md` — Angular 20, Nx, theming, i18n, widgets, layout rules.
- `40-mobile-rules.md` — Flutter/mobile app constraints.
- `50-edge-service-rules.md` — Edge service, Node/Fastify, routing, lightweight APIs.
- `60-infra-rules.md` — Docker, Traefik, Keycloak, CI/CD, environments.
- `75-catalog-rules.md` — Referential data, read-mostly models, catalog boundaries.
- `80-core-rules.md` — Core domain/application rules, lifecycle logic, commands, events.
- `81-feature-rules.md` — Feature/BFF rules, vertical slices, public/private composition.

---

## Domain packs

Pick at most one domain pack only when the feature touches domain logic.

Examples:

- `70-domain-draw.md`
- `71-domain-sales.md`
- `72-domain-payout.md`
- `73-domain-ledger.md`
- `74-domain-limitpolicy.md`

Domain packs:

- describe business rules;
- do not duplicate implementation details;
- point to backend `DOMAIN_*.md` files where deeper details live.

---

## Glossary

Optional.

- `90-glossary.md` — Load only when vocabulary clarification is needed.

---

## Usage rule

For each feature spec, load 2 to 4 packs max.

Always load:

- `05-version-guard.md`
- `tchalanet-server/openspec/context/10-non-negotiables.md`

Then load only what is needed:

- at most one technical pack;
- at most one domain pack;
- optional glossary only if terminology is unclear.

Do not load every pack.

Loading unnecessary context is considered an error.

---

## Component OpenSpecs

Each component/project may have its own OpenSpec workspace.

Examples:

- backend: `tchalanet-server/openspec/`
- web: `apps/tchalanet-web/openspec/`
- mobile: `tchalanet-mobile/openspec/`
- edge: `tchalanet-edge-service/openspec/`
- infra: `tchalanet-infra/openspec/`
- docs: `tchalanet-docs/openspec/`

The global OpenSpec context must stay light. It is a routing layer, not a full documentation copy.

Component OpenSpecs may contain detailed context closer to the code.

---

## Source of truth

Context packs summarize rules. They never replace canonical documentation.

Canonical sources:

- Backend: `tchalanet-server/docs/` and `src/**/DOMAIN_*.md`
- Web: `tchalanet-web/**/README.md` and app/lib docs
- Mobile: `tchalanet-mobile/**/README.md` and app docs
- Edge: `tchalanet-edge-service/docs/`
- Infra: `tchalanet-infra/docs/`
- Published docs: `tchalanet-docs/docs/`
- Runtime versions: `VERSIONS.md`
