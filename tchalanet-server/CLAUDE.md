# CLAUDE.md — tchalanet-server

> **Lire d'abord** : `../CLAUDE.md` (règles transverses, secrets, OpenSpec)

---

## Stack backend

| Item            | Valeur                                         |
| --------------- | ---------------------------------------------- |
| Java            | 25 (`jakarta.*` uniquement)                    |
| Spring Boot     | 4.0.1 (`./mvnw`)                               |
| Architecture    | Hexagonal + CQRS — 4 couches strictes          |
| ORM / migration | JPA + Hibernate · Flyway (`ddl-auto=validate`) |
| Mappers         | MapStruct + `CommonIdMapper`                   |
| Cache           | Redis + Caffeine                               |
| Auth            | OAuth2 Resource Server (JWT/Keycloak)          |
| Tests           | JUnit 5 · AssertJ · Testcontainers             |

---

## Skills backend (`tchalanet-server/.claude/skills/`)

Couches : `hexagonal-cqrs` · `core-module` · `catalog-module` · `feature-module` · `common-module`
Patterns : `backend-typed-ids` · `domain-events` · `backend-rls` · `persistence-flyway` · `backend-events`
Conventions : `backend-naming` · `backend-architecture` · `backend-testing` · `testing-conventions`
Spring : `spring-boot-core` · `spring-security` · `java-best-practices`

---

## Do ✅

- Constructor injection uniquement
- Commands/Queries = `record` + typed IDs (jamais `UUID` brut hors persistence)
- Controllers thin : validation + mapping + délégation au bus
- Domain layer = pure Java, framework-free, déterministe
- Events publiés `AfterCommit` via `DomainEventPublisher`
- Listeners idempotents via `ProcessedEventPort`
- Flyway pour toute modification de schéma
- `ApiResponse<T>` sur tous les controllers JSON
- `TchPage<T>` pour les collections (pas Spring `Page`)
- `BaseTenantEntity` pour les tables tenantées, `BaseEntity` sinon

## Don't ❌

- `javax.*` · `UUID` hors persistence · `UUID.randomUUID()` dans le domaine
- Logique métier dans `catalog/` · domain events depuis `catalog/`
- `core/` dépend de `features/` · `common/` contient du métier
- `WHERE tenant_id = ?` dans le code Java (RLS fait le travail)
- `ddl-auto=update/create` · `@RepositoryRestResource`
- `@Autowired` champ · `@Data` Lombok · `XxxDto` · `DTO` dans features
- `/api/v1` dans les `@RequestMapping` · suffixe `Impl` sans interface

---

## Commandes

```bash
./mvnw clean verify        # build + tests complets
./mvnw spring-boot:run     # démarrage local (port 8080)
./mvnw test -pl :module    # tests d'un module
```

---

## Références backend

| Besoin               | Fichier                                  |
| -------------------- | ---------------------------------------- |
| Architecture couches | `openspec/context/10-non-negotiables.md` |
| Nommage              | `docs/NAMING.md`                         |
| Typed IDs            | `docs/conventions/typed_ids.md`          |
| RLS / multi-tenant   | `docs/conventions/persistence/rls.md`    |
| Events               | `docs/conventions/event_model.md`        |
| Idempotency          | `docs/conventions/idempotency.md`        |
| Tests                | `docs/conventions/testing.md`            |
| Domaine métier       | `src/**/DOMAIN_*.md`                     |
