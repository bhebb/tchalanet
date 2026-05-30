# pr-readiness

## Use when

You are about to propose a PR or asked to review changes before they ship. Read-only by default.

## Load

- This file.
- The diff / changed files.
- The touched project's `AGENTS.md` for its validation commands.

## Do

- Confirm scope: only files the task required, no accidental refactor.
- Confirm no secrets, `.env`, tokens, keys in the diff.
- Confirm no version bump without a matching `VERSIONS.md` change.
- Confirm no new dependency without justification.
- Confirm tests added or updated for changed behavior.
- Confirm durable docs updated only if a durable rule changed.
- Run the project's validation command (see its `AGENTS.md`).

## Per-project validation

- Backend: `./mvnw test` / `./mvnw verify`.
- Web: relevant Nx target (`pnpm nx test|lint|build <project>`).
- Mobile: `flutter analyze`, `flutter test`.
- Edge: `npm run typecheck`, `npm test`, `npm run build`.
- Infra: `docker compose config` (never commit secrets).
- Docs: `mkdocs build`.

## Do not

- Push to `main`. Force-push. Auto-merge.
- Approve a diff that crosses slice boundaries without it being stated.

## Output

```
Recommendation: Ship / Not yet
Blockers: <list or none>
Important (non-blocking): <list>
Missing tests: <list or none>
Overall risk: Low / Medium / High
```
