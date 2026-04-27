---

## ✅ `openspec/context/60-infra-rules.md`

```md
# Infrastructure Rules & Map

This context defines infrastructure rules and constraints.
It applies to backend, frontend, mobile, edge, and ops.

---

## Scope

Infrastructure includes:

- Docker & Compose
- CI/CD pipelines
- Secrets management
- Networking & routing
- External services
- Observability
- Runtime environments

---

## Core Principles

- Infrastructure is declarative
- Environments are reproducible
- No hidden or manual steps
- No implicit defaults

---

## Versioning (MANDATORY)

All infra versions MUST be tracked in:

- `VERSIONS.md`

A version change requires:

1. Update `VERSIONS.md`
2. Update wrapper or config (`mvnw`, `pnpm`, `compose.env`)
3. Update Docker images
4. Note impact if production-facing

No exceptions.

---

## Environments

Standard environments:

- local
- dev
- staging
- prod

Rules:

- Same topology across environments
- Only configuration differs
- No code branching per environment

---

## Docker & Images

Rules:

- No `:latest` tags in production
- Images must be pinned
- Base images documented
- Custom images must be versioned

Compose files:

- live in `tchalanet-infra/compose/`
- environment variables sourced from `envs/*`

---

## Secrets & Configuration

Rules:

- Secrets NEVER committed
- Secrets injected via:
  - Doppler
  - environment variables
- No secrets in:
  - code
  - specs
  - documentation

---

## External Dependencies

External systems MUST be documented:

- purpose
- ownership
- failure mode
- retry / fallback behavior

Examples:

- Lottery providers
- Keycloak
- Redis
- Meilisearch
- Unleash

---

## CI/CD

Rules:

- CI must be deterministic
- Build must fail on:
  - version drift
  - missing migrations
  - invalid configs
- No manual production deploys

---

## Observability

Required:

- structured logs
- traceable request IDs
- clear error boundaries

Optional but encouraged:

- metrics
- dashboards
- alerting

---

## Source of Truth

- Infrastructure docs: `tchalanet-infra/docs/`
- Runtime versions: `VERSIONS.md`
- Deployment logic: `tchalanet-infra/compose/`
- Edge-specific rules: `50-edge-service-rules.md`
