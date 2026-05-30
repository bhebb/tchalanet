# Workflow de développement — Tchalanet

Référence rapide : commandes AI, optimisation des tokens, et routage selon l'outil.

---

## Commandes Claude Code (slash commands / skills)

| Commande | Quand l'utiliser |
|---|---|
| `/spec` | Transformer une idée vague en spec implémentable — aucun code écrit |
| `/backend-task` | Tâche scoped dans `tchalanet-server` |
| `/web-task` | Tâche scoped dans `tchalanet-web` |
| `/mobile-task` | Tâche scoped dans `tchalanet-mobile` |
| `/scoped-task` | Tâche scoped dans n'importe quel autre projet (infra, edge, docs) |
| `/ready-check` | Vérification pré-PR des changements en cours (read-only) |
| `/handoff` | Compact handoff en fin de session ou avant `/clear` |
| `/code-review` | Review d'une PR (passe `--fix` pour auto-appliquer) |

### Règle d'or

Toujours commencer par un skill, pas par une instruction libre.  
Un skill charge le bon contexte dans le bon ordre — Claude fait moins d'erreurs et brûle moins de tokens.

---

## Équivalents Codex

Codex n'a pas de slash commands. Il lit directement `AGENTS.md` + un skill.

| Claude | Codex |
|---|---|
| `/spec` | Mentionner "scoping — use spec-scoping skill" dans le prompt |
| `/backend-task` | "backend task — use backend-task skill" |
| `/web-task` | "web task — use web-task skill" |
| `/ready-check` | "run pr-readiness skill on current diff" |
| `/handoff` | "produce handoff — use handoff skill" |

Le `.codex/AGENTS.md` pointe vers les mêmes skills canoniques dans `.agents/skills/`.  
Codex lit ce fichier automatiquement à l'ouverture — pas besoin de le préciser dans chaque prompt.

---

## Optimisation des tokens — conseils par slice

### Principe : slice-first, charge minimale

```
Charger : AGENTS.md racine + AGENTS.md projet + 1 skill + fichiers touchés
Ne pas charger : tout le projet, les tests, les autres modules
Cible : < 500 lignes hors code source
```

### Par type de tâche

| Tâche | Ce qu'il faut charger | Ce qu'il ne faut PAS charger |
|---|---|---|
| Feature backend | `AGENTS.md`, skill `backend-task`, slice domain touchée (ex: `PLATFORM_*.md`) | Toutes les autres slices |
| Spec / planning | `AGENTS.md`, skill `spec-scoping`, routers des projets impactés | Implémentations |
| PR review | diff + skill `pr-readiness` | Historique git, fichiers non touchés |
| Infra / Docker | `tchalanet-infra/AGENTS.md`, fichiers compose touchés | Scripts non modifiés |
| Docs | Fichier doc + doc-policy | Code source |

### Astuces concrètes

- **Ouvrir Claude Code depuis le sous-dossier** concerné (`cd tchalanet-server && claude`) — réduit le contexte chargé automatiquement.
- **Nommer le skill en premier** dans le prompt : `"backend task: ajouter endpoint X"` → Claude charge le skill sans demander.
- **`/clear` entre deux tâches non liées** — ne pas laisser le contexte d'une feature polluer la suivante.
- **Utiliser `/spec` avant de coder** toute feature > 1h — évite les allers-retours de correction.
- **Ne pas copier de fichiers entiers** dans le prompt — donner le chemin, Claude lit directement.

### Codex spécifiquement

- Codex tourne en mode non-interactif — être précis et exhaustif dans le prompt de départ.
- Inclure explicitement les fichiers à modifier : `"edit tchalanet-infra/Makefile and compose/docker-compose-api.yml"`.
- Commencer par la contrainte de scope : `"scope: tchalanet-infra only — do not touch other projects"`.

---

## Séquence type d'une tâche

```
1. /spec           ← scoper si la tâche est > 30 min ou multi-fichiers
2. /backend-task   ← implémenter (une slice à la fois)
3. /ready-check    ← valider avant PR
4. /handoff        ← si la session s'arrête avant la PR
5. PR + merge
```

---

## Outputs longs → Slack

Quand un résultat dépasse ~50 lignes (spec, handoff, ready-check), l'envoyer dans `#tchalanet-agents` (`C0B76AV9WAW`) via Slack MCP.  
Voir `.agents/mobile-workflow.md` pour le setup Termius/tmux.
