# postgres-mandatory-remove-neon

## Why

The infra carries two contradictory stories about where the database lives, and the
contradiction has already caused a staging outage.

`envs/staging/.env` declares the local Postgres container, but `up-seq.sh` only includes
`docker-compose-postgres.yml` when `ENV=dev`, on the stated assumption that
"staging/prod use Neon". The result on staging is a Postgres container that holds all the
real data while being an *orphan* from Compose's point of view — every Compose command
warns about it, and a `--remove-orphans` would delete it.

Doppler's `stg` config meanwhile held `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD`
pointing at a Neon branch that no longer exists. Because `docker-compose-api.yml` loads
`env_file` in the order `.env.merged` then `.secrets`, those values silently beat the
committed local-Postgres config. The failure stayed hidden because the deploys that
followed had `build-api: skipped` and never recreated the API container; the first
restart that actually reloaded the secrets brought the API down with
`password authentication failed`.

Neon is not used anywhere today: `dev` and `prd` carry no datasource entries at all, no
recent deploy has used the `disposable-neon` mode, and the prod server does not exist yet.
Keeping the option costs an ambiguous topology, a live foot-gun, and a `NEON_API_KEY`
secret that grants database control for no benefit.

Self-hosting the production database means the backup guarantees a managed provider
supplies for free become ours. The existing backup path did not provide them: it called
`pg_dumpall -U postgres` while the superuser is `admin`, so it produced nothing, and
`staging-destroy.sh` gated destruction on `[ -s "$BACKUP_FILE" ]`, which a gzipped error
message satisfies. Destruction believed itself protected by a backup that did not exist.

## What

- Make the Postgres container part of the Compose stack for **every environment**, so it is
  owned by Compose instead of surviving as an orphan.
- Replace the broken backup path with scheduled, encrypted, off-site backups verified by
  restore, plus a weekly restore rehearsal — see `docs/operations/BACKUP-RESTORE.md`.
- Remove the `disposable-neon` database mode, the `create-neon-branch` and
  `delete-neon-branch` jobs, and the `NEON_API_KEY` / `NEON_PROJECT_ID` usage from
  `deploy-infra-runtime.yml`.
- Strip the Neon placeholders from `envs/*/deploy-secrets.env.local` so they cannot be
  uploaded to Doppler again.
- Establish the rule that Doppler holds **passwords only**, never connection targets:
  `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` live in committed env files.
- Update the comments and runbooks that still describe staging/prod as Neon-backed.

## Impact

- One database topology everywhere: the Compose-managed Postgres container. The
  environment-dependent branch in `up-seq.sh` disappears entirely.
- Backups become real: daily, encrypted with `age`, stored in Cloudflare R2 (a different
  vendor from the VM host), and proven by an actual restore rather than a file-size check.
- The Postgres container stops being an orphan on staging, so `--remove-orphans` is no
  longer able to destroy the database.
- Because Doppler no longer carries a connection target, a stale value can no longer
  silently override the committed configuration — the failure mode that caused this
  outage becomes impossible.
- `NEON_API_KEY` becomes removable from GitHub secrets.
- Operators lose the disposable-Neon-branch option for staging. Nothing uses it today; the
  isolated-runtime need is already served by `nightly-e2e-disposable-validation`, which
  relies on the Firebase Auth Emulator, not Neon.

## Non-goals

- Migrating existing staging data. The owner confirmed the staging database is disposable.
- Resolving the `app_user` password desync between Doppler and the running Postgres
  volume. That is an operational fix, not a topology change.
