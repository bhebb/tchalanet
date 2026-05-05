# Tasks: bootstrap-edge-service-fastify

## 1. Inspect current edge-service

- [ ] Inspect `tchalanet-edge-service/package.json`
- [ ] Inspect `tchalanet-edge-service/tsconfig.json` if present
- [ ] Inspect `tchalanet-edge-service/src`
- [ ] Inspect `tchalanet-edge-service/tests` if present
- [ ] Identify whether current project uses npm lockfile or pnpm lockfile

## 2. Align dependencies

- [ ] Add/keep runtime dependencies:
  - `fastify`
  - `dotenv`
- [ ] Add/keep dev dependencies:
  - `typescript`
  - `tsx`
  - `vitest`
  - `@types/node`
- [ ] Do not add Slack/Brevo/Twilio/Redis dependencies in this change
- [ ] Keep only one lockfile type if possible

## 3. Align package scripts

- [ ] Add or update `dev`
- [ ] Add or update `start`
- [ ] Add or update `build`
- [ ] Add or update `test`
- [ ] Add or update `test:watch`
- [ ] Add or update `typecheck`

Expected scripts:

```json
{
  "dev": "tsx watch src/main.ts",
  "start": "node dist/main.js",
  "build": "tsc -p tsconfig.json",
  "test": "vitest run",
  "test:watch": "vitest",
  "typecheck": "tsc -p tsconfig.json --noEmit"
}
```

## 4. Configure TypeScript

- [ ] Ensure `tsconfig.json` exists
- [ ] Set `rootDir` to `src`
- [ ] Set `outDir` to `dist`
- [ ] Enable strict mode if feasible
- [ ] Use Node-compatible module settings
- [ ] Ensure tests can import source files

## 5. Create source structure

Create or align:

```text
src/
  main.ts
  app.ts
  config/env.ts
  plugins/error-handler.plugin.ts
  common/errors/app-error.ts
  modules/ping/ping.routes.ts
  modules/ping/ping.types.ts
  modules/health/health.routes.ts
  modules/health/health.types.ts
```

## 6. Implement app startup

- [ ] `main.ts` builds the app
- [ ] `main.ts` listens on env host/port
- [ ] default host is `0.0.0.0`
- [ ] default port is `3000`
- [ ] logs successful startup
- [ ] exits with code 1 on startup failure

## 7. Implement app assembly

- [ ] `app.ts` creates Fastify instance
- [ ] registers error handler plugin
- [ ] registers ping routes
- [ ] registers health routes
- [ ] returns Fastify instance

## 8. Implement ping service

- [ ] `GET /ping` returns HTTP 200
- [ ] response body:

```json
{
  "ok": true,
  "service": "tchalanet-edge-service"
}
```

## 9. Implement health/readiness routes

- [ ] `GET /health` returns:

```json
{
  "status": "UP",
  "service": "tchalanet-edge-service"
}
```

- [ ] `GET /ready` returns:

```json
{
  "status": "READY",
  "service": "tchalanet-edge-service"
}
```

## 10. Add tests

- [ ] Add `tests/ping.test.ts`
- [ ] Test `GET /ping`
- [ ] Optionally test `GET /health`
- [ ] Optionally test `GET /ready`

## 11. Validate

Run:

```bash
npm run typecheck
npm run build
npm test
```

Or pnpm equivalent if the repo uses pnpm:

```bash
pnpm typecheck
pnpm build
pnpm test
```

Then manually test:

```bash
npm run dev
curl http://localhost:3000/ping
```

Expected:

```json
{
  "ok": true,
  "service": "tchalanet-edge-service"
}
```

## 12. Final report

Claude must report:

```text
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
```
