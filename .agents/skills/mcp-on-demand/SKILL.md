# mcp-on-demand

## Use when

A task might benefit from an MCP server (GitHub, Nx Cloud, etc.), or you are reviewing which MCPs are active.

## Rule

No permanent MCP by default. An MCP is activated only when the current task needs it, and deactivated after use when practical. Monthly audit: any MCP unused for 2 weeks is deactivated.

## Load

- This file.
- `.agents/mcp-activations.md` (the activation log).

## Do

- Activate an MCP only for the task that requires it.
- Log every activation in `.agents/mcp-activations.md` (Date / MCP / Task / Deactivation due).
- Prefer the narrowest MCP scope for the task.
- Deactivate after the task when practical.

## Status reference

- GitHub MCP: ✓ active (PAT, scope local). Activer pour PR/issues. ⚠️ PAT à régénérer.
- Slack MCP: ✓ active (claude.ai). Canal `#tchalanet-agents` (`C0B76AV9WAW`).
- Hetzner MCP: ✓ active (`@lazyants/hetzner-mcp-server@2.3.1`, `.mcp.json` racine). Nécessite `HETZNER_API_TOKEN` dans l'env shell. 185 outils — Cloud, DNS, Storage Box. Utiliser pour toute tâche ops Hetzner.
- Cloudflare MCP: ✓ active (`@cloudflare/mcp-server-cloudflare@0.2.0`, `.mcp.json` racine). Nécessite `CLOUDFLARE_API_TOKEN` dans l'env shell. Outils : zones DNS, Workers, KV, R2, D1, secrets, versions. **⚠️ Pas d'outils CF Pages** — les projets Pages se gèrent via le dashboard CF ou wrangler CLI.
- Nx Cloud MCP: on seulement pendant un travail web/Nx actif.
- Trello / Filesystem MCP: pas encore activés.

## Pipeline Slack → #tchalanet-agents

Envoyer dans `#tchalanet-agents` **uniquement** pour ces événements :

| Événement | Déclencheur | Format |
|---|---|---|
| ✅ PR mergée | après merge sur `main` | `PR #N — titre` |
| ⚠️ Blocage agent | 3 tentatives échouées | slice + raison + next step |
| 🔴 Alerte safety | refus d'action sensible | action refusée + règle citée |
| 📋 Handoff session | fin de session longue ou `/clear` | slice / fichiers / risques / next |
| 🔁 Review MCP | audit mensuel (date dans mcp-activations.md) | liste actifs + à désactiver |

**Ne pas envoyer** : chaque commit, chaque lecture de fichier, chaque step intermédiaire — seulement les événements ci-dessus.

## Do not

- Leave an MCP enabled "just in case".
- Activate a broad-surface MCP (e.g. filesystem) without explicit need and approval.

## Output

When activating, append one row to `.agents/mcp-activations.md` and state which MCP and why.
