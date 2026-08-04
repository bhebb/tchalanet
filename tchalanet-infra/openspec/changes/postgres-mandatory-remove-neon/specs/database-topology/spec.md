# database-topology Delta

## ADDED Requirements

### Requirement: Postgres Is A Compose-Managed Service In Every Environment

Every stack SHALL include the Postgres container as a Compose-managed service so that no
environment runs a database the Compose project does not own.

#### Scenario: Staging stack includes Postgres

- **WHEN** the deployment sequence composes the staging stack
- **THEN** `docker-compose-postgres.yml` is part of the compose file set
- **AND** `postgres` is started as a core service before the API
- **AND** Compose reports no orphan container for the database.

#### Scenario: Orphan removal cannot destroy the staging database

- **WHEN** an operator runs a Compose command with `--remove-orphans` against staging
- **THEN** the Postgres container is recognised as part of the project
- **AND** it is not removed.

### Requirement: Doppler Carries Database Passwords Only

Secret storage SHALL hold database credentials but SHALL NOT hold database connection
targets, so that a stale secret cannot silently redirect a service to another database.

#### Scenario: Connection target lives in committed configuration

- **WHEN** the API resolves its datasource
- **THEN** `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` come from the committed
  `envs/<env>/.env`
- **AND** only `SPRING_DATASOURCE_PASSWORD` comes from Doppler.

#### Scenario: Secrets upload cannot reintroduce a connection target

- **WHEN** environment secrets are uploaded to Doppler from `deploy-secrets.env.local`
- **THEN** the file contains no `SPRING_DATASOURCE_URL` or `SPRING_DATASOURCE_USERNAME`
  entry.

### Requirement: Self-Hosted Databases Have Verified Off-Site Backups

Because no environment uses a managed database, every environment holding data that
matters SHALL have scheduled backups stored off the host, encrypted, and proven usable by
restore.

#### Scenario: Daily backup is verified by restoring it

- **WHEN** the scheduled backup runs
- **THEN** it dumps roles and the application database
- **AND** it restores that dump into a throwaway container and counts the restored tables
- **AND** the backup fails if the restore fails or yields zero tables
- **AND** a file-size check alone is never accepted as verification.

#### Scenario: Backups survive the loss of the host

- **WHEN** a backup completes
- **THEN** it is encrypted with a public key the host cannot decrypt with
- **AND** it is uploaded to object storage held by a different vendor than the host.

#### Scenario: Restore is rehearsed before it is needed

- **WHEN** the weekly rehearsal runs
- **THEN** it decrypts and restores the most recent backup on an isolated runner
- **AND** it touches no live environment
- **AND** it fails loudly if the backup cannot be restored.

#### Scenario: Destruction cannot proceed on a failed backup

- **WHEN** an operator destroys an environment with the pre-destroy backup enabled
- **THEN** destruction aborts if the backup step fails
- **AND** proceeding anyway requires an explicit opt-out.

## REMOVED Requirements

### Requirement: Staging Can Deploy Against A Disposable Neon Branch

**Reason**: The mode wrote its ephemeral connection string into the persistent Doppler
config while its cleanup job deleted only the branch, leaving every environment that used
it pointing at a destroyed database. No recent deploy used it, and disposable-runtime
validation is already covered by `nightly-e2e-disposable-validation` through the Firebase
Auth Emulator.

**Migration**: Deploy with `database_mode=configured`, which targets the Compose-managed
Postgres container. For an isolated validation runtime, use the nightly validation path.
