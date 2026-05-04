# /infra-task

Use for Docker, compose, env files, Traefik, service wiring, local/staging/prod infra.

Default agent:

- infra-docker-agent

Rules:

- Do one task only.
- Do not scan the whole repo.
- Inspect only relevant infra files first.
- Never use `:latest`.
- Check root `VERSIONS.md` before image/tag changes.
- Do not change image tags unless explicitly requested.
- Do not commit secrets.
- Do not delete volumes.
- Document new env vars.
- Prefer `docker compose config` validation.

Required input:

- Environment: local | staging | prod
- Service(s) affected
- Task
- Files to inspect first
- Files allowed to edit
- Validation command

Output:

1. Environment affected
2. Services affected
3. Files inspected
4. Files changed
5. Env vars changed
6. Validation command
7. Rollback note
8. Handoff

Template:

Environment:

- local

Services:

- edge-service
- backend-api

Task:

- <one precise infra task>

Can edit:

- tchalanet-infra/...

Must inspect first:

- ...

Do not touch:

- tchalanet-server/src
- apps/\*
- tchalanet-edge-service/src

Validation:

- docker compose config
