# Claude — tchalanet-infra

Scope:

- Docker Compose
- environment files
- Traefik
- service wiring
- local/staging/prod infra scripts
- Makefile targets if infra-related

Source of truth:

- Root `VERSIONS.md` for all runtime/image versions.
- Do not change Docker image tags without updating `VERSIONS.md`.

Rules:

- Never use `:latest`.
- Keep local/staging/prod config separated.
- Do not put secrets in git.
- Prefer env files and documented variables.
- Keep Docker Compose changes minimal.
- Do not change exposed ports casually.
- Do not change volumes without migration/backward-compat note.
- Do not delete named volumes in scripts unless explicitly requested.
- Healthchecks should be explicit when service readiness matters.
- Infra must not encode business rules.
- Use deterministic image tags.
- Preserve existing service names unless migration is planned.

Services commonly involved:

- Postgres
- Redis
- Meilisearch
- Keycloak
- Traefik
- Unleash
- Umami
- backend API
- edge-service

Before editing:

- Inspect only relevant compose/env files.
- Check `VERSIONS.md` before changing images.
- List files to touch before editing.

Validation examples:

- `docker compose config`
- `docker compose ps`
- `docker compose logs <service>`
- `make <target>` if existing

Output:

1. Infra files inspected
2. Files changed
3. Services affected
4. Required env vars
5. Validation command
6. Rollback note
7. Handoff

Docker/infra:

- Do not change runtime image tags without checking root `VERSIONS.md`.
- Do not commit secrets.
- Keep Dockerfile minimal and production-safe.
- Prefer non-root runtime user if already supported.
- Expose only the configured service port.
- Health endpoint should be lightweight.
