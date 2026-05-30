# Where truth lives

## What this page answers

Where should I look for authoritative information about a rule, behavior, or decision?

## Truth hierarchy

When two sources conflict, trust this order:

| Priority | Source | What it describes |
| --- | --- | --- |
| 1 | **Code + green tests** | The actual current state |
| 2 | **Accepted ADR** | Architectural intention |
| 3 | `<project>/docs/ARCHITECTURE.md` | Project structure and layer rules |
| 4 | `<project>/docs/conventions/*` | Technical rules for that project |
| 5 | `src/**/DOMAIN_*.md` · `CATALOG_*.md` · `PLATFORM_*.md` | Business domain and catalog invariants |
| 6 | `tchalanet-docs/docs/*` | Cross-project portal — orientation and summaries |
| 7 | `openspec/context/*` | AI context routing — not normative |
| 8 | `openspec/changes/*` | WIP specifications — temporary, not normative |

> If code contradicts a normative rule → it is **explicit debt**, not accepted truth.
> If an OpenSpec change contradicts a convention → the convention wins until a merge delivers the change.

## What lives where

| Need | Canonical source |
| --- | --- |
| Backend architecture and layers | `tchalanet-server/docs/ARCHITECTURE.md` |
| Backend implementation rules | `tchalanet-server/docs/conventions/` |
| Business domain invariants | `tchalanet-server/src/**/DOMAIN_*.md` |
| Mobile architecture | `tchalanet-mobile/docs/ARCHITECTURE.md` |
| Infra operations | `tchalanet-infra/docs/operations/` |
| Active feature work | `openspec/changes/<id>/` |
| Architecture decisions | `tchalanet-docs/docs/03-adr/` |
| Cross-project guidelines | `tchalanet-docs/docs/00-guidelines/` |
| Runtime versions | `VERSIONS.md` (root) |

## What MkDocs is for

MkDocs **orients and links** — it does not duplicate technical rules owned by component docs.

- It may describe what a feature does (observable behavior).
- It must not copy implementation rules or class-level detail.
- When a rule lives in `docs/conventions/`, MkDocs points there — it does not restate it.

## Reading paths

**I need to implement a backend feature**
→ `ARCHITECTURE.md` → `PLAYBOOK.md` → relevant `docs/conventions/` file

**I need to understand a business rule**
→ `DOMAIN_*.md` near the domain in `src/`

**I need to understand a cross-project flow**
→ `tchalanet-docs/docs/02-functional/flows/`

**I need to find an architecture decision**
→ `tchalanet-docs/docs/03-adr/`

See [Documentation policy](../00-guidelines/doc-policy.md) for the full ownership rules and project-by-project breakdown.
