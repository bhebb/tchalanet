# Claude — tchalanet-web

Scope:

## OpenSpec local

```text
apps/tchalanet-web/openspec/
```

Toutes les changes web (Angular/Nx, PageModel, auth web, theming) vivent ici.

Archiver via :

```bash
cd apps/tchalanet-web
openspec archive <change-id> --yes
```

## Périmètre

Ce projet est **autonome**. Ne pas inspecter ni modifier `tchalanet-server`, `tchalanet-mobile`, `tchalanet-edge-service` sauf demande explicite.

## Vérification contexte (obligatoire avant analyse ou édition)

```bash
pwd
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 3 -type d -name openspec
```

---

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
