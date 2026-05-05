# Documentation Tchalanet

MkDocs is the published portal for Tchalanet documentation. It provides curated
navigation, stable summaries, decision records, and links to the canonical docs
that remain close to each project.

## Operating Model

| Need                                      | Start here                                                             | Canonical source                                               |
| ----------------------------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------- |
| Project rules for agents and contributors | [Guidelines](00-guidelines/index.md)                                   | `AGENTS.md`                                                    |
| Runtime and tool versions                 | [Versions](00-guidelines/versions.md)                                  | `VERSIONS.md`                                                  |
| System boundaries and maps                | [Architecture](01-architecture/index.md)                               | `tchalanet-docs/docs/01-architecture/` plus component docs     |
| Domain and workflow summaries             | [Functional docs](02-functional/index.md)                              | `tchalanet-server/src/**/DOMAIN_*.md` for backend domain truth |
| Component implementation docs             | [Applications](03-apps/index.md)                                       | Docs near the owning component                                 |
| Local operations and deployments          | [Operations](04-operations/index.md), [Infra links](99-links/infra.md) | `tchalanet-infra/docs/` and component runbooks                 |
| Architecture decisions                    | [Decisions](05-decisions/index.md)                                     | `tchalanet-docs/docs/03-adr/`                                  |
| Change planning                           | [OpenSpec](06-openspec/index.md)                                       | `openspec/` and component `openspec/` workspaces               |
| Full documentation audit                  | [Reference](99-reference/index.md)                                     | Generated inventory reports                                    |

## Portal Rules

- MkDocs links to long implementation docs instead of copying them.
- Component docs remain canonical near code.
- Global OpenSpec context stays light and works as a router.
- Unknown or possibly stale docs are reviewed before any archive or deletion.
- The main navigation stays curated; large component inventories live in
  [Reference](99-reference/index.md) and [Links](99-links/index.md).

## Generated Reports

- [Documentation inventory](99-reference/docs-inventory.md)
- [Duplicate documentation report](99-reference/docs-duplicates.md)
- [Documentation cleanup plan](99-reference/docs-cleanup-plan.md)
- [MkDocs navigation plan](99-reference/mkdocs-nav-plan.md)
- [AI-agent files inventory](99-reference/ai-agent-files-inventory.md)
- [AI-agent cleanup plan](99-reference/ai-agent-cleanup-plan.md)
