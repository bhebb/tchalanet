# Skill — Web Testing for Tchalanet Angular/Nx

Use this skill only for `tchalanet-web` tasks.

## Principle

Angular tests verify UI behavior and contract consumption. They do not retest backend business invariants.

## Unit tests

Good targets:

- services and API clients;
- HTTP interceptors;
- guards;
- i18n/theme/settings runtime services;
- pure mappers from ApiResponse/PageModel DTOs;
- reducers/selectors/effects if NgRx is used;
- stateless components with meaningful branching.

## Component tests

Test shared UI contracts:

- TchNotice / TchNoticeList;
- TchErrorPanel for ProblemDetail;
- TchLoading / EmptyState;
- TchActionButton / ActionList;
- TchStatusBadge;
- PageHeader;
- PublicShell / PrivateShell / SideNav minimal rendering.

## Integration tests

Use when wiring matters:

- auth guard redirects unauthenticated users;
- interceptor unwraps/handles ApiResponse correctly;
- ProblemDetail displays correctly;
- runtime bootstrap loads settings + i18n + theme + PageModel separately;
- PageModel renderer chooses correct widget components;
- notices/service status are displayed.

## E2E candidates

- public home loads;
- language switch works;
- login/logout;
- role-based dashboard landing;
- tenant admin dashboard/overview;
- tenant admin users/outlets/terminals minimal CRUD happy path;
- public ticket check;
- permission denied screens.

## Rules

- Mock backend for unit/component tests.
- Use real backend or stable fixtures only for e2e.
- Do not couple tests to internal CSS unless visual behavior is the point.
- Prefer accessible selectors or data-testid for e2e.
- Contract-shape tests should use backend OpenAPI/examples when available.

## Do NOT test

- Angular internals (ChangeDetectorRef, ViewChild, component lifecycle unless it's the point).
- CSS classes unless visual behavior is the test (prefer semantic/accessible selectors).
- Backend business rules in frontend unit tests (assume the API contract is correct).
- Framework default behavior (routing, DI, HTTP client) unless customized.
