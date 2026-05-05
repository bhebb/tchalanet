# Claude — tchalanet-web

Claude router for Angular/Nx web work. Keep detailed rules in `AGENTS.md`,
`README.md`, and `libs/**/README.md`.

Read first:

- `../../AGENTS.md`
- `../../VERSIONS.md`
- `AGENTS.md`
- `README.md`
- nearest component/lib README for touched files

OpenSpec:

- Web changes live in `apps/tchalanet-web/openspec/`.
- Use root `openspec/` only for cross-project changes.

Context rule:

- Inspect only touched app/lib files and their nearest README.
- Load i18n/theme docs only when labels or styling are changed.
- Do not edit backend contracts unless explicitly requested.

Commands:

```bash
pnpm nx test tchalanet-web
pnpm nx build tchalanet-web
```

Claude-specific output:

1. Files inspected
2. Files changed
3. UI behavior affected
4. Tests or build run
