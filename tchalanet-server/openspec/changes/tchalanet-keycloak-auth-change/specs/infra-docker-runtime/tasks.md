# Tasks — infra-docker-runtime

- [ ] Add/confirm Docker image variables for Postgres, Redis, Traefik, Keycloak.
- [ ] Update `VERSIONS.md` with Docker image tags if changed.
- [ ] Add healthchecks for Postgres, Redis, Keycloak.
- [ ] Add Traefik routers for local hostnames.
- [ ] Add Keycloak provider mount/build path.
- [ ] Add realm import volume/path.
- [ ] Add `.env.example` for prod-ready values.
- [ ] Add smoke test script: `docker compose ps`, Keycloak health, Postgres health, Redis health.
