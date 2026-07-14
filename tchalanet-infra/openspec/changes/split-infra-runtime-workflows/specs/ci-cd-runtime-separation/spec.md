# ci-cd-runtime-separation Delta

## ADDED Requirements

### Requirement: Staging Infra Workflow Owns Core Services

The staging infrastructure workflow SHALL manage the staging server lifecycle and the core infrastructure services only.

#### Scenario: Deploy staging core services

- **WHEN** the staging infrastructure workflow runs with `infra_action=deploy-core`, `create`, or `recreate`
- **THEN** it provisions or reuses the staging host as requested
- **AND** it configures Docker networks and remote infra files
- **AND** it starts and verifies Traefik and Redis
- **AND** it does not build or deploy API or edge-service images.

### Requirement: Runtime Workflow Owns Runtime Services

The runtime deployment workflow SHALL build, pull, and deploy API and edge-service images only after core services are ready.

#### Scenario: Runtime deploy with missing core services

- **WHEN** the runtime deployment workflow runs and Traefik or Redis is not ready
- **THEN** the workflow fails before replacing API or edge-service containers
- **AND** the failure message directs the operator to run the staging infrastructure workflow.

#### Scenario: Runtime deploy with ready core services

- **WHEN** the runtime deployment workflow runs and core services are ready
- **THEN** it may deploy API, edge-service, or both
- **AND** it does not start, recreate, or implicitly repair Traefik or Redis.
