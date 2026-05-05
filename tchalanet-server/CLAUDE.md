# Claude — tchalanet-server

Claude router for backend work. Keep this file short; backend rules live in
`AGENTS.md` and the docs listed below.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- nearest `DOMAIN_*.md` or `FEATURE_*.md` for the touched area

OpenSpec:

- Backend changes live in `tchalanet-server/openspec/`.
- Use root `openspec/` only for cross-project changes.

Context rule:

- Inspect only the package/slice needed for the task.
- Load one relevant convention doc from `docs/conventions/` when needed.
- Do not scan or edit web, mobile, infra, or edge unless explicitly requested.

Commands:

```bash
./mvnw test
./mvnw verify
```

Claude-specific output:

1. Files inspected
2. Files changed
3. Tests or validation run
4. Remaining risks
