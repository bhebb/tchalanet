# Documentation — Tchalanet Infrastructure

## Quick Start

- **[../QUICK-START.md](../QUICK-START.md)** — Local dev stack from scratch (canonical, ENV=dev)

## Environment & Configuration

- **[ENV-ARCHITECTURE.md](ENV-ARCHITECTURE.md)** — compose.env vs .env vs .secrets, merge flow, examples
- **[api.env.example](api.env.example)** — Reference env file for the Spring Boot API
- **[DEMARRAGE.md](DEMARRAGE.md)** — Service startup guide with troubleshooting
- **[LAN-SETUP.md](LAN-SETUP.md)** — Run Tchalanet on a Manjaro LAN host and access it from a Mac

## Secrets Management

- **[DOPPLER-SETUP-GUIDE.md](DOPPLER-SETUP-GUIDE.md)** — Doppler project setup, service tokens, secret rotation, download to server

## Services

- **[EDGE-SERVICE.md](EDGE-SERVICE.md)** — Edge service Docker config : build, démarrage, env vars, intégration backend

## Images & Deployment

- **[operations/IMAGES-DEPLOYMENT.md](operations/IMAGES-DEPLOYMENT.md)** — Build and publish Docker images to GHCR
- **[operations/DEPLOYMENT.md](operations/DEPLOYMENT.md)** — Staging/production deployment (Hetzner)
- **[operations/HETZNER.md](operations/HETZNER.md)** — Server creation, firewall, network (Hetzner Cloud)
- **[operations/OPERATIONS.md](operations/OPERATIONS.md)** — Day-to-day operations, backups, restores
- **[reference/QUICK-REFERENCE.md](reference/QUICK-REFERENCE.md)** — Command cheat-sheet

## Scripts

- **[reference/scripts-index.md](reference/scripts-index.md)** — Inventory of all scripts with purpose and usage

## Mobile

- **[mobile-distribution-v0.md](mobile-distribution-v0.md)** — Flutter app distribution strategy

## Reference

- **[reference/](reference/)** — Additional reference material
- **[BUILD-LOCAL-VS-PUBLISHED.md](BUILD-LOCAL-VS-PUBLISHED.md)** — Local build vs published image trade-offs
- **[VITE-ALLOWED-HOSTS.md](VITE-ALLOWED-HOSTS.md)** — Vite dev server allowed hosts for local SSL

## Compose Files

See **[../compose/docker-compose.index.md](../compose/docker-compose.index.md)** for the index of Docker Compose files and when to use each.
