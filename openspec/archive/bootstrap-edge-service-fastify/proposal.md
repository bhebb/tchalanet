# Change: bootstrap-edge-service-fastify

## Why

`tchalanet-edge-service` existait déjà mais devait être nettoyé et réaligné sur l'architecture cible Tchalanet.

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

Règle d'architecture :

```text
Spring Boot prépare, valide, autorise et audite.
Edge-service livre, formate, route et intègre.
```

## What

This change bootstrapped the Fastify TypeScript structure for `tchalanet-edge-service`.

## Non-goals

This change did not implement:

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

---

## Archive Information

**Archived:** 2026-05-03
**Duration:** 1 day
**Outcome:** Successfully implemented

### Files Created

- `tchalanet-edge-service/src/config/env.ts`
- `tchalanet-edge-service/src/app.ts`
- `tchalanet-edge-service/src/main.ts`
- `tchalanet-edge-service/src/plugins/error-handler.plugin.ts`
- `tchalanet-edge-service/src/common/errors/app-error.ts`
- `tchalanet-edge-service/src/modules/ping/ping.routes.ts`
- `tchalanet-edge-service/src/modules/ping/ping.types.ts`
- `tchalanet-edge-service/src/modules/health/health.routes.ts`
- `tchalanet-edge-service/src/modules/health/health.types.ts`
- `tchalanet-edge-service/tests/ping.test.ts`
- `tchalanet-edge-service/.gitignore`
- `tchalanet-edge-service/.env.example`

### Files Rewritten

- `tchalanet-edge-service/package.json`
- `tchalanet-edge-service/tsconfig.json`
- `tchalanet-edge-service/CLAUDE.md`
- `tchalanet-edge-service/README.md`

### Files Deleted

- `src/index.ts`, `src/routes/*`, `src/core/*`, `src/channels/*`
- `templates/`, `rules/`, `.claude/skills/`

### Specs Updated

- `openspec/specs/edge-service/spec.md` — new spec created
