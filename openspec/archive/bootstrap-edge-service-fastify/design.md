# Design: bootstrap-edge-service-fastify

## Overview

This change initializes the `tchalanet-edge-service` as a small Fastify TypeScript application.

The goal is not to implement business features yet. The goal is to establish a clean, testable foundation.

## Runtime model

The service runs as a standalone Node.js process.

```text
src/main.ts
  -> loads env
  -> calls buildApp()
  -> listens on HOST/PORT
```

```text
src/app.ts
  -> creates Fastify instance
  -> registers plugins
  -> registers routes
```

## Module model

Modules own their routes and types.

Example:

```text
modules/ping/
  ping.routes.ts
  ping.types.ts
```

Routes should not contain provider-specific logic. Future modules will use:

```text
domain/
application/
ports/
adapters/
http/
```

For this bootstrap change, `ping` and `health` can remain simple.

## System endpoints

### `GET /ping`

Purpose:

- smoke test
- local development check
- simple integration test target
- easy curl endpoint

Response:

```json
{
  "ok": true,
  "service": "tchalanet-edge-service"
}
```

### `GET /health`

Purpose:

- liveness probe

Response:

```json
{
  "status": "UP",
  "service": "tchalanet-edge-service"
}
```

### `GET /ready`

Purpose:

- readiness probe
- future dependencies can be checked here, such as Redis or provider config

For this change, it returns static `READY`.

Response:

```json
{
  "status": "READY",
  "service": "tchalanet-edge-service"
}
```

## Environment

Minimum env:

```env
NODE_ENV=development
HOST=0.0.0.0
PORT=3000
```

Defaults:

```text
NODE_ENV=development
HOST=0.0.0.0
PORT=3000
```

## Error handling

Add a minimal error handler plugin.

For now:

- log unexpected errors
- return HTTP 500 with generic message
- do not leak stack traces in production

Future changes can add:

- structured error codes
- requestId
- validation error mapping
- provider error mapping

## Tests

Use Fastify `inject()` to test without opening a real network port.

Minimal test:

```text
buildApp()
  -> app.inject({ method: "GET", url: "/ping" })
  -> expect statusCode 200
  -> expect body.ok true
```

## Future changes

After this bootstrap, follow-up changes may add:

- HMAC auth for `/internal/*`
- Slack notification adapter
- Brevo email adapter
- Twilio SMS adapter
- delivery ticket endpoint
- attachment URL loading
- Redis anti-spam/idempotency
- feature-management helper/proxy
- rules module
- webhooks module
