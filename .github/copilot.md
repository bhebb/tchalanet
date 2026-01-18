# Copilot Context — Tchalanet (Monorepo)

This file helps Copilot find the right context fast.
It is an index + non-negotiable rules + pointers (NOT full docs).

## Read first (mandatory)

1. `AGENTS.md` (root) — global rules & invariants (server/web/mobile/infra/edge)
2. `VERSIONS.md` (root) — pinned versions (runtime/build/images) + update policy
3. `openspec/project.md` — short project context (index + non-negotiables)
4. `openspec/context/00-index.md` — context packs index
5. `DOCUMENTATION.md` (root) — documentation hub (where to find what)

## Backend architecture (NON-NEGOTIABLE)

Backend has FOUR strict layers:

- `common/` = technical transversal (NO business rules)
- `catalog/` = reference/lookup data (read-mostly, minimal business logic)
- `core/` = critical business domains (hexagonal + CQRS)
- `features/`= vertical slices / orchestration (BFF, pages, aggregation)

Rules:

- `core/` MUST NOT depend on `features/`
- `features/` may orchestrate `core/` and read from `catalog/`
- `common/` MUST NOT contain business logic

## Version-aware coding + Deprecated Guard (MUST)

- Always check `VERSIONS.md` before using framework/library APIs.
- Generate code compatible with the pinned versions only.
- Do NOT introduce deprecated APIs/classes/config.
- If unavoidable: add a short WHY comment + create an ADR if impact is wide.

## Before generating code (AI checklist)

- [ ] Identify scope: server / web / mobile / infra / edge-service
- [ ] Read `AGENTS.md` + `VERSIONS.md`
- [ ] Load only relevant context packs (2–4 max) from `openspec/context/`
- [ ] Read near-code docs:
  - Backend domain: `tchalanet-server/src/**/DOMAIN_*.md`
  - Backend feature: `tchalanet-server/src/**/FEATURE_*.md`
  - Backend conventions: `tchalanet-server/docs/conventions/*`
  - Web entry: `apps/tchalanet-web/README.md` + `libs/**/README.md`
  - Infra: `tchalanet-infra/docs/**`
- [ ] Follow existing patterns; propose at most 2 options if ambiguous.

## Where to find conventions (source of truth)

### Backend

- `tchalanet-server/docs/ARCHITECTURE.md`
- `tchalanet-server/docs/rls.md`
- `tchalanet-server/docs/ROUTING_AND_API_PATHS_V1.md`
- `tchalanet-server/docs/conventions/`

### Web

- `apps/tchalanet-web/README.md`
- `libs/ui/**/README.md`, `libs/web/**/README.md`, `libs/shared/**/README.md`

### Infra / Edge

- `tchalanet-infra/README.md` + `tchalanet-infra/docs/**`
- `tchalanet-edge-service/README.md`

### Central docs (MkDocs)

- `tchalanet-docs/docs/00-guidelines/*`
- `tchalanet-docs/docs/01-architecture/*`
- `tchalanet-docs/docs/02-functional/*`
- `tchalanet-docs/docs/03-adr/*`

## OpenSpec workflow (SDD)

- Project context: `openspec/project.md`
- Context packs: `openspec/context/*`
- Feature specs: `openspec/specs/`
