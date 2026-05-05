# Claude — tchalanet-docs

Claude router for MkDocs documentation work. Keep this file short; documentation
policy lives in `AGENTS.md` and `docs/00-guidelines/doc-policy.md`.

Read first:

- `../AGENTS.md`
- `AGENTS.md`
- `docs/index.md`
- `docs/00-guidelines/doc-policy.md`
- the page being edited

OpenSpec:

- Docs changes live in `tchalanet-docs/openspec/`.
- Use root `openspec/` only for cross-project documentation coordination.

Context rule:

- MkDocs is a portal and navigation layer.
- Link to component docs instead of copying long implementation detail.
- Keep long inventories in `docs/99-reference/`, not in daily navigation.

Commands:

```bash
venv/bin/mkdocs build --config-file mkdocs.yml
```

Claude-specific output:

1. Pages inspected
2. Pages changed
3. Navigation impact
4. Build result
