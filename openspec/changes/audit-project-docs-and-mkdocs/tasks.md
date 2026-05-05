# Tasks

## 1. Inventory all Markdown files

- [ ] Add or update a script that scans the repository for `.md` files.
- [ ] Exclude generated/vendor directories:
  - [ ] `.git/`
  - [ ] `node_modules/`
  - [ ] `dist/`
  - [ ] `build/`
  - [ ] `target/`
  - [ ] `.angular/`
  - [ ] `.nx/`
  - [ ] `.venv/`
- [ ] Count `.md` files by component:
  - [ ] root/global
  - [ ] `openspec/`
  - [ ] `tchalanet-server/`
  - [ ] `tchalanet-web/`
  - [ ] `tchalanet-mobile/`
  - [ ] `tchalanet-edge-service/`
  - [ ] `tchalanet-infra/`
  - [ ] `tchalanet-docs/`
- [ ] Generate `build/docs-inventory.json`.
- [ ] Generate `tchalanet-docs/docs/99-reference/docs-inventory.md`.

## 2. Classify docs

- [ ] Add classification fields:
  - [ ] owner
  - [ ] doc_type
  - [ ] title
  - [ ] status
  - [ ] canonical_source
  - [ ] recommended_action
- [ ] Classify docs into:
  - [ ] `CANONICAL`
  - [ ] `SUMMARY`
  - [ ] `LINK_ONLY`
  - [ ] `DUPLICATE`
  - [ ] `OBSOLETE`
  - [ ] `ARCHIVE`
  - [ ] `DELETE_LATER`
  - [ ] `UNKNOWN`

## 3. Detect likely duplicates

- [ ] Detect duplicate filenames.
- [ ] Detect duplicate H1 titles.
- [ ] Detect docs with high textual overlap.
- [ ] Detect docs mentioning obsolete services or workflows.
- [ ] Produce `build/docs-duplicates.md`.
- [ ] Do not delete anything automatically.

## 4. Define documentation ownership

- [ ] Create a canonical docs ownership map.
- [ ] Identify docs that should stay near code.
- [ ] Identify docs that should be summarized in MkDocs.
- [ ] Identify docs that should be link-only in MkDocs.
- [ ] Identify docs that should be archived.

## 5. Reorganize MkDocs

- [ ] Update `tchalanet-docs/mkdocs.yml`.
- [ ] Add or update:
  - [ ] `docs/index.md`
  - [ ] `docs/00-overview/`
  - [ ] `docs/01-architecture/`
  - [ ] `docs/02-domains/`
  - [ ] `docs/03-apps/`
  - [ ] `docs/04-operations/`
  - [ ] `docs/05-decisions/`
  - [ ] `docs/06-openspec/`
  - [ ] `docs/99-reference/`
- [ ] Add link pages to component docs.
- [ ] Avoid copying long component docs into MkDocs.

## 6. OpenSpec documentation strategy

- [ ] Document that global OpenSpec context is light.
- [ ] Document that each component may have its own OpenSpec.
- [ ] Add a MkDocs page listing component OpenSpec locations.
- [ ] Add archive policy for OpenSpec changes.
- [ ] Add context pack index page.

## 7. Cleanup plan

- [ ] Produce a cleanup proposal table:
  - [ ] keep
  - [ ] move
  - [ ] archive
  - [ ] merge
  - [ ] delete later
- [ ] Move only low-risk obsolete docs to archive after review.
- [ ] Keep redirects or index links when docs are moved.
- [ ] Validate MkDocs after changes.

## 8. Validation

- [ ] Run docs inventory script.
- [ ] Run MkDocs build.
- [ ] Validate OpenSpec:
  - [ ] `openspec validate audit-project-docs-and-mkdocs --strict`
- [ ] Confirm no component docs were deleted without archive/approval.
