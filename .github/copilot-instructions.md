# Copilot Instructions for Tchalanet Monorepo

## Project Overview
- **Monorepo** managed with Nx, containing:
  - `apps/`: Web (Angular/Nx), Mobile (Ionic/Angular), E2E
  - `libs/`: Shared code (API, Auth, Facades, UI, Web features)
  - `tchalanet-server/`: Spring Boot backend
  - `tchalanet-infra/`: Infrastructure (Docker, Keycloak, DB, scripts)
  - `tchalanet-docs/`: MkDocs documentation

## Key Workflows
- **Start full stack (dev):**
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
  ```
- **Web app (Angular/Nx):**
  - Start: `npm run start:web`
  - Build: `npm run build:web`
- **Mobile app (Ionic):**
  - See `apps/tchalanet-mobile/README.md` for details
- **Backend (Spring Boot):**
  - Build: `./mvnw clean install` in `tchalanet-server/`
  - Run: `./mvnw spring-boot:run`
- **Unit tests (Nx libs):**
  - Example: `nx test shared/api`

## Architecture & Patterns
- **Frontend:**
  - Angular with Nx workspace, Tailwind, DaisyUI, NgRx, i18n (ngx-translate), Keycloak auth
  - Shared UI and logic in `libs/`
  - Use Nx generators for new libs/apps
- **Backend:**
  - Spring Boot REST API, see `tchalanet-server/README.md`
  - Auth via Keycloak (see infra/keycloak/)
- **Infrastructure:**
  - Docker Compose for local dev, Keycloak, Postgres
  - Environment configs in `envs/`

## Conventions & Tips
- Use Nx CLI for workspace operations (e.g., `nx generate`, `nx test`)
- Prefer shared code in `libs/` for cross-app logic
- Keep infra scripts in `tchalanet-infra/scripts/`
- Document new features in `tchalanet-docs/`
- Use `README.md` in each lib/app for specific usage

## Examples
- Add a new shared util: `nx generate @nrwl/js:lib shared/utils/my-util`
- Run backend tests: `./mvnw test` in `tchalanet-server/`

## References
- Main project README: `/README.md`
- Backend: `/tchalanet-server/README.md`
- Infra: `/tchalanet-infra/`
- Docs: `/tchalanet-docs/`

---
For more details, see the referenced READMEs and infra scripts. Update this file as workflows evolve.
