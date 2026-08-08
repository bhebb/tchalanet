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

## Architecture and conventions

Architecture overview (read when adding features, new libs, or touching shell/routing):

- `docs/ARCHITECTURE.md` — deux paradigmes (PageModel public / Console privé), carte des briques
  avec statut, chantiers actifs C1–C6, règles non négociables
- `docs/dependencies.md` — lib dependency graph and allowed import directions

**Créer/modifier un écran console (admin/platform) : commence TOUJOURS par
`docs/conventions/feature-playbook.md`** — archétypes liste/détail/création/édition/drawer/config,
squelettes copiables, table de décision des briques.

Conventions par sujet (load the relevant file for your task — do not load all):

| Tu travailles sur… | Lis |
|---|---|
| Un écran console : liste, détail, création, édition, drawer | `docs/conventions/feature-playbook.md` |
| État async : lecture (resource), écriture (mutation), loading/erreur/empty, filtres URL | `feature-playbook.md` §1.3–1.5 + §2 ; primitives dans `@tch/web/async` + `TchBackendClient.get*Resource` |
| APIs Angular modernes (signals, signal forms, control flow, DI) | playbook §1.9 (baseline) + skill `angular-developer` — références par sujet dans `.agents/skills/angular-developer/references/` (`signal-forms.md`, `signals-overview.md`, `linked-signal.md`, `inputs.md`, `outputs.md`…) |
| Style, SCSS, tokens CSS `--tch-*`/`--comp-*` (public + consoles) | `docs/conventions/style.md` + `docs/conventions/theme.md` |
| Composants : lequel utiliser, où le créer | `feature-playbook.md` §8 (table des briques) + `docs/conventions/placement-guide.md` |
| Le moteur de page public (PageModel, widgets) | `docs/conventions/pagemodel.md` |
| State/store (signals dans la page, quand extraire un store) | `docs/conventions/state-management.md` |
| i18n : clés, bundles, TranslatePipe (obligatoire partout) | `docs/conventions/i18n.md` |
| Erreurs : error-panel/section-error/notice/field-error, ProblemDetail | `docs/conventions/error-management.md` + `feature-playbook.md` §1.5 |
| HTTP, backend client, headers | `docs/conventions/http-api.md` |
| Organisation dossiers/fichiers d'une feature | `docs/conventions/structure.md` |
| Naming files, classes, selectors | `docs/conventions/naming.md` |
| Placement (dans quelle lib va ce code) | `docs/conventions/placement-guide.md` |
| Auth : règles (guards, session, headers) | `docs/conventions/auth.md` |
| Auth : flow détaillé (login, restore, interceptor, logout) | `docs/auth-flow.md` |
| Access gating (`*tchCan`, `can` pipe) | `docs/conventions/access.md` |
| Entitlements | `docs/conventions/entitlements.md` |
| Feature flags | `docs/conventions/feature-flags.md` |
| Nx lib boundaries | `docs/conventions/nx-boundaries.md` |
| Runtime settings | `docs/conventions/settings.md` |

Full index: `docs/conventions/README.md`

## Key libs

- `@tch/ui/console` — **design system des consoles** : `tch-admin-page-shell` (racine de toute
  page), `tch-admin-section-card`, `tch-admin-detail-layout` (main/aside), `tch-admin-crud-shell`,
  `tch-admin-empty-state`, `tch-identity-card`
- `@tch/ui/components` — primitives transverses : `TchLoading`, `TchErrorPanel`, `TchSectionError`,
  `TchNotice`, `TchFieldError`, `TchStatusBadge`, `TchConfirmDialog`, `TchActionButton`,
  `AdminListSurface`, … (le dossier `admin-crud/` hors `admin-list-surface` est deprecated — ne pas utiliser)
- `@tch/api` — `TchBackendClient` (dont `getResource`/`getPageResource` avec projection DTO→vue),
  `ActionItem`, `ProblemDetail`, contrats génériques (les services API métier restent dans
  `features/<feature>/data-access/`)
- `@tch/web/async` — primitives des pages liste/CRUD : `tch-async-view` (+ `tchAsyncReady`),
  `tchMutation`, `resourceErrorVm`, `serverFieldMessage`, helpers URL
  (`numberParam`/`dateParam`/`textParam`/`enumParam`), `delayedVisibility`
- `@tch/ui/console` — DS console (+ `tch-pagination`)
- `@tch/web/errors` — mapping erreurs bas niveau : `webAppErrorFromProblemDetail`,
  `resolveErrorFeedbackCopy`, `toErrorViewModel` (préférer `resourceErrorVm`/`tchMutation` en page)
- `@tch/ui/styles` — SCSS mixins and design token helpers
- `@tch/ui/theme` — Material 3 theme pipeline, tokens `--tch-*`
- `@tch/page-model` — moteur de rendu public : contrats runtime, renderer, `WidgetHost`
- `@tch/widgets` — widgets concrets rendus par PageModel
- `@tch/web/shell` — primitives shell public/privé
- `@tch/web` (façade racine) — mort, 0 usage, ne rien y ajouter (suppression planifiée)

OpenSpec:

- Use `tchalanet-web/openspec/` for Angular/Nx changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- For every web change, run the focused Nx targets for the touched app or library:
  `pnpm nx lint <project>`, `pnpm nx test <project>`, and `pnpm nx build <app>` when an app
  surface or shared runtime contract changed.
- For user-facing flows, run the relevant Playwright target/script such as `pnpm e2e:web`,
  `pnpm e2e:web:admin`, or the focused `web-e2e` spec when the runtime is available.
- If time or environment prevents the full suite, run the narrow equivalent, report exactly what
  ran, and call out any skipped gate.

## HTTP / API convention

Toute communication avec le backend Tchalanet suit le layering :

```
Page / Component → service métier → TchBackendClient → HttpClient
```

- Les services métier injectent `TchBackendClient` (depuis `@tch/api`), pas `HttpClient`.
- Les pages n'appellent ni `TchBackendClient` ni `HttpClient` directement.
- Les paths passés à `TchBackendClient` sont des paths logiques sans `/api/v1` (ex. `/public/results`).
- Les appels hors backend Tchalanet (assets, i18n, fournisseurs d'identité) conservent `HttpClient` direct.
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
