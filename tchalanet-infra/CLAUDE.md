# Claude — tchalanet-infra

Claude router for infra work. Detailed infra guidance lives in `AGENTS.md` and
`docs/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `AGENTS.md`
- `docs/README.md`
- the compose/env/runbook files explicitly touched

OpenSpec:

- Infra changes live in `tchalanet-infra/openspec/`.
- Use root `openspec/` only for cross-project changes.

Context rule:

- Inspect only relevant compose/env/script files.
- Check `VERSIONS.md` before image/runtime changes.
- Do not commit secrets or broaden exposed ports casually.

Commands:

```bash
docker compose config
make help
```

Claude-specific output:

1. Infra files inspected
2. Services affected
3. Validation command
4. Rollback note
