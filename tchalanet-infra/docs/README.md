# Documentation — Tchalanet Infrastructure

## Quick Start

- **[../QUICK-START.md](../QUICK-START.md)** — Local dev stack from scratch (canonical, ENV=dev)

## Environment & Configuration

- **[ENV-ARCHITECTURE.md](ENV-ARCHITECTURE.md)** — compose.env vs .env vs .secrets, merge flow, examples
- **[api.env.example](api.env.example)** — Reference env file for the Spring Boot API
- **[DEMARRAGE.md](DEMARRAGE.md)** — Service startup guide with troubleshooting

## Secrets Management

- **[DOPPLER-SETUP-GUIDE.md](DOPPLER-SETUP-GUIDE.md)** — Doppler project setup, service tokens, secret rotation, download to server

## Images & Deployment

- **[IMAGES-DEPLOYMENT.md](IMAGES-DEPLOYMENT.md)** — Build and publish Docker images to GHCR
- **[DEPLOYMENT.md](DEPLOYMENT.md)** — Staging/production deployment (Hetzner)
- **[HETZNER.md](HETZNER.md)** — Server creation, firewall, network (Hetzner Cloud)
- **[OPERATIONS.md](OPERATIONS.md)** — Day-to-day operations, backups, restores
- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** — Command cheat-sheet

## Scripts

- **[scripts-index.md](scripts-index.md)** — Inventory of all scripts with purpose and usage

## Mobile

- **[mobile-distribution-v0.md](mobile-distribution-v0.md)** — Flutter app distribution strategy

## Reference

- **[reference/](reference/)** — Additional reference material
- **[ACTION-REALM-REGEN.md](ACTION-REALM-REGEN.md)** — How to regenerate the Keycloak realm export
- **[BUILD-LOCAL-VS-PUBLISHED.md](BUILD-LOCAL-VS-PUBLISHED.md)** — Local build vs published image trade-offs
- **[VITE-ALLOWED-HOSTS.md](VITE-ALLOWED-HOSTS.md)** — Vite dev server allowed hosts for local SSL

## Compose Files

See **[../compose/docker-compose.index.md](../compose/docker-compose.index.md)** for the index of Docker Compose files and when to use each.
