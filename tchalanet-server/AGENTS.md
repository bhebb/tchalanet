# AGENTS.md — Tchalanet Server

Backend agent router for `tchalanet-server/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`
- `openspec/context/10-non-negotiables.md`

Canonical local docs:

- `docs/ARCHITECTURE.md` — Layer structure, hexagonal pattern, CQRS, Command/Query Bus, cross-domain calls
- `docs/PLAYBOOK.md` — Operational workflows, handler templates, inter-domain patterns
- `docs/NAMING.md` — Naming conventions
- `docs/conventions/` — Technical standards (clean architecture, bus patterns, context, RLS, etc.)
- `src/**/DOMAIN_*.md` — Domain-specific invariants and lifecycle rules
- `src/**/FEATURE_*.md` — Feature-specific orchestration flows

OpenSpec:

- Use `tchalanet-server/openspec/` for backend changes.
- Use `openspec/context/80-core-rules.md` for core module architecture (layers, ports, CQRS).
- Use `openspec/context/81-features-rules.md` for feature module architecture (vertical slices, orchestration).
- Use root `openspec/` only for cross-project coordination.

Core Architecture Rules (KEY DECISION):

- **Mandatory layering** (domain → application → infra, NO reverse dependencies)
- **Typed IDs everywhere** (UUID only in persistence)
- **CommandBus / QueryBus only** (no port.in, no direct handler calls)
- **Events after commit** (AfterCommit.run)
- **Clean separation**: domain is pure, application orchestrates, infra adapts
- **Cross-domain via queries or events** (never direct repository access)

Feature Architecture Rules (KEY DECISION):

- **Vertical slices** (NOT hexagonal)
- **Orchestration only** (no business invariants)
- **Depends on core/catalog** (never vice versa)
- **Use CommandBus/QueryBus** (no direct core repository access)

Validation:

- `./mvnw test`
- `./mvnw verify`
- Relevant focused tests for touched packages.
- `archUnit` enforces layer and dependency rules

Context rule:

- Load root rules (`AGENTS.md`, `../AGENTS.md`), local `CLAUDE.md`, relevant convention pack (`command_query_handlers.md`, `clean_architecture.md`, `inter_domain_calls.md`), and near-code `DOMAIN_*.md` or `FEATURE_*.md` for touched area.
- Prefer reading narrow scopes over whole-repo audits.

DB migrations (NORMATIVE — voir `docs/conventions/persistence.md` §9):

- **Pré-go-live** : ne pas créer de nouveau `V*.sql`. Absorber les évolutions dans la migration d'origine (`CREATE TABLE`, index, trigger, RLS, vue, seed).
- **Avant tout nouveau fichier de migration**, demander confirmation explicite à l'utilisateur.
- **Cible** : `V001` + `V100-V108` (schéma + vues) + `V200-V209` (seeds).
- **Vues read-model** vivent dans `V108__create_read_views.sql`. Toute modification de table doit vérifier les 3 vues : `v_ticket_summary`, `v_ticket_print`, `v_draw_summary`.
- **Synchro obligatoire** à chaque modification de table : entités JPA + mappers/projections + tables `_aud` (si `@Audited`) + vues V108 + seeds. Tolérance temporaire en PR adjacente, mais jamais laissé en dette.
