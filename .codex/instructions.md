# Codex Instructions — Tchalanet

## Canonical Sources

- global rules: `AGENTS.md`
- versions: `VERSIONS.md`
- OpenSpec workflow: `openspec/project.md` + `openspec/context/*`
- backend documentation: `tchalanet-server/docs/` + `tchalanet-server/src/**/DOMAIN_*.md`
- central documentation: `tchalanet-docs/docs/`
- Claude alignment: `CLAUDE.md` + scope `CLAUDE.md` files

## OpenSpec Context Loading

- start with `openspec/context/00-index.md`
- always include `openspec/context/10-non-negotiables.md`
- then load only the packs relevant to the task
- keep context loading to `2-4` packs max

Typical routing:

- backend work: `20-backend-rules.md`
- web work: `30-frontend-rules.md`
- mobile work: `40-mobile-rules.md`
- infra work: `60-infra-rules.md`
- catalog-heavy work: `75-catalog-rules.md`

## Architecture

- backend: Spring Boot DDD with `common / catalog / core / features`
- frontend web: Angular 20 in the Nx monorepo
- mobile: Flutter is the target client architecture to preserve

## Critical Rules

- OpenSpec is mandatory before implementing a feature or architectural change
- business logic lives in the backend only
- web and mobile are clients of backend contracts and OpenSpec specs
- do not duplicate business logic in Angular or Flutter
- keep specs UI-agnostic so web and mobile can share the same source of truth
- use OpenSpec context packs as routing and constraint sources, not as duplicated implementation docs

## Before Commit

- `pnpm ops:check`
- `pnpm lint`
- `pnpm test`
