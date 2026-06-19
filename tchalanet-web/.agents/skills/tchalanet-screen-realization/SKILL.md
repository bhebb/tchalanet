---
name: tchalanet-screen-realization
description: Mandatory workflow for creating, correcting, or replacing a Tchalanet web screen. Use for Angular pages or screen flows in tchalanet-web that involve routing, shells, layout, theme/style, API contracts, forms, loading/error/empty states, success behavior, i18n, responsive behavior, or accessibility.
metadata:
  short-description: Realize complete Tchalanet screens
---

# Skill — Tchalanet Screen Realization

Status: ACTIVE draft. Use this skill for `tchalanet-web` screen work: create, replace, repair, or integrate an Angular screen. A Tchalanet screen is a complete flow, not only a component template.

## Required References

Before coding, load the applicable project docs and the full guide when the task is implementation or review of a real screen:

- `WEB_ARCHITECTURE.md`
- `docs/conventions/style.md`
- `docs/conventions/theme-convention.md`
- `docs/conventions/web-naming.md`
- Shell, PageModel, or runtime docs when routing/navigation is involved
- Backend/API docs relevant to the screen contract
- [references/screen-realization-guide.md](references/screen-realization-guide.md)

If a referenced document is missing, say so briefly and continue from the closest available project convention.

## Golden Flow

Do not start with HTML or SCSS. Reason in this order:

1. Where we come from: route, shell, navigation, actor, permission.
2. What we receive: route params, query params, state, context, defaults, permissions.
3. How we present: layout type, theme tokens, reusable components, responsive rules.
4. Who we call: existing Angular service, backend endpoint, request DTO.
5. How we receive: response DTO, `ApiResponse<T>`, `ProblemDetail`, pagination.
6. How we render: loading, error, empty, ready, submitting, success.
7. What happens after: confirmation, navigation, refresh, local invalidation.

## Non-Negotiables

- Application shells own `router-outlet`; page/layout shells use `ng-content`.
- Never add `router-outlet` to `AdminPageShellComponent`, `AdminCrudShellComponent`, `AdminDetailLayoutComponent`, or `AdminSectionCardComponent`.
- Do not invent endpoints, DTOs, tenant IDs, permissions, or backend context.
- Angular services unwrap `ApiResponse<T>`; components should consume unwrapped views.
- Read backend errors as `ProblemDetail`.
- Use existing admin/public UI primitives before creating new components.
- Use runtime tokens: `--tch-*`; reusable components expose `--comp-*` with `--tch-*` fallbacks.
- Do not hardcode brand colors such as `#1A1B4B` or `#FECB00`.
- Use BEM-like classes and respect `docs/conventions/style.md`.
- Build screens mobile-first: compact layout first, then progressively enhance for wider viewports using project breakpoints.
- If a component file exceeds 100 lines, split into `.ts`, `.html`, and `.scss`; for multi-file components, place each component in its own folder.
- All user-visible UI text must use translation keys; temporary hardcoded text requires an explicit TODO and must not ship as final.
- Every data-driven screen must have explicit loading/error/empty/ready states; forms also need invalid/submitting/submit-error/success states.

## Standard Layout Selection

- List/CRUD index: `AdminPageShellComponent`, `AdminCrudShellComponent`, toolbar, content, footer, pagination if backend-paged.
- Create/edit/detail: `AdminPageShellComponent`, `AdminDetailLayoutComponent`, `AdminSectionCardComponent`, `TchIdentityCardComponent`, footer actions.
- Dashboard: `AdminPageShellComponent`, KPI cards, section cards, charts/tables, summary cards.
- Placeholder: `AdminPageShellComponent`, `AdminEmptyStateComponent`, linked TODO/story. A placeholder is not a final screen.

## Required Pre-Code Notes

Before editing, identify:

- surface, route, parent shell, router-outlet owner, navigation source;
- actor and permission;
- route/query/state/context inputs and refresh behavior;
- design/capture/stitch source, if any;
- components to reuse versus components to create;
- theme/style constraints and tokens;
- API contract and UI/API matrix;
- UI states;
- success behavior;
- translation keys for every visible text, mobile-first layout, and accessibility considerations.

## Required Post-Code Check

Before final response, report:

- files changed and whether the route/shell contract remains correct;
- whether existing components and tokens were used;
- API alignment and error handling;
- loading/error/empty/submitting/success coverage;
- i18n status;
- responsive/accessibility notes;
- validation commands run and any warnings or blockers.

## Golden Rule

A Tchalanet screen must answer six questions:

- Where do I come from?
- What do I receive?
- How do I present myself?
- Who do I call?
- How do I render the response?
- What happens after?

If one question has no answer, the screen is not ready.
