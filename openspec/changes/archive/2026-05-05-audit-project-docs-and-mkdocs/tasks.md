# Tasks

## 1. Inventory all Markdown files

- [x] Add or update a script that scans the repository for `.md` files.
- [x] Exclude generated/vendor directories:
  - [x] `.git/`
  - [x] `node_modules/`
  - [x] `dist/`
  - [x] `build/`
  - [x] `target/`
  - [x] `.angular/`
  - [x] `.nx/`
  - [x] `.venv/`
- [x] Count `.md` files by component:
  - [x] root/global
  - [x] `openspec/`
  - [x] `tchalanet-server/`
  - [x] `tchalanet-web/`
  - [x] `tchalanet-mobile/`
  - [x] `tchalanet-edge-service/`
  - [x] `tchalanet-infra/`
  - [x] `tchalanet-docs/`
- [x] Generate `build/docs-inventory.json`.
- [x] Generate `tchalanet-docs/docs/99-reference/docs-inventory.md`.

## 2. Classify docs

- [x] Add classification fields:
  - [x] owner
  - [x] doc_type
  - [x] title
  - [x] status
  - [x] canonical_source
  - [x] recommended_action
- [x] Classify docs into:
  - [x] `CANONICAL`
  - [x] `SUMMARY`
  - [x] `LINK_ONLY`
  - [x] `DUPLICATE`
  - [x] `OBSOLETE`
  - [x] `ARCHIVE`
  - [x] `DELETE_LATER`
  - [x] `UNKNOWN`

## 3. Detect likely duplicates

- [x] Detect duplicate filenames.
- [x] Detect duplicate H1 titles.
- [x] Detect docs with high textual overlap.
- [x] Detect docs mentioning obsolete services or workflows.
- [x] Produce `build/docs-duplicates.md`.
- [x] Do not delete anything automatically.

## 4. Define documentation ownership

- [x] Create a canonical docs ownership map.
- [x] Identify docs that should stay near code.
- [x] Identify docs that should be summarized in MkDocs.
- [x] Identify docs that should be link-only in MkDocs.
- [x] Identify docs that should be archived.

## 5. Reorganize MkDocs

- [x] Update `tchalanet-docs/mkdocs.yml`.
- [x] Add or update:
  - [x] `docs/index.md`
  - [x] `docs/00-overview/`
  - [x] `docs/01-architecture/`
  - [x] `docs/02-domains/`
  - [x] `docs/03-apps/`
  - [x] `docs/04-operations/`
  - [x] `docs/05-decisions/`
  - [x] `docs/06-openspec/`
  - [x] `docs/99-reference/`
- [x] Add link pages to component docs.
- [x] Avoid copying long component docs into MkDocs.

## 6. OpenSpec documentation strategy

- [x] Document that global OpenSpec context is light.
- [x] Document that each component may have its own OpenSpec.
- [x] Add a MkDocs page listing component OpenSpec locations.
- [x] Add archive policy for OpenSpec changes.
- [x] Add context pack index page.

## 7. Cleanup plan

- [x] Produce a cleanup proposal table:
  - [x] keep
  - [x] move
  - [x] archive
  - [x] merge
  - [x] delete later
- [x] Move only low-risk obsolete docs to archive after review.
  - No docs were moved in this inventory-first pass; archive actions are listed for review.
- [x] Keep redirects or index links when docs are moved.
  - No docs were moved in this inventory-first pass.
- [x] Validate MkDocs after changes.

## 8. Validation

- [x] Run docs inventory script.
- [x] Run MkDocs build.
- [x] Validate OpenSpec:
  - [x] `openspec validate audit-project-docs-and-mkdocs --strict`
- [x] Confirm no component docs were deleted without archive/approval.
