# AI Entry — tchalanet-server (Copilot / ChatGPT)

This file is a small index to the project-wide AI rules and context.
When working in **tchalanet-server/**, always start here.

---

## 1) Mandatory reads (before generating code)

1. `../AGENTS.md` — global rules for the whole monorepo (placement, invariants)
2. `../VERSIONS.md` — version guard (use features of the pinned versions, avoid deprecated APIs)
3. `./docs/ARCHITECTURE.md` — backend architecture (`common/`, `catalog/`, `core/`, `features/`)
4. `./docs/conventions/` — server conventions (API, persistence, ids, pagination, security, testing)

---

## 2) OpenSpec workflow (features)

- Feature specs live here: `../openspec/specs/`
- Context packs (load 2–4 max): `../openspec/context/`

**Rule**: each feature spec MUST reference:

- `openspec/context/10-non-negotiables.md`
- plus only relevant packs (backend/catalog/infra + domain)

---

## 3) Central published documentation (MkDocs)

Stable cross-module docs (guidelines, architecture maps, functional docs, ADR):

- `../tchalanet-docs/docs/00-guidelines/`
- `../tchalanet-docs/docs/01-architecture/`
- `../tchalanet-docs/docs/02-functional/`
- `../tchalanet-docs/docs/03-adr/`

---

## 4) Where to put documentation updates

- If **stable/shared rule** → `tchalanet-docs/docs/00-guidelines/` or `01-architecture/`
- If **business domain/workflow** → `tchalanet-docs/docs/02-functional/`
- If **important technical decision** → `tchalanet-docs/docs/03-adr/`
- If **implementation detail** → `tchalanet-server/docs/**` or `src/**/DOMAIN_*.md`
- If **feature in progress** → `openspec/specs/FEAT-*/`

---

## 5) Quick checklist (server)

- [ ] Correct layer? (`common/`, `catalog/`, `core/`, `features/`)
- [ ] Typed IDs used outside JPA/repositories?
- [ ] Multi-tenant enforced (context + RLS)?
- [ ] API responses follow `ApiResponse<T>` + `TchPage<T>`?
- [ ] No deprecated APIs (check `VERSIONS.md`)?
- [ ] Events published after commit (`AfterCommit`)?
- [ ] Tests use AssertJ only?

---
