# openspec-workflow

## Use when

A task is planning, architecture, refactor, a new capability, a breaking change, or broad documentation organization. OpenSpec is mandatory for these.

## Load

- `openspec/project.md` — project conventions.
- `openspec/README.md` — workflow overview.
- The touched project's `openspec/changes/` (not all of them).

## Do

- Create the change in the **touched project's** OpenSpec, not the root, unless the work is cross-project.
- Keep a change as `proposal.md` + `tasks.md` (+ `design.md` when non-trivial).
- Use the `opsx:*` skills for the lifecycle: `propose`, `apply`, `continue`, `verify`, `archive`.
- Keep global OpenSpec context light — it routes, it does not copy component docs.

## Change locations

- Cross-project: `openspec/changes/`
- Backend: `tchalanet-server/openspec/changes/`
- Web: `tchalanet-web/openspec/changes/`
- Mobile: `tchalanet-mobile/openspec/changes/`
- Edge: `tchalanet-edge-service/openspec/changes/`
- Infra: `tchalanet-infra/openspec/changes/`
- Docs: `tchalanet-docs/openspec/changes/`

## Do not

- Implement a new capability or broad refactor without an OpenSpec change.
- Archive manually with `rm`/`cp`/`mv` — use the OpenSpec CLI / `opsx:archive` from the project root.
- Duplicate component docs into the change.

## Output

A change folder with `proposal.md` (Why / What / Impact / Non-goals) and `tasks.md` (checkable steps).
