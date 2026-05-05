# Proposed MkDocs Navigation Organization

MkDocs is a portal and navigation layer. It should keep curated summaries in
`tchalanet-docs/docs/` and link to component docs for implementation detail.

## Proposed top-level navigation

| Section | Purpose | Canonical detail |
| --- | --- | --- |
| Home | Orientation and source-of-truth map | `DOCUMENTATION.md`, `AGENTS.md` |
| Overview | Product, architecture map, glossary | MkDocs summaries |
| Guidelines | Stable policies and governance | `tchalanet-docs/docs/00-guidelines/` |
| Architecture | System maps and cross-cutting architecture | MkDocs summaries plus component docs |
| Functional | Domain and workflow summaries | Backend domain docs for implementation truth |
| Applications | Component entrypoints | Docs near each component |
| Operations | Local/devops/deployment map | `tchalanet-infra/docs/` and runbooks |
| Decisions | ADR portal | `tchalanet-docs/docs/03-adr/` |
| OpenSpec | Change-workspace map | `openspec/` and component `openspec/` |
| Reference | Generated inventories and link maps | Generated reports |

## Non-goals

- Do not copy every component Markdown file into MkDocs.
- Do not make global OpenSpec a full documentation dump.
- Do not delete or move docs as part of inventory generation.
