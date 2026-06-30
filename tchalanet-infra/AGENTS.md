# AGENTS.md — Tchalanet Infra

Infra agent router for `tchalanet-infra/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `docs/architecture/` — ENV-ARCHITECTURE.md, BUILD-LOCAL-VS-PUBLISHED.md
- `docs/setup/` — DEMARRAGE.md, LAN-SETUP.md, DOPPLER-SETUP-GUIDE.md
- `docs/operations/` — DEPLOYMENT.md, OPERATIONS.md, HETZNER.md, IMAGES-DEPLOYMENT.md
- `docs/services/` — EDGE-SERVICE.md, VITE-ALLOWED-HOSTS.md, ACTION-REALM-REGEN.md
- `docs/reference/` — QUICK-REFERENCE.md, scripts-index.md
- `CLAUDE.md`
- `openspec/`
- `envs/common/compose.env`

OpenSpec:

- Use `tchalanet-infra/openspec/` for Docker, environment, deployment, and CI/CD changes.
- Use root `openspec/` only for cross-project coordination.

MCP disponible:

- **Hetzner MCP** (`@lazyants/hetzner-mcp-server`) — 185 outils : serveurs, réseaux, firewalls, volumes, DNS, Load Balancers, IPs, Storage Box. Requiert `HETZNER_API_TOKEN` dans l'env shell. Token : [console.hetzner.cloud](https://console.hetzner.cloud/) → Security → API Tokens.

- **Cloudflare MCP** (`@cloudflare/mcp-server-cloudflare`) — zones DNS (`zones_list`, `zones_get`), Workers, KV, R2, D1, secrets, env vars, versions + rollback. Requiert `CLOUDFLARE_API_TOKEN` dans l'env shell. Token : [Cloudflare dashboard](https://dash.cloudflare.com/profile/api-tokens) → Create Token → template "Edit Cloudflare Workers" (ajouter Zone:Read si besoin DNS). **⚠️ Pas d'outils CF Pages** — les projets Pages se créent dans le dashboard CF et se déploient via GitHub integration automatique. Voir [RB-02](docs/operations/runbooks/RB-02-web-cf-pages.md).

Les deux MCP sont activés via `.mcp.json` à la racine du monorepo. Démarrer Claude Code avec les deux tokens exportés :
```bash
export HETZNER_API_TOKEN=<token>
export CLOUDFLARE_API_TOKEN=<token>
```

Validation:

- `docker compose config`
- Existing `make` targets when relevant.
- Never commit secrets.

Context rule:

- Load root rules, local infra router, `VERSIONS.md`, and only the compose/env files being changed.
