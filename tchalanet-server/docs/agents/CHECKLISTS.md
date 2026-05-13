# Agent Checklists

## Platform capability migration

- [ ] `api/` exists and exposes stable public types.
- [ ] `internal/` contains implementation.
- [ ] No external import of `internal`.
- [ ] Controllers are in `internal.web`.
- [ ] JPA is in `internal.persistence`.
- [ ] API models are records and typed-id based.
- [ ] Admin-only operations are internal unless externally consumed.

## Operational context integration

- [ ] Uses `@CurrentContext`.
- [ ] Uses identity/accesscontrol APIs.
- [ ] Uses outlet/terminal/session APIs.
- [ ] No direct repository imports across modules.
- [ ] Failure cases tested.

## Offline sync handler

- [ ] Technical validation stays in offlinesync.
- [ ] Sales validation stays in sales.
- [ ] Promotion goes through sales command/API.
- [ ] Idempotency/deduplication tested.
- [ ] Events are after commit.
