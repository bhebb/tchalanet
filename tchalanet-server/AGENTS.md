# AGENTS.md — Tchalanet Server

Backend agent router for `tchalanet-server/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`
- `openspec/context/10-non-negotiables.md`

Canonical local docs:

- `docs/ARCHITECTURE.md`
- `docs/PLAYBOOK.md`
- `docs/NAMING.md`
- `docs/conventions/`
- `src/**/DOMAIN_*.md`
- `src/**/FEATURE_*.md`

OpenSpec:

- Use `tchalanet-server/openspec/` for backend changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- `./mvnw test`
- `./mvnw verify`
- Relevant focused tests for touched packages.

Context rule:

- Load root rules, local `CLAUDE.md`, one relevant convention pack, and near-code docs for the touched domain.

DB migrations (NORMATIVE — voir `docs/conventions/persistence/persistence.md` §9):

- **Pré-go-live** : ne pas créer de nouveau `V*.sql`. Absorber les évolutions dans la migration d'origine (`CREATE TABLE`, index, trigger, RLS, vue, seed).
- **Avant tout nouveau fichier de migration**, demander confirmation explicite à l'utilisateur.
- **Cible** : `V001` + `V100-V108` (schéma + vues) + `V200-V209` (seeds).
- **Vues read-model** vivent dans `V108__create_read_views.sql`. Toute modification de table doit vérifier les 3 vues : `v_ticket_summary`, `v_ticket_print`, `v_draw_summary`.
- **Synchro obligatoire** à chaque modification de table : entités JPA + mappers/projections + tables `_aud` (si `@Audited`) + vues V108 + seeds. Tolérance temporaire en PR adjacente, mais jamais laissé en dette.
