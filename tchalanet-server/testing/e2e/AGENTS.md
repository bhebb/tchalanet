# AGENTS.md — E2E (testing/e2e)

**Working on E2E tests? Read [`README.md`](./README.md) in this folder first — fully.**
It is the single source of truth for entry points, env/URL config, scenarios, and the
stack mechanics. Agents repeatedly get stuck on the same handful of things; they are all
solved there.

Three traps that waste the most time (full detail in README §5–§7):

1. **Keycloak `keycloak-init` cache** — after a re-import against a *live* server,
   `users/count` returns 0 and `cashier` login fails while `super_admin` still works.
   Fix: `docker restart tchl-keycloak-dev`. (README §5)
2. **`make rebuild-api` is unreliable** — builds the JAR but doesn't recreate the running
   image, so code changes silently don't apply. Use the direct
   `docker build … local-dev` + `--no-deps --force-recreate api`. (README §7)
3. **Auth client** — password grant uses the *public* `tchalanet-swagger` client with **no**
   secret. Sending a secret → `invalid_client`. (README §3/§5)

Quickstart, env vars, markers, edge/Slack verification, and a troubleshooting table are all
in the README. Do not duplicate that content here — update the README instead.
