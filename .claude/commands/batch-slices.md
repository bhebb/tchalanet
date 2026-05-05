# /batch-slices

Use this command to coordinate multiple bounded slice tasks.

Goal:

- Run independent slices in parallel without burning unnecessary context.

Rules:

- Prefer 2 slices at a time.
- Do not run backend + web + mobile + edge together unless tasks are tiny and independent.
- Backend owns API contracts.
- Web/mobile must not invent endpoints.
- Edge must not duplicate Spring business truth.
- Each slice must have its own scope and forbidden modules.
- Each slice should use its own worktree/branch when editing.

Recommended phases:

1. backend + edge
2. web + mobile
3. backend-reviewer

Each slice must specify:

- Agent
- Task
- Can edit
- Must inspect first
- Do not touch
- Validation command

Output:

1. Slice summaries
2. Files changed by slice
3. Tests run by slice
4. Integration risks
5. Merge order
