# Claude — tchalanet-edge-service

Claude router for edge-service work. Keep implementation detail in `AGENTS.md`,
`README.md`, and near-code docs.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `AGENTS.md`
- `README.md`
- files explicitly mentioned in the task

OpenSpec:

- Edge changes live in `tchalanet-edge-service/openspec/`.
- Use root `openspec/` only for cross-project changes.

Context rule:

- Inspect only touched modules/templates/rules.
- Do not implement new external adapters without an OpenSpec change.
- Do not edit server, web, mobile, or infra unless explicitly requested.

Commands:

```bash
npm test
npm run typecheck
npm run build
```

Claude-specific output:

1. Files inspected
2. Channels/templates/rules affected
3. Tests or build run
4. Remaining risks
