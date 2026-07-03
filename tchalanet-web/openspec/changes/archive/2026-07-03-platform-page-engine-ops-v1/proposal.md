# platform-page-engine-ops-v1

## Why

Platform operations need a controlled PageModel editor so support/admin operators can inspect and
modify the raw page model JSON when a tenant page needs a targeted correction. The backend already
exposes the core PageModel operations, but the platform shell has no operations entry for them.

## What

- Add an operations page at `/app/platform/ops/page-engine`.
- Expose PageModel list and preview through data-access resources consumed by `tch-async-view`.
- Provide JSON edit/save for the selected PageModel.
- Wire available backend actions: create draft, publish, duplicate, reset.
- Add the operations sidenav entry in both server page-model resources and static fallback navigation.
- Translate all visible page strings in fr/en/ht.

## Impact

- Platform operators can list PageModels, select one, edit its JSON, and publish/reset/duplicate it
  without leaving the admin console.
- The page follows the resource/async-view pattern used by the platform referential lists.

## Backend contract check

Checked `tchalanet-core/pagemodel`: the available REST methods are list, create, update, preview,
duplicate, reset, and publish. Archive is present in the domain vocabulary but not exposed as a safe
REST operation with persisted archive metadata in this slice.

## Non-goals

- No backend archive endpoint or persistence change.
- No schema-aware JSON editor.
- No optimistic updates.
