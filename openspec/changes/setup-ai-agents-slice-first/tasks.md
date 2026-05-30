# Tasks

> Phases bloquantes. Pas de saut sans validation humaine du livrable de la phase précédente.

**Statut global : ✅ V1 livrée — PR #107 mergée sur `main` le 2026-05-30.**

## Phase 0 — Freeze scope

- [x] Créer la branche `chore/ai-agent-setup`. (déjà sur la branche)
- [x] Confirmer la liste des fichiers intouchables (`docs/`, `openspec/specs/`, `VERSIONS.md`, `tchalanet-web/.agents/skills/`, `tchalanet-web/.claude/`).
- [x] Confirmer la liste des `AGENTS.md` existants (root, server, web, infra).
- [x] Confirmer la liste des `AGENTS.md` manquants (mobile, edge-service, docs) — supprimés mais récupérables depuis HEAD.
- [x] Auditer `.gitignore`. Gaps proposés (non appliqués) : `.env*` manquant dans server + mobile ; `tchalanet-docs/.gitignore` absent (proposer `site/`, `.DS_Store`, `.env*`). Root/web/edge/infra OK.
- [ ] Test refus : humain demande commande destructive → agent refuse. (à exécuter par l'humain)

**Livrable** : `.agents/skills/ai-safety/SKILL.md` créé (55 l.).

## Phase 1 — Audit en lecture seule

> Ne modifier aucun fichier dans cette phase.

- [x] Lire les 4 `AGENTS.md` existants + `tchalanet-server/openspec/context/10-non-negotiables.md`.
- [x] Table d'audit produite et décidée :

  | File | Lines | Duplicates docs/openspec? | Action | Decision |
  |---|---:|---|---|---|
  | AGENTS.md | 101 | yes (Cross-Project Invariants ↔ 10-non-negotiables) + réf cassée | rewrite ≤50 router | **rewrite** |
  | tchalanet-server/AGENTS.md | 63 | yes (archi ↔ ARCHITECTURE.md ; DB ↔ persistence §9) | trim, pointeurs | **trim** |
  | tchalanet-web/AGENTS.md | 57 | non (router OK + bloc Nx) | keep | **keep** |
  | tchalanet-infra/AGENTS.md | 32 | non | keep | **keep** |

- [x] Décisions confirmées par l'humain. Routers manquants : **réécrire de zéro**. CLAUDE.md : **pointeur minimal**.

**Livrable** : table d'audit + décisions enregistrées.

## Phase 2 — Élagage validé

- [x] `AGENTS.md` racine → rewrite 101→51 l., réf cassée corrigée (`tchalanet-server/openspec/context/10-non-negotiables.md`).
- [x] `tchalanet-server/AGENTS.md` → trim 63→40 l., réfs vérifiées/corrigées (`docs/conventions/persistence/persistence.md`).
- [x] `tchalanet-web/AGENTS.md` → keep (bloc Nx préservé).
- [x] `tchalanet-infra/AGENTS.md` → keep.
- [x] CLAUDE.md pointeurs minimaux créés (racine + 6 projets), sens unique → AGENTS.md.

**Note** : tous les changements committés dans `chore/ai-agent-setup` → PR #107 mergée sur `main` (2026-05-30).

## Phase 3 — Hub .agents/

- [x] `.agents/README.md` (28 l.).
- [x] `.agents/skills/ai-safety/SKILL.md` (55 l.).
- [x] `.agents/skills/openspec-workflow/SKILL.md` (38 l. — pas de `openspec/AGENTS.md`, pointe vers `openspec/project.md` + `opsx:*`).
- [x] `.agents/skills/pr-readiness/SKILL.md` (45 l.).
- [x] `.agents/skills/handoff/SKILL.md` (32 l.).
- [x] `.agents/skills/mcp-on-demand/SKILL.md` (36 l.).
- [x] `.agents/skills/scoped-task/SKILL.md` (44 l.).
- [x] `.agents/skills/spec-scoping/SKILL.md` (37 l.).
- [x] `.agents/mcp-activations.md` (header + table vide).

**Livrable** : 7 skills + README + log. Tous ≤55 lignes (cible ≤120).

## Phase 4 — AGENTS.md manquants

- [x] `tchalanet-mobile/AGENTS.md` (34 l., router pur, archi pointée vers doc Flutter).
- [x] `tchalanet-edge-service/AGENTS.md` (33 l., réfs `templates/`/`rules/` inexistantes corrigées → `docs/` + `src/`).
- [x] `tchalanet-docs/AGENTS.md` (32 l., réfs `mkdocs.yml`/`docs/` vérifiées).

**Livrable** : 3 routers purs, chemins vérifiés sur disque.

## Phase 5 — Adapters minces

- [x] `.claude/settings.json` racine minimal (schema seul).
- [x] `.claude/skills/<name>/SKILL.md` pour les 7 skills (6 l. chacun, frontmatter `name`/`description` + pointeur vers canonique).
- [x] `.claude/commands/spec.md` (10 l.).
- [x] `.claude/commands/backend-task.md` (19 l., scope lock).
- [x] `.claude/commands/web-task.md` (20 l., note "web bouge encore").
- [x] `.claude/commands/mobile-task.md` (19 l.).
- [x] `.claude/commands/ready-check.md` (11 l., lecture seule).
- [x] `.codex/AGENTS.md` racine (16 l., pointeur).
- [x] `.github/copilot.md` (10 l., pointeur).
- [~] `.claude/settings.json` per-project : **volontairement non créés** (anti-bloat — aucun skill/command propre hors web ; à ajouter quand un projet aura un skill spécifique).
- [ ] Tester chaque slash command sur une vraie tâche backlog (humain).

**Livrable** : adapters racine + slash commands, tous sous les limites de lignes.

## Phase 6 — Mesure réelle + MCP

- [x] Slice backend : `AGENTS.md` (51) + `tchalanet-server/AGENTS.md` (40) + `scoped-task` (44) = **135 l.** (cible <250). ✅
- [x] Slice web : `AGENTS.md` (51) + `tchalanet-web/AGENTS.md` (57) + `scoped-task` (44) = **152 l.** (cible <250). ✅
- [x] Installer/configurer GitHub MCP (humain) — `github` (PAT, scope local), ✓ Connected 2026-05-30.
- [x] Tester activation/désactivation, logger dans `.agents/mcp-activations.md` — Slack + GitHub testés, loggés.
- [x] Aucun MCP activé en permanence (règle dans `mcp-on-demand`, log prêt).

**Livrable** : mesures sous cible. GitHub MCP : install à faire par l'humain.

## Phases ultérieures (changes OpenSpec séparées, hors V1)

- [ ] Skills officiels Nx supplémentaires : étendre `tchalanet-web/.claude/skills/` avec pointeurs vers `nx-workspace`, `nx-generate`, `nx-run-tasks`.
- [ ] Enchaînement Claude/Codex/Copilot (10+ tâches sur 2 semaines, `.agents/lessons-learned.md`).
- [ ] Async léger (Slack MCP, garde-fous, 1 tâche éligible).
- [~] Setup accès distant Mac — Tailscale ✅, Termius ✅ (SSH par mot de passe opérationnel), tmux à installer, SSH par clé + PAT GitHub à régénérer (sécurité).
