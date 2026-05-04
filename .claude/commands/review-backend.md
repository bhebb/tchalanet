# /review-backend

Review backend changes only.

Use agent:

- backend-reviewer

Scope:

- Changed files or files explicitly provided.
- Do not refactor.
- Do not scan whole repo.

Checklist:

- Architecture layers respected.
- Typed IDs outside persistence.
- RLS/context safe.
- CommandBus/QueryBus used.
- `@TchTx` on write handlers.
- Events/audit/cache after commit.
- No cross-domain write in critical transaction.
- No repository/entity in controllers.
- Tests cover changed behavior.

Output:

1. Blockers
2. Non-blocking issues
3. Missing tests
4. Safe-to-merge verdict
