---
name: backend-naming
description: >
  Use when naming Java classes, methods, routes, SQL tables, or Spring beans in tchalanet-server — covers suffixes for handlers/ports/adapters/controllers/mappers, HTTP scope prefixes, response envelope types, SQL column conventions, and forbidden anti-patterns like XxxDto or XxxImpl.
---

# Conventions de nommage — Backend

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/NAMING.md`

## Principes

- Noms **searchable** (grep-friendly) et **predictable**
- Encoder l'**intention**, pas l'implémentation
- Langage du domaine (Ubiquitous Language)
- Un concept = un seul terme (pas de synonymes concurrents)

---

## Classes par responsabilité

| Rôle                  | Pattern                                                   |
| --------------------- | --------------------------------------------------------- |
| Command               | `XxxCommand` (record, typed IDs)                          |
| Command handler       | `XxxCommandHandler`                                       |
| Query                 | `XxxQuery` (record)                                       |
| Query handler         | `XxxQueryHandler`                                         |
| Domain event          | `XxxCreatedEvent`, `XxxCancelledEvent` (nom au **passé**) |
| Event listener        | `XxxEventListener` / méthode `onXxx(...)`                 |
| Output port read      | `XxxReaderPort`                                           |
| Output port write     | `XxxWriterPort`                                           |
| Output port repo      | `XxxRepositoryPort`                                       |
| External client       | `XxxClientPort` / `XxxGatewayPort`                        |
| JPA adapter           | `XxxJpaAdapter`                                           |
| JDBC adapter          | `XxxJdbcAdapter`                                          |
| JPA entity            | `XxxJpaEntity`                                            |
| JPA repository        | `XxxJpaRepository`                                        |
| Mapper générique      | `XxxMapper`                                               |
| Mapper web            | `XxxWebMapper`                                            |
| Mapper persistence    | `XxxPersistenceMapper`                                    |
| Catalog interface     | `XCatalog`                                                |
| Catalog impl          | `XCatalogImpl`                                            |
| Write service catalog | `XAdminService`                                           |
| Controller (défaut)   | `XxxController`                                           |
| Controller public     | `PublicXxxController`                                     |
| Controller admin      | `AdminXxxController`                                      |
| Controller tenant     | `TenantXxxController`                                     |
| Controller platform   | `PlatformXxxController`                                   |
| Feature service       | `XxxService` / `XxxOrchestrator`                          |
| Scheduler             | `XxxScheduler` — méthodes `tick()` / `runOnce()`          |
| Cache adapter         | `XxxCacheAdapter`                                         |

---

## Modèles par couche

| Couche                 | Pattern                               |
| ---------------------- | ------------------------------------- |
| Web input              | `XxxRequest`                          |
| Web output             | `XxxResponse`, `XxxItemResponse`      |
| Application read model | `XxxRow`, `XxxDetails`, `XxxSummary`  |
| Catalog model          | `XxxView`, `XxxSummaryView`           |
| **Interdit**           | ~~`XxxDto`~~, ~~`XxxModel`~~ (ambigu) |
| **Interdit features**  | ~~`DTO`~~ comme terme                 |

---

## Routes et scopes HTTP

- Prefix global `/api/v1` configuré dans `spring.mvc.servlet.path` → **ne pas le répéter dans les controllers**
- Scopes : `/public/...`, `/platform/...`, `/admin/...`, `/tenant/...`, `/_sdr/...`
- Noms de ressources en nouns pluriels : `/tickets`, `/draws`, `/outlets`
- Actions non-CRUD : `/approve`, `/settle`, `/void`, `/cancel`

---

## Réponses HTTP

- Tous les endpoints JSON : `ApiResponse<T>`
- Collections paginées : `TchPage<T>` (pas Spring `Page`)
- Erreurs : `ProblemDetail` (jamais wrappé dans `ApiResponse`)

---

## Tables et colonnes SQL

| Élément           | Convention                        |
| ----------------- | --------------------------------- |
| Tables            | `snake_case`                      |
| Colonnes          | `snake_case`                      |
| FK                | `<ref>_id`                        |
| Tenant            | `tenant_id`                       |
| Soft delete       | `deleted_at`                      |
| Audit             | `created_at`, `updated_at`        |
| Lock optimiste    | `version`                         |
| Migrations Flyway | `V###__short_snake_case_name.sql` |

---

## Directions de mapping autorisées

```
A) JpaEntity → XxxRow | XxxDetails | XxxSummary    (persistence → application)
B) XxxRow | XxxDetails → XxxResponse                (application → web)
C) XxxRequest → XxxCommand                          (web → application)
D) web params → XxxSearchCriteria → XxxQuery        (web → query)
```

**Forbidden** :

- `JpaEntity → WebResponse` (direct)
- `WebRequest → JpaEntity`
- `WebRequest → Domain Aggregate`
- `Controller → Repository` (direct)

---

## Anti-patterns interdits

- ~~`TicketServiceImpl`~~ — le suffixe `Impl` partout sans interface justifiée
- ~~`TkCtl`, `TixSvc`~~ — abréviations locales inventées
- ~~`Shop` vs `Outlet`~~ — deux noms pour le même concept
- ~~`/api/v1/tickets`~~ dans un `@RequestMapping` de controller
- ~~`@Autowired`~~ sur des champs (field injection)
- ~~`@Data`~~ Lombok
