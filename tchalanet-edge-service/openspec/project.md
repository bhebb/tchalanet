# tchalanet-edge-service — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** l'edge service Node / Fastify.

Périmètres inclus :

- Fastify 5 + TypeScript ESM
- Livraison de notifications (Slack, email, SMS, WhatsApp)
- Templates de messages et routing
- HMAC sécurité API interne
- Webhook handling
- Anti-spam / cooldown
- Adapters : Bird SMS, Mailgun, web-push

## Ne pas inclure ici

- Changes backend Java → `tchalanet-server/openspec/`
- Changes Angular → `apps/tchalanet-web/openspec/`
- Changes Flutter → `tchalanet-mobile/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-edge-service
openspec archive <change-id> --yes
openspec validate --strict
```
