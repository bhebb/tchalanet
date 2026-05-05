# Security model

## Scopes API (routing)

- PUBLIC: `/api/v1/public/**`
- TENANT: `/api/v1/tenant/**`
- ADMIN: `/api/v1/admin/**`
- PLATFORM: `/api/v1/platform/**`
- SDR: `/_sdr/**`

## AuthN / AuthZ

- AuthN: JWT (Keycloak)
- AuthZ: permissions centralisées (pas de logique ad-hoc dans chaque controller)

## Multi-tenant isolation

- Tenant résolu via request context
- PostgreSQL RLS comme dernière ligne de défense
- Aucun `tenant_id` venant du client n’est fiable

## Public endpoints (règles)

- explicites
- rate-limited
- validation stricte
- noindex quand nécessaire (`/ticket/:code`)

## Liens

- Backend docs: `tchalanet-server/docs/ROUTING_AND_API_PATHS_V1.md`, `tchalanet-server/docs/rls.md`
- OpenSpec: `openspec/context/10-non-negotiables.md`
