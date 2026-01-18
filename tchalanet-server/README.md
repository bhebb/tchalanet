# Tchalanet Server

Backend service for the Tchalanet platform.

## Architecture

- Spring Boot
- Hexagonal architecture (Ports & Adapters)
- CQRS (command/query separation)
- Multi-tenant by default (Postgres RLS)

## Rules & context

- OpenSpec backend rules:
  → `../openspec/context/20-backend-rules.md`
- Global non-negotiables:
  → `../openspec/context/10-non-negotiables.md`

## Documentation

- Technical standards: `tchalanet-server/docs/`
- Domain rules: `src/main/java/**/DOMAIN*.md`
