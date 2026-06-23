# AGENTS.md — Tchalanet Web

Web agent router for `tchalanet-web/`, `apps/tchalanet-portal/`, and `libs/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `README.md`
- `CLAUDE.md`
- `openspec/`
- `libs/**/README.md`
- `libs/ui/widget-renderer/README.md`

## Architecture and conventions

Architecture overview (read when adding features, new libs, or touching shell/routing):

- `docs/ARCHITECTURE.md` — app structure, PageModel contract, lib boundaries
- `docs/dependencies.md` — lib dependency graph and allowed import directions

Conventions (load the relevant file for your task — do not load all):

| Task area | Load |
|---|---|
| Creating/moving files or components | `docs/conventions/placement-guide.md` |
| Naming files, classes, selectors | `docs/conventions/naming.md` |
| Adding a feature (route, page, service) | `docs/conventions/feature-playbook.md` |
| Feature folder structure (pages/components/data-access) | `docs/conventions/feature-structure.md` |
| HTTP calls, backend client, errors | `docs/conventions/http-api.md` |
| Styles, CSS tokens, SCSS | `docs/conventions/style.md` + `docs/conventions/theme.md` |
| i18n keys and translation files | `docs/conventions/i18n.md` |
| Auth, guards, session | `docs/conventions/auth.md` |
| Access gating (`*tchCan`, `can` pipe) | `docs/conventions/access.md` |
| Entitlements | `docs/conventions/entitlements.md` |
| Feature flags | `docs/conventions/feature-flags.md` |
| Nx lib boundaries | `docs/conventions/nx-boundaries.md` |
| State (signals, NgRx) | `docs/conventions/state-management.md` |
| PageModel / widget renderer | `docs/conventions/pagemodel.md` |
| Runtime settings | `docs/conventions/settings.md` |

Full index: `docs/conventions/README.md`

## Key libs

- `@tch/api` — `TchBackendClient`, `NavigationSection`, `ActionItem`, contracts
- `@tch/ui/components` — `TchCard`, `TchEmptyState`, `TchStatusBadge`, `TchLoading`, `TchSidebarNav`, …
- `@tch/ui/styles` — SCSS mixins and design token helpers
- `@tch/ui/theme` — Material 3 theme pipeline
- `@tch/page-model` — `PageModel`, `WidgetDef`, `VerificationStatus`, …
- `@tch/web` — `NotFoundPage`, shared app-level pieces
- `libs/widgets/` — widget components rendered by the widget renderer

OpenSpec:

- Use `tchalanet-web/openspec/` for Angular/Nx changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- Use existing Nx/pnpm targets for the touched app or library.
- Keep validation focused on changed web surfaces.

## HTTP / API convention

Toute communication avec le backend Tchalanet suit le layering :

```
Page / Component → service métier → TchBackendClient → HttpClient
```

- Les services métier injectent `TchBackendClient` (depuis `@tch/api`), pas `HttpClient`.
- Les pages n'appellent ni `TchBackendClient` ni `HttpClient` directement.
- Les paths passés à `TchBackendClient` sont des paths logiques sans `/api/v1` (ex. `/public/results`).
- Les appels hors backend Tchalanet (assets, i18n, Keycloak) conservent `HttpClient` direct.
- Voir `libs/api/README.md` pour la référence complète (`TchBackendClient`, raw downloads,
  multipart, suppressShellFeedback, asTenantAdmin).

Context rule:

- Load root rules, local web router, one relevant frontend/design doc, and touched component files.


<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

## General Guidelines for working with Nx

- For navigating/exploring the workspace, invoke the `nx-workspace` skill first - it has patterns for querying projects, targets, and dependencies
- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
- Prefix nx commands with the workspace's package manager (e.g., `pnpm nx build`, `npm exec nx test`) - avoids using globally installed CLI
- You have access to the Nx MCP server and its tools, use them to help the user
- For Nx plugin best practices, check `node_modules/@nx/<plugin>/PLUGIN.md`. Not all plugins have this file - proceed without it if unavailable.
- NEVER guess CLI flags - always check nx_docs or `--help` first when unsure

## Scaffolding & Generators

- For scaffolding tasks (creating apps, libs, project structure, setup), ALWAYS invoke the `nx-generate` skill FIRST before exploring or calling MCP tools

## When to use nx_docs

- USE for: advanced config options, unfamiliar flags, migration guides, plugin configuration, edge cases
- DON'T USE for: basic generator syntax (`nx g @nx/react:app`), standard commands, things you already know
- The `nx-generate` skill handles generator discovery internally - don't call nx_docs just to look up generator syntax


<!-- nx configuration end-->