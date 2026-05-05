# Design: Project documentation inventory and MkDocs organization

## Current problem

The project has several documentation layers:

```text
repo-root/
├── openspec/
├── tchalanet-server/
├── tchalanet-web/
├── tchalanet-mobile/
├── tchalanet-edge-service/
├── tchalanet-infra/
└── tchalanet-docs/
```

Each layer may contain `.md` files. Some are canonical, some are temporary, some are obsolete, and some duplicate global rules.

## Target model

Use a layered documentation model:

```text
Global OpenSpec context
= short routing rules only

Component OpenSpecs
= change planning close to each component

Component docs
= implementation and domain source of truth near code

tchalanet-docs / MkDocs
= published documentation portal and curated navigation
```

## Canonical ownership

| Documentation type | Canonical location |
| --- | --- |
| Runtime/tool versions | `VERSIONS.md` |
| Global architecture rules | `openspec/context/10-non-negotiables.md` |
| Backend technical rules | `openspec/context/20-backend-rules.md` + `tchalanet-server/docs/` |
| Web technical rules | `openspec/context/30-frontend-rules.md` + `tchalanet-web/**/README.md` |
| Mobile technical rules | `openspec/context/40-mobile-rules.md` + `tchalanet-mobile/docs/` |
| Edge rules | `openspec/context/50-edge-service-rules.md` + `tchalanet-edge-service/docs/` |
| Infra rules | `openspec/context/60-infra-rules.md` + `tchalanet-infra/docs/` |
| Catalog rules | `openspec/context/75-catalog-rules.md` |
| Core rules | `openspec/context/80-core-rules.md` |
| Feature rules | `openspec/context/81-feature-rules.md` |
| Backend domain rules | `tchalanet-server/src/**/DOMAIN_*.md` |
| Published product/architecture docs | `tchalanet-docs/docs/` |
| Active change specs | component `openspec/changes/*` |
| Archived change specs | component `openspec/changes/archive/*` |

## MkDocs organization

`tchalanet-docs` should become the published documentation portal.

Suggested navigation:

```text
tchalanet-docs/docs/
├── index.md
├── 00-overview/
│   ├── product.md
│   ├── architecture-map.md
│   └── glossary.md
├── 01-architecture/
│   ├── boundaries.md
│   ├── runtime-topology.md
│   ├── auth-and-access.md
│   ├── rls.md
│   ├── events.md
│   ├── batch.md
│   └── caching.md
├── 02-domains/
│   ├── draw.md
│   ├── drawresult.md
│   ├── sales.md
│   ├── payout.md
│   ├── ledger.md
│   └── limitpolicy.md
├── 03-apps/
│   ├── server.md
│   ├── web.md
│   ├── mobile.md
│   ├── edge.md
│   └── infra.md
├── 04-operations/
│   ├── local-development.md
│   ├── staging.md
│   ├── backups-restore.md
│   ├── deployments.md
│   └── troubleshooting.md
├── 05-decisions/
│   ├── index.md
│   └── adr-index.md
├── 06-openspec/
│   ├── index.md
│   ├── active-changes.md
│   ├── context-packs.md
│   └── archive-policy.md
└── 99-reference/
    ├── repo-map.md
    ├── docs-inventory.md
    └── links-to-component-docs.md
```

## Component docs linking strategy

MkDocs should not blindly copy every component Markdown file.

Preferred approach:

1. MkDocs owns curated summaries and architecture guides.
2. Component docs remain near code.
3. MkDocs pages link to component docs using repository links or generated link pages.
4. For important stable domain docs, MkDocs may include a summary and link to the canonical source.
5. Do not duplicate long implementation docs unless publication requires it.

## OpenSpec handling in MkDocs

MkDocs should expose OpenSpec as a documentation map, not as a full dump.

Recommended pages:

```text
06-openspec/index.md
06-openspec/context-packs.md
06-openspec/active-changes.md
06-openspec/archive-policy.md
```

Rules:

- Global OpenSpec context stays light.
- Component OpenSpecs remain in their component.
- MkDocs lists active changes and links to component OpenSpec folders.
- Archived OpenSpecs are discoverable but not loaded into AI context by default.
- OpenSpec specs are not copied into global context packs.

## Documentation inventory

Create a generated inventory file:

```text
tchalanet-docs/docs/99-reference/docs-inventory.md
```

The inventory should include:

- path;
- component owner;
- doc type;
- last modified date;
- status;
- canonical/duplicate/obsolete/unknown;
- recommended action.

Example table:

| Path | Owner | Type | Status | Action |
| --- | --- | --- | --- | --- |
| `tchalanet-infra/docs/00-infra-charter.md` | infra | charter | canonical | link from MkDocs |
| `openspec/context/60-infra-rules.md` | global | context | canonical summary | keep short |
| `tchalanet-infra/docs/OLD-QUICK-START.md` | infra | legacy | obsolete | archive |

## Duplicate detection

Start with heuristic detection:

- same filename in multiple components;
- same title;
- high line overlap;
- same key phrases;
- old docs with newer replacements;
- docs that contradict current decisions.

Suggested script:

```text
scripts/docs/inventory-docs.py
```

Output:

```text
build/docs-inventory.json
build/docs-duplicates.md
tchalanet-docs/docs/99-reference/docs-inventory.md
```

## Cleanup policy

Do not delete immediately.

Use statuses:

```text
CANONICAL
SUMMARY
LINK_ONLY
DUPLICATE
OBSOLETE
ARCHIVE
DELETE_LATER
UNKNOWN
```

Move old docs to an archive folder first:

```text
<component>/docs/archive/
```

or:

```text
tchalanet-docs/docs/99-reference/archive/
```

Deletion requires a follow-up change after validation.

## Acceptance criteria

- A script counts all `.md` files.
- A docs inventory is generated.
- Likely duplicates are listed.
- A canonical ownership map exists.
- MkDocs navigation is reorganized.
- MkDocs links to component docs instead of duplicating them.
- OpenSpec context remains light.
- Component OpenSpecs are referenced, not centralized.
