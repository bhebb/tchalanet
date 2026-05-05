# Backend

Backend implementation documentation remains canonical near code.

## Start Here

| Need                         | Canonical source                                                     |
| ---------------------------- | -------------------------------------------------------------------- |
| Backend agent/context rules  | `tchalanet-server/AGENTS.md`                                         |
| Architecture and layer rules | `tchalanet-server/docs/ARCHITECTURE.md`                              |
| Delivery workflow            | `tchalanet-server/docs/PLAYBOOK.md`                                  |
| Naming                       | `tchalanet-server/docs/NAMING.md`                                    |
| API routing                  | `tchalanet-server/docs/ROUTING_AND_API_PATHS_V1.md`                  |
| RLS and tenant isolation     | `tchalanet-server/docs/rls.md`, `tchalanet-server/docs/conventions/` |
| Implementation conventions   | `tchalanet-server/docs/conventions/`                                 |
| Domain truth                 | `tchalanet-server/src/**/DOMAIN_*.md`                                |
| Feature truth                | `tchalanet-server/src/**/FEATURE_*.md`                               |
| Backend OpenSpec             | `tchalanet-server/openspec/`                                         |

## Source Folders

- `tchalanet-server/docs/ARCHITECTURE.md`
- `tchalanet-server/docs/PLAYBOOK.md`
- `tchalanet-server/docs/ROUTING_AND_API_PATHS_V1.md`
- `tchalanet-server/docs/rls.md`
- `tchalanet-server/docs/conventions/`
- `tchalanet-server/src/**/DOMAIN_*.md`
- `tchalanet-server/src/**/FEATURE_*.md`

MkDocs should summarize and route to these files; it should not copy long
backend implementation docs.
