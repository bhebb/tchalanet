# MCP Activations Log

No permanent MCP by default. Log every activation here. Monthly audit: any MCP unused for 2 weeks is deactivated. See `.agents/skills/mcp-on-demand/SKILL.md`.

| Date | MCP | Task | Deactivation due |
| ---- | --- | ---- | ---------------- |
| 2026-05-30 | GitHub (http, scope local, PAT) | PR/issue workflow — PR #107 créée + mergée via MCP. ⚠️ PAT exposé en clair → **régénérer**. | Review 2026-06-13 — garder si usage régulier ; migrer vers OAuth plugin quand PAT régénéré |
| 2026-05-30 | Slack (claude.ai) | Connectivity test (read-only) — `#tchalanet` + `#tchalanet-agents` créé. Slack = Phase 6 (async), pas encore actif. | Review 2026-06-13 — garder si Phase 6 démarrée ; désactiver sinon |
| 2026-06-30 | Hetzner MCP (`@lazyants/hetzner-mcp-server@2.3.1`) | Gestion infra Hetzner Cloud — serveurs, réseaux, firewalls, volumes, DNS, Load Balancers, IPs, Storage Box (185 outils). Token via `HETZNER_API_TOKEN` env var. Config dans `.mcp.json` à la racine. | Review 2026-07-31 — garder si usage ops régulier |
| 2026-06-30 | Cloudflare MCP (`@cloudflare/mcp-server-cloudflare@0.2.0`) | Gestion Cloudflare — zones DNS, Workers, KV, R2, D1, secrets, env vars, versions, rollback. Token via `CLOUDFLARE_API_TOKEN` env var. ⚠️ Pas d'outils CF Pages natifs — Pages géré via dashboard + GitHub integration. | Review 2026-07-31 — garder si usage DNS/Workers actif |
