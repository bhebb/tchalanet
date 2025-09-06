# ADR 0001 — Architecture Decisions for Tchalanet

Date: 2025-09-06  
Status: Accepted  
Context: Initial setup of the Tchalanet platform (Web, Mobile, Server, Infra, Docs).

---

## 1. Overall Architecture
- **Multi-application monorepo (logical)**, split into:
    - `tchalanet-web` → Angular 20 + Nx
    - `tchalanet-mobile` → Ionic Angular + Capacitor
    - `tchalanet-server` → Spring Boot 3.3 (Maven)
    - `tchalanet-docs` → MkDocs (Material)
    - `tchalanet-infra` → Terraform, CI/CD, Docker Compose

- **Communication**: REST APIs secured with OIDC (Keycloak).
- **Persistence**: PostgreSQL with **Row-Level Security (RLS)** for tenants, managed by Flyway.

---

## 2. Web (tchalanet-web)
- Framework: **Angular 20 + Nx (angular-monorepo preset)**.
- **Bundler**: esbuild (fast dev & build).
- **Unit test runner**: Vitest (fast, modern, ESM support).
- **E2E test runner**: Playwright.
- **CI/CD**: GitHub Actions.
- **Component defaults**:
    - Standalone components
    - ChangeDetection: OnPush
    - Style: SCSS
    - Flat file structure
    - Inline HTML/CSS by default (can override with `--inlineTemplate=false`)
    - Prefix: `tch-` (e.g., `<tch-ticket-card>`)

- **Lib structure**:
    - `libs/shared/ui` → UI components
    - `libs/config` → runtime config
    - `libs/<domain>/feature-*` → feature modules (tickets, tenants…)

---

## 3. Mobile (tchalanet-mobile)
- Framework: **Ionic Angular** with Capacitor.
- **Why Ionic?**:
    - Reuse Angular ecosystem & shared code
    - Faster MVP than Flutter/React Native
    - Easy access to native features via Capacitor plugins

- **Features planned (MVP)**:
    - OIDC login (PKCE with Capacitor OAuth2 plugin)
    - Tenant selector, theme & i18n sync with web
    - Tickets list & create (with offline queue + sync)
    - Deep links (`myapp://tickets/:id`)
    - Light telemetry (login, create_ticket)

---

## 4. Server (tchalanet-server)
- Framework: **Spring Boot 3.3** (Java 21, Maven build).
- **Architecture style**: Hexagonal (domain, app, api, infra).
- **Key libs**:
    - Spring Web, Validation, Security, OAuth2 Resource Server
    - Spring Data JPA, Flyway, PostgreSQL driver
    - MapStruct for DTO ↔ domain mapping
    - Actuator + Micrometer Prometheus
    - Testcontainers (Postgres, JUnit 5)

- **Persistence**:
    - PostgreSQL with **RLS per tenant_id**
    - Flyway migrations, idempotent & timestamped

- **Security**:
    - Keycloak OIDC (JWT → Spring authorities)
    - Capabilities model on top of roles & tenant config
    - Error responses in RFC 7807 format (ProblemDetails)

- **Packaging**:
    - Build fat JAR with Maven
    - Docker image via `spring-boot:build-image`

---

## 5. Documentation (tchalanet-docs)
- Tool: **MkDocs Material**
- Structure:
    - `docs/architecture/overview.md` → mermaid diagrams
    - `docs/backend/` → setup, persistence, security, API
    - `docs/web/` → Angular/Nx architecture, UI
    - `docs/devops/` → CI/CD, observability
    - `docs/adr/` → Architecture Decision Records
- ADRs: one file per major decision (e.g., stack, build tool, test strategy)

---

## 6. Infra
- **Dev**: Docker Compose (Postgres + Keycloak).
- **CI/CD**: GitHub Actions per module (web, mobile, server, infra).
- **Terraform** planned for Azure (in `tchalanet-infra`).

---

## 7. Testing Strategy
- **Web & Mobile**:
    - Unit: Vitest + Testing Library
    - E2E: Playwright
- **Server**:
    - Unit: JUnit 5
    - Slice: @WebMvcTest, @DataJpaTest
    - Integration: Testcontainers Postgres
    - Contract: OpenAPI spec + validation
- **Docs**:
    - MkDocs build check in CI

---

## 8. Naming Conventions
- Angular components: `<tch-*>`
- Spring packages: `com.tch.server.<domain>.{api,app,domain,infra}`
- Database: `snake_case` table/column names
- Git commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…)

---

## Consequences
- Clear separation between apps, server, mobile, docs, infra.
- Fast iteration with Nx (web), Ionic (mobile), Maven + Spring (server).
- Extensible docs with MkDocs + ADRs.
- Strong CI/CD setup with GitHub Actions.
- Easy multi-tenant handling with Postgres RLS.
