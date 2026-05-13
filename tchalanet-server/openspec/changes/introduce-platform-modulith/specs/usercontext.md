# Deprecated spec name — use `platform.identity`

This file is kept only to avoid broken references in older migration notes.

Decision:
- Do NOT create a separate user-context platform capability.
- Use `platform.identity` for persistent user/profile/membership data.
- Keep `common.context` for runtime/request context (`TchRequestContext`, `TchContext`, argument resolvers, request/batch binding).
- Keep operational context as a runtime/application resolution concern, not as persisted identity state.

Canonical replacement specs:
- `openspec/changes/refactor-operational-context-identity/specs/platform-identity.md`
- `openspec/changes/refactor-operational-context-identity/specs/request-context-common.md`
- `openspec/changes/refactor-operational-context-identity/specs/operational-context-resolution.md`
