# Tasks

## Compose topology

- [x] Include `docker-compose-postgres.yml` unconditionally in `scripts/utils/up-seq.sh` — every environment, prod included.
- [x] Add `postgres` to the core service start order for every environment in `up-seq.sh`.
- [x] Update the `SPRING_DATASOURCE_URL` comment in `compose/docker-compose-api.yml` to stop describing staging/prod as Neon-backed.
- [x] Verify with `docker compose config` that staging resolves the Postgres service and no orphan warning remains.

## Environment files

- [x] Remove the Neon rationale comment from `envs/common/compose.env` (Postgres container is no longer dev-only).
- [x] Update the `SPRING_DATASOURCE_URL` guidance in `envs/common/api.env` to point at the committed per-env value instead of a Neon connection string.
- [x] Update the header comment in `envs/common/postgres.env`.
- [x] Replace the "Neon can make the Actuator DB health indicator slow" comment in `envs/staging/api.env` with the real reason the DB health indicator stays disabled.
- [x] Strip the Neon placeholder lines from `envs/staging/deploy-secrets.env.local` and `envs/prod/deploy-secrets.env.local` so they cannot be re-uploaded to Doppler.

## Deploy workflow

- [x] Remove the `disposable-neon` option from the `database_mode` input in `.github/workflows/deploy-infra-runtime.yml` and make `configured` the only value.
- [x] Delete the `create-neon-branch` and `delete-neon-branch` jobs and the `use_disposable_neon` plan output.
- [x] Remove `RUNTIME_DATABASE_URL` wiring from the `deploy` job where it only carried the Neon branch URL.
- [x] Remove the `RUNTIME_DATABASE_URL` override block from `scripts/remote/deploy-runtime-services.sh` — it appended `SPRING_DATASOURCE_URL` to `.secrets`, which is the pattern this change forbids.
- [x] Drop the Neon validation branches from the `plan` step script.
- [x] Reword the `destroy_database` input description so it no longer says "Neon database schema".
- [x] Reword the `skip_backup` description in `.github/workflows/deploy-staging.yml` ("safe for Neon-backed staging").

## Documentation

- [x] Update `.github/workflows/README.md` where it documents `NEON_API_KEY`.
- [x] Update `docs/operations/runbooks/RB-00-secrets-checklist.md` — remove the Neon connection-string note and record the passwords-only rule for Doppler.
- [x] Update `docs/operations/runbooks/RB-01-staging-provision.md` — Compose-managed Postgres, orphan check, and the silent backup failure in `staging-destroy.sh`.
- [x] Document in `docs/architecture/ENV-ARCHITECTURE.md` that `SPRING_DATASOURCE_URL` / `USERNAME` are committed per-env and only the password comes from Doppler.

## Backup and restore

- [x] Add `scripts/remote/pg-backup.sh` — correct superuser, globals + custom-format dump, verification by real restore, `age` encryption, upload to R2.
- [x] Add `scripts/remote/pg-restore.sh` — latest-or-named restore, typed confirmation, and a `DRY_RUN=1` rehearsal mode that touches nothing.
- [x] Add `.github/workflows/db-backup.yml` — daily scheduled backup plus a restore rehearsal, so a dead backup surfaces before it is needed.
- [x] Deprecate `scripts/remote/staging-backup.sh` with a pointer instead of leaving a broken backup in place.
- [x] Fix the pre-destroy backup in `scripts/hcloud/staging-destroy.sh` to call the verified script and **fail closed** instead of warning and continuing.
- [x] Write `docs/operations/BACKUP-RESTORE.md` — RPO/RTO, key handling, restore procedure, and the accepted limits.
- [x] Amend the root change `external-auth-managed-postgres-observability-v0` to record the move from managed to self-hosted PostgreSQL and the backup obligations it creates.

## Cleanup and verification — owner actions

- [x] Confirm Doppler `stg` carries no `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME`; `dev` and `prd` carry none either.
- [x] `openspec validate --strict`.
- [x] Delete `NEON_API_KEY` from GitHub secrets and `NEON_PROJECT_ID` from repo variables.
- [ ] Delete the orphaned Neon project itself (id was `flat-silence-79650612`) from the Neon console — the GitHub credentials are gone, but the project may still exist and bill.
- [ ] Resolve the `app_user` password desync on staging with `ALTER USER` before the first deploy under this change.
- [ ] Run `deploy-infra-runtime.yml` against staging with `database_mode=configured` and confirm the API reaches the database.
- [ ] Create the R2 bucket and a scoped S3 token; generate the `age` keypair with `age-keygen`.
- [ ] Add secrets `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET`, `BACKUP_AGE_PUBLIC_KEY`, `BACKUP_AGE_PRIVATE_KEY` (`CLOUDFLARE_ACCOUNT_ID` already exists and is reused); store the private key in the team password manager as well.
- [ ] Install `age` and `rclone` on the staging VM (`apt-get install -y age rclone`) — both are required by `pg-backup.sh`.
- [ ] Run `db-backup.yml` manually once with `rehearse_restore=true` to prove the whole loop end to end.
