# Spec: Mobile Flutter distribution v0

## ADDED Requirements

### Requirement: Android-first seller distribution

The mobile distribution strategy SHALL prioritize Android for seller/agent pilots.

#### Scenario: Seller pilot

- **WHEN** a seller pilot is prepared
- **THEN** Android SHALL be the default target platform
- **AND** iOS SHALL be treated as optional unless explicitly required

### Requirement: No automatic heavy mobile builds

Mobile builds SHALL NOT run automatically on every PR or push in v0.

#### Scenario: Mobile code PR

- **WHEN** mobile code changes in a PR
- **THEN** lightweight checks MAY run
- **AND** release builds SHALL NOT run automatically

### Requirement: Manual mobile release later

Mobile release builds SHALL be manual when introduced.

#### Scenario: Build mobile release

- **WHEN** a mobile release workflow is introduced
- **THEN** it SHALL be triggered by `workflow_dispatch`
- **AND** require explicit `platform`, `env`, and `release_track` inputs

### Requirement: Environment configuration

The Flutter app SHALL distinguish dev, staging, and prod environments.

#### Scenario: Staging build

- **WHEN** a staging build is produced
- **THEN** it SHALL use staging API/Auth URLs
- **AND** include environment/version metadata in the build

### Requirement: No embedded backend secrets

The mobile app SHALL NOT embed backend secrets.

#### Scenario: Inspect mobile config

- **WHEN** mobile runtime config is inspected
- **THEN** it MAY contain public URLs and public OIDC client IDs
- **AND** it SHALL NOT contain server passwords, API secrets, or privileged tokens

### Requirement: Staging dependency disclosure

Staging disposable infrastructure SHALL not be treated as stable client production.

#### Scenario: Client test app uses staging

- **WHEN** a client test app points to staging
- **THEN** the team SHALL document that the environment may be recreated
- **AND** it SHALL not be considered production SLA
