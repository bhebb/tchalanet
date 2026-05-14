# Tchalanet — Claude Instructions

## OpenSpec — sélection projet-local

Chaque projet autonome possède son propre OpenSpec. Utiliser **toujours** le plus proche :

| Projet                    | OpenSpec                           |
| ------------------------- | ---------------------------------- |
| Backend Spring Boot       | `tchalanet-server/openspec/`       |
| Frontend Angular          | `tchalanet-web/openspec/`          |
| Mobile Flutter / POS      | `tchalanet-mobile/openspec/`       |
| Edge Service              | `tchalanet-edge-service/openspec/` |
| Infrastructure            | `tchalanet-infra/openspec/`        |
| Documentation             | `tchalanet-docs/openspec/`         |
| Coordination cross-projet | `openspec/` (racine)               |

Ne jamais charger le contexte de tous les projets par défaut.

## Règles agent (NON-NÉGOCIABLES)

- **Ne pas modifier plusieurs projets en même temps** sauf demande explicite.
- **Ne jamais archiver manuellement** un change OpenSpec avec `rm`, `cp`, ou `mv`. Utiliser exclusivement le CLI depuis le répertoire du projet concerné :
  ```bash
  cd <project-root>
  openspec archive <change-id> --yes
  ```
- **Vérification worktree / sandbox obligatoire** avant toute analyse ou édition :
  ```bash
  pwd
  git rev-parse --show-toplevel
  git branch --show-current
  git status --short
  git log -1 --oneline
  find . -maxdepth 4 -type d -name openspec
  ```
  Si le worktree est obsolète, détaché, sur la mauvaise branche, ou sans OpenSpec → arrêter et signaler.
- **Discipline token** : ne charger que les fichiers explicitement mentionnés par la tâche. Ne pas scanner tout le repo. Demander avant d'élargir le scope.

---

Goal:

- Minimize token usage.
- Do not scan the whole repository.
- Work by slice.
- Prefer local patterns over new abstractions.

Source of truth:

- Versions: `VERSIONS.md`
- Global agent rules: `AGENTS.md`
- Backend architecture: `tchalanet-server/docs/ARCHITECTURE.md`
- Backend playbook: `tchalanet-server/docs/PLAYBOOK.md`
- OpenSpec workflow: `openspec/AGENTS.md`

Before coding:

- Read only the nearest relevant `CLAUDE.md`.
- Read only files explicitly mentioned by the task.
- Load at most one nearby `DOMAIN_*.md`, `FEATURE_*.md`, or convention doc if required.
- Before editing, list files you will touch.

Token policy:

- Do not read all docs.
- Do not load all conventions.
- Do not inspect unrelated apps.
- Ask before expanding scope.
- Use `/handoff` before `/clear`.

Architecture:

- `common`: technical only.
- `catalog`: reference/read-mostly data.
- `core`: business domains and invariants.
- `features`: BFF/orchestration only.

Non-negotiables:

- No raw UUID outside persistence.
- Use typed IDs.
- Controllers stay thin.
- Use CommandBus / QueryBus when handlers exist.
- Write handlers need transaction boundary.
- Cross-domain side effects happen after commit.
- Do not change versions unless `VERSIONS.md` is updated.
- Do not invent architecture when a local pattern exists.

OpenSpec:

- If the task is planning, proposal, architecture, breaking change, security, performance, or large refactor, read `openspec/AGENTS.md` first.
- Otherwise do not load OpenSpec by default.

Output for implementation tasks:

1. Files inspected
2. Files changed
3. Tests run
4. Risks
5. Compact handoff
