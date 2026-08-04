# Design — postgres-mandatory-remove-neon

## Decision 1 — Doppler holds passwords, never connection targets

The outage was not caused by a wrong value so much as by a wrong *location*.
`docker-compose-api.yml` loads `env_file` in the order `.env.merged` then `.secrets`, and
Compose gives the last file precedence. Any `SPRING_DATASOURCE_URL` that reaches Doppler
therefore overrides the committed configuration invisibly — no diff, no warning, no review.

Rule going forward:

| Value | Home | Rationale |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | committed `envs/<env>/.env` | Topology is reviewable and diffable |
| `SPRING_DATASOURCE_USERNAME` | committed `envs/<env>/.env` | Same |
| `SPRING_DATASOURCE_PASSWORD` | Doppler | Genuine secret |

This is why the change strips the Neon entries from `deploy-secrets.env.local` rather than
only from Doppler: those files are the upload source, so leaving them would let the values
return on the next secrets push.

## Decision 2 — Postgres joins the Compose stack for dev and staging only

`up-seq.sh` currently branches on `ENV` to decide whether Postgres is part of the stack.
Removing the branch for dev and staging makes the container Compose-owned, which fixes the
orphan warning and the `--remove-orphans` hazard.

Prod is deliberately excluded. The active root change
`external-auth-managed-postgres-observability-v0` states that DigitalOcean Managed
PostgreSQL is the production V0 choice, with Neon and Supabase acceptable only for
non-production experiments. Making the container mandatory for prod here would silently
overrule an architectural decision that lives outside this project's OpenSpec. Prod has no
server and no datasource configuration today, so nothing is lost by leaving it untouched.

If the owner decides to run prod on the container as well, that belongs in an amendment to
the root change, not here.

## Decision 3 — Disposable environments keep the Firebase-emulator path

Removing `disposable-neon` takes away the only mechanism for a throwaway database per
deploy. That capability is not lost in practice: `nightly-e2e-disposable-validation`
already provides isolation through the Firebase Auth Emulator and per-SHA images, and no
run in the recent history used `disposable-neon`.

The mode also had a defect worth recording: `create-neon-branch` wrote its ephemeral
connection string into the persistent Doppler config while `delete-neon-branch` removed
only the branch. Every use therefore left Doppler pointing at a database that had just been
destroyed. Removing the mode removes the defect.

## Risk

The `app_user` password in the running staging Postgres volume does not match Doppler's
`APP_DB_PASSWORD`. That desync predates this change and is not addressed by it, but staging
will not come up until it is resolved operationally with an `ALTER USER`. Sequencing the
password fix before the first deploy under this change avoids attributing the failure to
the wrong cause.
