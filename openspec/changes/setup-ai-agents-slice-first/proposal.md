# Change: Setup AI agents — slice-first context

## Decision

Tchalanet AI agent setup becomes **slice-first**.

- `AGENTS.md` files are **routers only**. Durable rules stay in `docs/` and `openspec/context/`.
- The canonical agent workflow source is `.agents/skills/`.
- Claude, Codex, Copilot adapters stay **thin pointers** and must not duplicate rules.
- For a slice task, an agent loads only: root `AGENTS.md` + target project `AGENTS.md` + one relevant skill + touched files.
- **Target budget: <500 lines outside source code.**
- MCP is on-demand only. No permanent MCP by default. Every activation is logged and reviewed monthly.
- Existing `tchalanet-web/.agents/skills/` Nx/Angular official skills are preserved unchanged.

## Why

Previous AI agent configs were cleared to start clean. Without a slice-first principle baked into routers and skills, agents drift back to loading the whole monorepo, duplicating rules between `AGENTS.md` and `docs/`, and growing slash commands into ad-hoc rule dumps.

State after cleanup :

- `AGENTS.md` racine (~107 lignes) — duplicates `openspec/context/10-non-negotiables.md`. Must be rewritten as a thin router (40–50 lines target).
- `tchalanet-server/AGENTS.md`, `tchalanet-web/AGENTS.md`, `tchalanet-infra/AGENTS.md` — likely contain inline rules to trim.
- `tchalanet-web/.agents/skills/` — official Nx/Angular skills, preserved.
- Manquent : `tchalanet-mobile/AGENTS.md`, `tchalanet-edge-service/AGENTS.md`, `tchalanet-docs/AGENTS.md`, hub `.agents/` racine, adapters `.claude/`/`.codex/`/`.github/copilot.md` racine.

## What

1. **Audit en lecture seule** des 4 `AGENTS.md` existants. Produire une table de décision par fichier (lignes / duplications / action proposée / décision humaine).
2. **Élagage validé fichier par fichier** (un commit par `AGENTS.md`). Cible : racine 40–50 lignes, per-project ≤60 lignes.
3. **Hub `.agents/` racine** avec 7 skills d'emblée (les 5 shared + `scoped-task` + `spec-scoping` requis par les slash commands) + `.agents/mcp-activations.md` (vide avec header, pour rendre la règle concrète).
4. **Créer les 3 `AGENTS.md` manquants** (mobile, edge-service, docs), routeurs purs.
5. **Adapters minces racine** : `.claude/` (settings + skills pointeurs + 5 commands), `.codex/AGENTS.md` (pointeur ≤30 lignes), `.github/copilot.md` (pointeur ≤30 lignes).
6. **5 slash commands V1** dans `.claude/commands/` (`spec`, `backend-task`, `web-task`, `mobile-task`, `ready-check`) — chacun ≤30 lignes, **pointe** vers un skill canonique, contient un **scope lock explicite**.
7. **Mesure réelle finale** : `wc -l` sur slice backend et slice web, vérifier < 250 lignes pour router + skill.

## Scope V1 — workflow skills only

V1 autorise uniquement des **skills de workflow agent**, **pas des skills métier Tchalanet**.

- ✅ Autorisé : `ai-safety`, `openspec-workflow`, `pr-readiness`, `handoff`, `mcp-on-demand`, `scoped-task`, `spec-scoping`.
- ❌ Interdit : skill `sales`, `payout`, `settlement`, `dashboard`, `promotion`, `tenant`, ou tout skill qui encode une règle métier. La règle métier reste dans `docs/` et `openspec/`.

## Impact

- Configuration agents AI uniquement. Pas de changement runtime.
- Budget contexte par slice mesurable et plafonné.
- Adapters `.claude/`/`.codex/`/`.github/` deviennent des pointeurs vérifiables.
- Couvre les 6 projets : server, web, mobile, edge-service, infra, docs.

## Non-goals

- Pas de skill métier Tchalanet en V1.
- Pas d'automatisation PR (`/to-pr`, `/from-trello` différées).
- Pas de Trello MCP ni Slack MCP au démarrage.
- Pas d'async (Phase ultérieure, change séparée).
- Pas de modification de `docs/`, `openspec/specs/`, `VERSIONS.md`, ni de `tchalanet-web/.agents/skills/`, ni de `tchalanet-web/.claude/`.

## Principles

- **Slice-first** : charger le minimum nécessaire à la tâche.
- **No global scan by default** : pas de `grep -R`, `find /`, `tree` sur tout le monorepo. Commencer par le router projet + fichiers demandés. Recherche globale uniquement si justifiée (dépendance, symbole, erreur).
- **Source canonique unique** : `.agents/skills/` détient les skills, les adapters pointent.
- **Pointer plutôt que paraphraser** : tout `AGENTS.md`/skill avec une règle déjà dans `docs/` ou `openspec/context/` la remplace par un pointeur 1 ligne.
- **Validation humaine par fichier** pour :
  - Toute modification de root `AGENTS.md` (élagage ou réécriture).
  - Toute modification d'un per-project `AGENTS.md` existant.
  - Création des adapters `.claude/`/`.codex/`/`.github/copilot.md`.
  - Tout slash command qui dépasserait 30 lignes.
  - Tout skill qui dépasserait 120 lignes.

## Implementation guardrails

- Phase 1 audit is read-only. Do not edit files during audit.
- Pointers must be one-line repo-relative paths.
- Do not modify runtime, dependency, build, wrapper, Docker, or version files.
- Do not touch out-of-scope files unless explicitly instructed.
- Do not duplicate backend architecture rules in `tchalanet-server/AGENTS.md`; point to `docs/ARCHITECTURE.md`, `docs/PLAYBOOK.md`, `VERSIONS.md`, and `docs/conventions/*`.
- Do not create business/domain skills. Workflow skills only.
- Do not globally scan the repo by default. Use targeted reads first.

## Out-of-scope files

- `docs/`, `tchalanet-*/docs/`, `tchalanet-*/openspec/`, `openspec/specs/`, `VERSIONS.md` : intouchables sauf instruction explicite.
- `tchalanet-web/.agents/skills/*` et `tchalanet-web/.claude/*` : conservés tels quels.
