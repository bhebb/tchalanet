# Change: bootstrap-edge-service-fastify

## Why

`tchalanet-edge-service` existe déjà mais doit être nettoyé et réaligné sur l’architecture cible Tchalanet.

Le service doit devenir un edge-service léger en **Node.js + Fastify + TypeScript**, responsable des capacités périphériques :

- notifications Slack
- delivery email/SMS/WhatsApp plus tard
- delivery ticket avec pièces jointes plus tard
- templates
- rules/routing non critiques
- feature-management helpers/cache non critiques
- webhooks externes plus tard

Il ne doit pas posséder la vérité métier Tchalanet.

Spring Boot reste responsable de :

- tickets officiels
- draws / drawresults / settlement
- permissions
- tenant context / RLS
- audit métier
- transactions PostgreSQL
- règles critiques

Règle d’architecture :

```text
Spring Boot prépare, valide, autorise et audite.
Edge-service livre, formate, route et intègre.
```

## What

This change bootstraps the Fastify TypeScript structure for `tchalanet-edge-service`.

It must:

- clean or replace the current minimal folder structure
- add required runtime/dev dependencies
- configure TypeScript
- configure package scripts
- create a modular folder structure
- add a minimal Fastify app
- add `/ping`, `/health`, and `/ready` endpoints
- ensure the server starts successfully
- add minimal tests for ping/health
- avoid implementing Slack/Brevo/Twilio in this change

## Non-goals

This change must not implement:

- Slack sending
- Brevo email sending
- Twilio SMS sending
- HMAC authentication
- Redis anti-spam
- templates
- rules engine
- feature flag proxy
- delivery ticket attachments
- webhooks

These will be implemented in follow-up changes.

## Scope

Repository path:

```text
tchalanet-edge-service/
```

Allowed files/directories:

```text
tchalanet-edge-service/package.json
tchalanet-edge-service/package-lock.json OR pnpm-lock.yaml
tchalanet-edge-service/tsconfig.json
tchalanet-edge-service/README.md
tchalanet-edge-service/.env.example
tchalanet-edge-service/src/**
tchalanet-edge-service/tests/**
```

Do not modify unrelated backend, frontend, mobile, infra, or docs files.

## Architecture target

Create or align this structure:

```text
tchalanet-edge-service/
├─ package.json
├─ tsconfig.json
├─ .env.example
├─ README.md
├─ src/
│  ├─ main.ts
│  ├─ app.ts
│  ├─ config/
│  │  └─ env.ts
│  ├─ plugins/
│  │  └─ error-handler.plugin.ts
│  ├─ common/
│  │  └─ errors/
│  │     └─ app-error.ts
│  └─ modules/
│     ├─ health/
│     │  ├─ health.routes.ts
│     │  └─ health.types.ts
│     └─ ping/
│        ├─ ping.routes.ts
│        └─ ping.types.ts
└─ tests/
   └─ ping.test.ts
```

## Routing rules

System routes:

```text
GET /ping
GET /health
GET /ready
```

Expected responses:

```json
GET /ping
{
  "ok": true,
  "service": "tchalanet-edge-service"
}
```

```json
GET /health
{
  "status": "UP",
  "service": "tchalanet-edge-service"
}
```

```json
GET /ready
{
  "status": "READY",
  "service": "tchalanet-edge-service"
}
```

## Dependency guidance

Use Fastify + TypeScript.

Preferred dependencies:

```text
runtime:
- fastify
- dotenv

dev:
- typescript
- tsx
- vitest
- @types/node
```

Do not add Slack, Brevo, Twilio, Redis, Liquid, Zod, or other provider dependencies in this change.

Those dependencies belong to follow-up specs.

## Package scripts

Add or align scripts:

```json
{
  "scripts": {
    "dev": "tsx watch src/main.ts",
    "start": "node dist/main.js",
    "build": "tsc -p tsconfig.json",
    "test": "vitest run",
    "test:watch": "vitest",
    "typecheck": "tsc -p tsconfig.json --noEmit"
  }
}
```

## TypeScript rules

- Use TypeScript.
- Use ES modules if the package already uses `"type": "module"` or can safely be converted.
- Avoid `any`.
- Prefer explicit response types.
- Keep route handlers thin.
- Keep startup logic in `main.ts`.
- Keep app assembly in `app.ts`.

## Minimal implementation rules

`main.ts`:

- load env
- build app
- listen on `HOST` and `PORT`
- default `HOST=0.0.0.0`
- default `PORT=3000`
- log startup
- exit with code 1 on startup failure

`app.ts`:

- create Fastify instance
- register error handler plugin
- register ping routes
- register health routes
- return the app instance

`env.ts`:

- read `NODE_ENV`, `PORT`, `HOST`
- provide defaults
- avoid throwing unless absolutely necessary for this bootstrap

`ping.routes.ts`:

- register `GET /ping`
- return `{ ok: true, service: "tchalanet-edge-service" }`

`health.routes.ts`:

- register `GET /health`
- register `GET /ready`

## Test requirements

Add a minimal Vitest test using Fastify `inject()`.

Test:

- `GET /ping` returns HTTP 200
- response contains `ok: true`
- response contains `service: "tchalanet-edge-service"`

Optional:

- `GET /health` returns status `UP`
- `GET /ready` returns status `READY`

## Acceptance criteria

- `npm install` or `pnpm install` works depending on existing lockfile/package manager.
- `npm run typecheck` or equivalent passes.
- `npm run build` passes.
- `npm test` passes.
- `npm run dev` starts the server.
- `curl http://localhost:3000/ping` returns:

```json
{
  "ok": true,
  "service": "tchalanet-edge-service"
}
```

## Claude constraints

Important: conserve tokens.

Before editing:

1. Inspect only `tchalanet-edge-service/package.json`, `tsconfig.json`, `src`, and `tests`.
2. Do not scan the entire monorepo.
3. Do not touch Spring Boot, Angular, mobile, infra, or docs.
4. Do not implement providers.
5. Do not perform broad refactors.
6. Keep the patch minimal.

Output format after implementation:

```text
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
```
