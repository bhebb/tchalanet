# Spec: CI/CD workflows sobres

## ADDED Requirements

### Requirement: Minimal PR workflows

Le repo SHALL define lightweight PR workflows for backend, web, edge, infra, and docs.

#### Scenario: Backend-only PR

- **WHEN** a PR changes files under `tchalanet-server/**`
- **THEN** `server-pr.yml` SHALL run
- **AND** web, edge, docs, and deploy workflows SHALL NOT run because of that change alone

#### Scenario: Edge-only PR

- **WHEN** a PR changes files under `tchalanet-edge-service/**`
- **THEN** `edge-pr.yml` SHALL run TypeScript/Fastify checks
- **AND** it SHALL NOT build or push Docker images

### Requirement: Manual staging deployment

Staging deployment SHALL be triggered manually via `workflow_dispatch`.

#### Scenario: Manual deploy with default tag

- **WHEN** `deploy-staging.yml` is triggered without `image_tag`
- **THEN** the workflow SHALL compute `IMAGE_TAG=sha-<short_sha>`
- **AND** API/Web/Edge images built in the run SHALL use that same tag

#### Scenario: No latest tag

- **WHEN** any image is published by staging deployment
- **THEN** it SHALL NOT publish or update a `latest` tag

### Requirement: No automatic production deployment

Production deployment SHALL NOT run automatically in v0.

#### Scenario: Push main

- **WHEN** code is pushed to `main`
- **THEN** production deployment SHALL NOT run

### Requirement: Keycloak realm workflow archived

Realm create/update/delete SHALL NOT be exposed as a standalone destructive workflow in v0.

#### Scenario: Delete realm from GitHub Actions

- **WHEN** a user looks for a workflow action to delete Keycloak realm
- **THEN** no active workflow SHALL provide that operation
