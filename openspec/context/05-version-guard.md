# Version Guard — Runtime & Framework Awareness

This context enforces version correctness.
Any generated code MUST comply with the versions defined in `VERSIONS.md`.

---

## Source of Truth

- Versions file: `VERSIONS.md`
- Backend POM: `tchalanet-server/pom.xml`
- Frontend: `tchalanet-web/package.json`, `tchalanet-web/pnpm-lock.yaml`
- Infra: `tchalanet-infra/envs/common/compose.env`

---

## Backend (Java / Spring Boot)

### Runtime

- Java: **25**
- Spring Boot: **4.x**

### MUST

- Use Java 21+ language features when relevant (records, switch expressions, pattern matching)
- Use Spring Boot 4 APIs (Jakarta EE 10+)
- Prefer constructor binding & records
- Use `@ConfigurationProperties` (no legacy setters)

### MUST NOT

- Use deprecated Spring APIs
- Use `javax.*` (Jakarta only)
- Use legacy validation or servlet APIs
- Use outdated Spring Security configuration patterns

---

## Frontend (Angular / Nx)

### Runtime

- Angular: **20.x**
- Nx: **21.x**
- TypeScript: **5.8.x**

### MUST

- Use standalone components
- Use modern Angular control flow (`@if`, `@for`)
- Use signals where applicable
- Use latest Angular Material patterns

### MUST NOT

- Generate NgModules
- Use deprecated lifecycle hooks
- Use legacy RxJS patterns when signals fit better

---

## Mobile (Ionic / Capacitor)

### Runtime

- Ionic: **8.x**
- Capacitor: **7.x**

Rules:

- Use Capacitor APIs (not Cordova)
- No deprecated plugins

---

## Infra / Edge

### Node

- Runtime Node: **20.19.x**

### MUST

- Use modern ES syntax supported by Node 20
- Avoid polyfills for obsolete runtimes

---

## Rule

If a feature or API did not exist in the declared version:
→ DO NOT use it.

If multiple approaches exist:
→ Prefer the most modern approach supported by the current version.

If uncertain:
→ Ask for clarification BEFORE generating code.
