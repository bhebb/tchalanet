# Tasks: bootstrap-edge-service-fastify

## Status: DONE

## 1. Inspect current edge-service

- [x] Inspect `tchalanet-edge-service/package.json`
- [x] Inspect `tchalanet-edge-service/tsconfig.json` if present
- [x] Inspect `tchalanet-edge-service/src`
- [x] Inspect `tchalanet-edge-service/tests` if present
- [x] Identify whether current project uses npm lockfile or pnpm lockfile

## 2. Align dependencies

- [x] Add/keep runtime dependencies:
  - `fastify`
  - `dotenv`
- [x] Add/keep dev dependencies:
  - `typescript`
  - `tsx`
  - `vitest`
  - `@types/node`
- [x] Do not add Slack/Brevo/Twilio/Redis dependencies in this change
- [x] Keep only one lockfile type if possible

## 3. Align package scripts

- [x] Add or update `dev`
- [x] Add or update `start`
- [x] Add or update `build`
- [x] Add or update `test`
- [x] Add or update `test:watch`
- [x] Add or update `typecheck`

## 4. Configure TypeScript

- [x] Ensure `tsconfig.json` exists
- [x] Set `rootDir` to `src`
- [x] Set `outDir` to `dist`
- [x] Enable strict mode if feasible
- [x] Use Node-compatible module settings
- [x] Ensure tests can import source files

## 5. Create source structure

- [x] `src/main.ts`
- [x] `src/app.ts`
- [x] `src/config/env.ts`
- [x] `src/plugins/error-handler.plugin.ts`
- [x] `src/common/errors/app-error.ts`
- [x] `src/modules/ping/ping.routes.ts`
- [x] `src/modules/ping/ping.types.ts`
- [x] `src/modules/health/health.routes.ts`
- [x] `src/modules/health/health.types.ts`

## 6. Implement app startup

- [x] `main.ts` builds the app
- [x] `main.ts` listens on env host/port
- [x] default host is `0.0.0.0`
- [x] default port is `3000`
- [x] logs successful startup
- [x] exits with code 1 on startup failure

## 7. Implement app assembly

- [x] `app.ts` creates Fastify instance
- [x] registers error handler plugin
- [x] registers ping routes
- [x] registers health routes
- [x] returns Fastify instance

## 8. Implement ping service

- [x] `GET /ping` returns HTTP 200
- [x] response body: `{ ok: true, service: "tchalanet-edge-service" }`

## 9. Implement health/readiness routes

- [x] `GET /health` returns `{ status: "UP", service: "tchalanet-edge-service" }`
- [x] `GET /ready` returns `{ status: "READY", service: "tchalanet-edge-service" }`

## 10. Add tests

- [x] Add `tests/ping.test.ts`
- [x] Test `GET /ping`
- [x] Test `GET /health`
- [x] Test `GET /ready`

## 11. Validate

- [x] `npm run typecheck` → 0 errors
- [x] `npm run build` → dist/ compiled
- [x] `npm test` → 3/3 tests pass

## 12. Final report

- [x] Reported in conversation
