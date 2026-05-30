# Infra

Infra operational documentation remains canonical in the infra project.

## Start Here

| Need                   | Canonical source                                                                                      |
| ---------------------- | ----------------------------------------------------------------------------------------------------- |
| First local setup      | `tchalanet-infra/QUICK-START.md`, `tchalanet-infra/docs/setup/DEMARRAGE.md`                           |
| Daily operations       | `tchalanet-infra/docs/operations/OPERATIONS.md`                                                       |
| Infra rules for agents | `tchalanet-infra/AGENTS.md`                                                                           |
| Environment architecture | `tchalanet-infra/docs/architecture/ENV-ARCHITECTURE.md`                                             |
| Image/build deployment | `tchalanet-infra/docs/operations/IMAGES-DEPLOYMENT.md`, `tchalanet-infra/docs/architecture/BUILD-LOCAL-VS-PUBLISHED.md` |
| Secrets/Doppler        | `tchalanet-infra/docs/setup/DOPPLER-SETUP-GUIDE.md`                                                  |
| Keycloak               | `tchalanet-infra/keycloak/README.md`, `tchalanet-infra/keycloak/realms/README.md`                     |
| Compose                | `tchalanet-infra/compose/README.md`, `tchalanet-infra/compose/docker-compose.index.md`                |
| Scripts                | `tchalanet-infra/docs/reference/scripts-index.md`, `tchalanet-infra/scripts/README.md`                |
| Infra OpenSpec         | `tchalanet-infra/openspec/project.md`                                                                 |

## Source Folders

- `tchalanet-infra/docs/architecture/` — env layout, build vs published images
- `tchalanet-infra/docs/setup/` — local dev, LAN, Doppler
- `tchalanet-infra/docs/operations/` — deployments, Hetzner, image ops
- `tchalanet-infra/docs/services/` — edge, Vite, Keycloak realm actions
- `tchalanet-infra/docs/reference/` — quick reference, scripts index
- `tchalanet-infra/CLAUDE.md`
- `tchalanet-infra/AGENTS.md`
- `tchalanet-infra/openspec/`
- `tchalanet-infra/envs/common/compose.env`

MkDocs should provide an operations map and route to infra-owned runbooks.
It should not list every infra Markdown file in the main navigation.
