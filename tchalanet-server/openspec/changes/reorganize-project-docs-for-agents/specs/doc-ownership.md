# Spec — Documentation Ownership and Update Rules

## Rules

- ADRs are immutable after acceptance except typo/link corrections.
- Living inventories belong in `docs/reference`.
- Module-local rules belong in module `MODULE.md` or `AGENTS.md`.
- OpenSpec changes describe planned work and must be archived/updated after implementation.

## PR checklist

- If public API changed, update module doc.
- If package moved, update component map.
- If controller changed, update API docs/OpenAPI annotations where applicable.
- If persistence changed, update persistence docs and views/audit table notes.
