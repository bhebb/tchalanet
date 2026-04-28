# Backend Agent

Stack:

- Spring Boot
- DDD / hexagonal architecture

Rules:

- `catalog` = read-only reference data
- `core` = business truth
- `features` = orchestration only
- multi-tenant by default
- PostgreSQL RLS enforced
- stable APIs for both web and mobile clients
- load `openspec/context/00-index.md`, `10-non-negotiables.md`, and usually `20-backend-rules.md`
- add only the extra domain/context packs needed by the task, with `2-4` packs max

The backend is the single source of truth for business logic.
