# Change: Simplify PageModel Runtime Contract

## Status

Proposed

## Why

The current PageModel runtime exposes too much of the backend definition model to Angular:

- storage-oriented fields such as `binding`, provider sources, and `fileKey`;
- historical snake_case fields;
- shell fragments delivered indirectly through dynamic widget payloads;
- separate document and dynamic envelopes that force the frontend to understand backend resolution.

This makes `features.pagemodel` harder to evolve and pushes composition work into the frontend.
PageModel remains useful, but its runtime boundary must become smaller and ready to render.

## What changes

This cross-project change separates two contracts:

```text
PageDefinition
  internal backend definition stored/resolved by catalog + core
  may contain binding, source, fileKey, and other resolution metadata

PageRuntimeResponse
  external features.pagemodel BFF contract consumed by web
  resolved, camelCase, storage-agnostic, and ready to render
```

The backend runtime assembler will:

- resolve shell fragments and widget static props;
- execute dynamic widget providers;
- produce a typed public or private shell;
- retain only render-relevant runtime metadata (`logicalId`, `scope`, `slug`, `schemaVersion`);
- remove template/admin metadata (`code`, `name`, `label`, `schema`, `isDefault`, `level`) and
  storage/provider details (`binding`, `source`, `fileKey`);
- serialize all runtime contract fields in camelCase;
- return layout, widget config, dynamic widget data, and an optional non-authoritative theme hint in
  one response; request-level notices and services remain on the existing API envelope.

Angular will:

- consume only `PageRuntimeResponse`;
- choose the shell renderer from `shell.type`;
- render layout widget ids by joining `content.widgets[id]` with `dynamic.widgets[id]`;
- stop resolving fragments, bindings, provider sources, or shell data.

## Impact

### Backend slice

- `features.pagemodel` owns the runtime DTOs and assembly boundary.
- `catalog.pagemodeltemplate` and `core.pagemodel` keep the internal definition/lifecycle model.
- Surface-oriented public, tenant-dashboard, and platform-dashboard endpoints return the simplified
  runtime contract without exposing internal PageModel identifiers.
- Focused contract, assembly, security, and serialization tests are required.

### Web slice

- Shared PageModel runtime types are replaced with camelCase runtime-only types.
- PageModel API consumers, shell components, widget host, and public/private page containers are adapted.
- Focused renderer and API contract tests are required.

### Existing OpenSpec changes

- `tchalanet-web/openspec/changes/extend-pagemodel-runtime-role-dashboards` may still define role,
  template, and provider coverage, but its `doc + dynamic` response design is superseded by this
  runtime contract.
- `openspec/changes/week-public-admin-minimal-dashboards` remains historical delivery context; its
  requirement that the backend return ready-to-render payloads is preserved.

## Non-goals

- No CMS or visual page editor.
- No arbitrary backend-driven CSS.
- No complex visibility expression engine.
- No deeply nested or backend-defined responsive layout system.
- No visual redesign of all widgets.
- No redesign of theme, i18n, settings/bootstrap, auth, or session APIs.
- No change to PageModel persistence lifecycle or tenant publication rules.
- No multiple frontend business calls to reconstruct a page.
