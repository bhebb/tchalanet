# Design — Setup AI agents (slice-first)

## Principe directeur

**Slice-only context loading.** Pour une tâche slice :

```
root/AGENTS.md          (40–50 lignes, router)
+ <project>/AGENTS.md   (≤60 lignes, router projet)
+ .agents/skills/<one>  (≤120 lignes, skill ciblé)
+ fichiers touchés
= < 500 lignes hors code
```

Pas de chargement transversal. Pas de duplication. Pas de scan global par défaut.

## Architecture fichiers

```
docs/, tchalanet-*/docs/        Vérité durable (intouchable)
openspec/, tchalanet-*/openspec/  Specs (intouchable)
VERSIONS.md                     Vérité versions

AGENTS.md (racine)              Router cross-projet, 40–50 lignes
tchalanet-*/AGENTS.md           Router projet, ≤60 lignes

.agents/                        Source canonique skills
  README.md                     Hub, ≤40 lignes
  mcp-activations.md            Log activations (vide au départ, header seul)
  skills/
    ai-safety/SKILL.md
    openspec-workflow/SKILL.md
    pr-readiness/SKILL.md
    handoff/SKILL.md
    mcp-on-demand/SKILL.md
    scoped-task/SKILL.md
    spec-scoping/SKILL.md

tchalanet-web/.agents/skills/   Skills officiels Nx/Angular (préservés)

.claude/                        Adapter Claude (pointeurs minces)
  settings.json
  skills/<name>.md              Pointeur ≤10 lignes vers .agents/skills/<name>/
  commands/                     Slash commands V1 (≤30 lignes chacune)

.codex/AGENTS.md                Adapter Codex (pointeur ≤30 lignes)
.github/copilot.md              Adapter Copilot (pointeur ≤30 lignes)
```

## Root AGENTS.md cible (template)

```md
# Tchalanet Agents Router

## Context budget

For slice work, load only:
1. this file
2. the target project AGENTS.md
3. one relevant skill from .agents/skills/
4. files being edited or reviewed

Target: <500 lines outside source code.

## Durable truth

Do not duplicate durable project rules here.
See:
- docs/
- openspec/context/
- VERSIONS.md

## Project routers

- Backend: tchalanet-server/AGENTS.md
- Web: tchalanet-web/AGENTS.md
- Mobile: tchalanet-mobile/AGENTS.md
- Edge: tchalanet-edge-service/AGENTS.md
- Infra: tchalanet-infra/AGENTS.md
- Docs/specs: tchalanet-docs/AGENTS.md

## Safety

If a task touches multiple projects, state the slices explicitly before editing.
If unsure, stop and ask for scope confirmation.
Never run global scans (grep -R, find /, tree at root) without an explicit reason.
```

Aucune section "Cross-Project Invariants" inline. Pointage vers `openspec/context/`.

## Règle anti-duplication

Tout `AGENTS.md` ou skill qui contient une règle déjà présente dans `openspec/context/` ou `docs/` doit la remplacer par un pointeur 1 ligne :

```
Voir openspec/context/10-non-negotiables.md.
```

Pas de paraphrase, pas de résumé inline.

## Règle anti-scan global

Les skills `scoped-task` et `spec-scoping` doivent contenir explicitement :

```
Do not:
- grep -R / find / tree across the monorepo by default
- load files outside the declared slice
- preload sibling project routers
```

Le scan global est autorisé uniquement si justifié (recherche d'un symbole partagé, dépendance non triviale, erreur cross-projet).

## Scope V1 — workflow skills only

V1 = skills de workflow agent uniquement. **Pas de skill métier Tchalanet.**

- ✅ `ai-safety`, `openspec-workflow`, `pr-readiness`, `handoff`, `mcp-on-demand`, `scoped-task`, `spec-scoping`.
- ❌ `sales`, `payout`, `settlement`, `dashboard`, `promotion`, `tenant`, etc.

Si une règle métier est nécessaire à une slice, l'agent la lit depuis `docs/` ou `openspec/` du projet concerné, pas depuis un skill `.agents/`.

## Format standard SKILL.md

```md
# <skill name>

## Use when
…

## Load
…

## Do
…

## Do not
…

## Output
…
```

Pas de longs exemples métier. Les exemples vivent dans `docs/` ou `openspec/context/`.

## Slash commands → skills

Chaque `.claude/commands/*.md` est un **pointeur** avec scope lock. Exemple `/backend-task` :

```md
# /backend-task

Canonical skill: .agents/skills/scoped-task/SKILL.md
Project router: tchalanet-server/AGENTS.md
Default slice: backend

Scope lock:
- Allowed: tchalanet-server/
- Forbidden unless explicitly requested: tchalanet-web/, tchalanet-mobile/, tchalanet-edge-service/, tchalanet-infra/, tchalanet-docs/

Load only:
1. AGENTS.md
2. tchalanet-server/AGENTS.md
3. .agents/skills/scoped-task/SKILL.md
4. files being edited/reviewed
```

Aucune règle backend inline. Tout passe par le skill canonique + le router projet.

## Validation humaine par fichier — étendue

Validation humaine explicite, fichier par fichier, requise pour :

- Toute modification de root `AGENTS.md`.
- Toute modification d'un per-project `AGENTS.md` existant.
- Création des adapters `.claude/`, `.codex/`, `.github/copilot.md`.
- Tout slash command qui dépasserait 30 lignes.
- Tout skill qui dépasserait 120 lignes.

Les dérives arrivent surtout dans adapters et slash commands. La validation par fichier les bloque tôt.

## Phase 1 — table d'audit attendue

```
| File | Lines | Duplicates docs/openspec? | Proposed action | Risk note | Human decision |
|---|---:|---|---|---|---|
| AGENTS.md | 107 | yes (Cross-Project Invariants ↔ 10-non-negotiables) | trim to ≤50 router | invariants visibility lost on first read | pending |
| tchalanet-server/AGENTS.md | ? | ? | ? | ? | pending |
| tchalanet-web/AGENTS.md | ? | ? (préserver bloc Nx auto-généré) | ? | ? | pending |
| tchalanet-infra/AGENTS.md | ? | ? | likely keep | ? | pending |
```

Décisions humaines : `keep` / `trim` / `rewrite` / `postpone`. Pas de gros patch aveugle.

## Phases bloquantes

Pas de saut. Chaque phase a un livrable vérifiable validé avant la suivante.

```
0 → 1 → 2 → 3 → 4 → 5 → 6
```

Phase 5+ (Nx skills supplémentaires, enchaînement Claude/Codex/Copilot, async, accès distant Mac) = changes OpenSpec séparées.

## MCP à la demande

Skill `mcp-on-demand/` détient la règle. Pas de MCP permanent. Log dans `.agents/mcp-activations.md` (table : Date / MCP / Task / Deactivation due). Audit mensuel : tout MCP non utilisé depuis 2 semaines → désactivé.

## Risques principaux

1. **Élagage trop agressif** d'`AGENTS.md` existant → règle perdue. Mitigation : audit lecture seule + validation par fichier + pointer plutôt que supprimer.
2. **Duplication subtile** entre skills et docs. Mitigation : revue chaque skill contre `docs/` et `openspec/context/`.
3. **Adapters qui dérivent** (règles inline dans `.claude/`). Mitigation : limite ≤30 lignes par command, ≤10 par pointeur skill, validation humaine si dépassement.
4. **MCP qui s'accumulent**. Mitigation : log obligatoire, audit mensuel.
5. **Skills métier qui se glissent** dans `.agents/skills/`. Mitigation : whitelist explicite V1, refus de toute autre.

## Vérification finale

```
wc -l AGENTS.md tchalanet-server/AGENTS.md .agents/skills/scoped-task/SKILL.md
wc -l AGENTS.md tchalanet-web/AGENTS.md .agents/skills/scoped-task/SKILL.md
```

Cible : < 250 lignes pour router + skill, soit budget total < 500 lignes hors code touché.
