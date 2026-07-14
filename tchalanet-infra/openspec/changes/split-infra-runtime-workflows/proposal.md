# split-infra-runtime-workflows

## Why

The staging workflow mixed infrastructure lifecycle, core container setup, runtime image builds, runtime deploys, and health checks. This made fixes repeat across workflows and allowed runtime deploys to implicitly manage Traefik or Redis.

## What

- Make the staging infrastructure workflow responsible only for server lifecycle and core services: Traefik and Redis.
- Keep API and edge-service build/deploy/promotion in the runtime deployment workflow.
- Make runtime deploys fail clearly when core services are not ready, instead of trying to repair infra implicitly.
- Keep generated Traefik router output environment-owned on the remote host.

## Impact

- Recreating staging becomes a two-step operation: run infra, then run runtime deployment.
- Runtime deployment becomes narrower and easier to promote to production.
- Troubleshooting errors should point to either core infra readiness or runtime service health.

## Non-goals

- No Cloudflare Pages changes.
- No backend or edge-service application code changes.
- No production server lifecycle automation beyond preserving the existing runtime workflow inputs.
