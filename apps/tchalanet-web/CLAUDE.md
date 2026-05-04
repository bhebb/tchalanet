# Claude — tchalanet-web

Scope:

- Angular web app only.

Stack:

- Angular 20
- Angular Material 20
- Nx
- Signals + OnPush
- Mobile-first

Read first:

- Local component/service files.
- Closest README if present.
- i18n files only if labels are changed.

Rules:

- Use `input()`, `input.required()`, `output()` where appropriate.
- Use `@if` / `@for`.
- Prefer signals for UI state.
- No hardcoded colors.
- Use CSS variables/tokens.
- Respect light/dark theme.
- Respect breakpoints 480 / 768 / 1024.
- i18n keys use snake_case and functional namespaces.
- Do not duplicate i18n keys.
- Do not put widget logic directly in pages.
- Use renderer pattern for widgets.

Do not:

- Modify backend contracts.
- Invent endpoints.
- Change theme architecture casually.
- Scan all libs unless required.

Output:

1. Files inspected
2. Files changed
3. UI behavior
4. Tests/build command
5. Handoff
