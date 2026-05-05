# Component AGENTS.md Map

Component-specific details belong in component agent files. Root `AGENTS.md` should stay a short router.

| Component   | Agent file                         | Purpose                                                           |
| ----------- | ---------------------------------- | ----------------------------------------------------------------- |
| root/global | `AGENTS.md`                        | Global router, versions, OpenSpec routing, component map.         |
| backend     | `tchalanet-server/AGENTS.md`       | Backend commands, docs, OpenSpec workspace, validation.           |
| web         | `apps/tchalanet-web/AGENTS.md`     | Angular/Nx commands, docs, OpenSpec workspace, validation.        |
| mobile      | `tchalanet-mobile/AGENTS.md`       | Flutter commands, docs, OpenSpec workspace, validation.           |
| edge        | `tchalanet-edge-service/AGENTS.md` | Edge-service commands, docs, OpenSpec workspace, validation.      |
| infra       | `tchalanet-infra/AGENTS.md`        | Infra compose/env commands, docs, OpenSpec workspace, validation. |
| docs        | `tchalanet-docs/AGENTS.md`         | MkDocs commands, docs, OpenSpec workspace, validation.            |
