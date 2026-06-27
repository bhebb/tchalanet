# Naming Conventions (Server)

> **Status**: NORMATIVE  
> **Applies to**: `tchalanet-server` (common / core / catalog / features)  
> **Goal**: cohérence des noms pour réduire la charge cognitive, améliorer la recherche et éviter la dérive d'architecture.

---

## 1) Principes (MUST)

- Les noms MUST être **searchable** (grep-friendly) et **predictable**.
- Les noms MUST encoder **l'intention**, pas l'implémentation.
- Préférer le langage du domaine (Ubiquitous Language).
- Éviter les abréviations sauf standard du projet (ex : RLS, SDR, API).
- Un concept = un seul terme (pas de synonymes concurrents).

---

## 2) Modules & packages

### 2.1 Top-level modules (fixes)

- `common/` = transversal technique uniquement
- `catalog/` = données de référence read-mostly partagées
- `core/` = domaines critiques (money, tickets, draw, audit, security, limits)
- `features/` = orchestration / BFF / composition multi-domaines

### 2.2 Bounded context package naming

- Utiliser lowercase, un seul segment : `core.sales`, `core.draw`, `platform.audit`, `platform.accesscontrol`
- Éviter : pluriels fantaisistes, séparateurs mixtes, dossiers `impl` inutiles.

---

## 3) Nommage par responsabilité

### 3.1 Commands / Queries

- Commands (write) : `XxxCommand`
- Queries (read) : `XxxQuery`
- Rules : Commands/Queries MUST être `record` et MUST utiliser des typed IDs (pas de UUID bruts).

Exemples : `SellTicketCommand`, `GetTicketStatusQuery`.

### 3.2 Handlers (use cases)

- Command handler : `XxxCommandHandler`
- Query handler : `XxxQueryHandler`
- Si un style court (`XxxHandler`) existe, le conserver uniquement si cohérent dans le BC.

### 3.3 Ports

- Read port : `XxxReaderPort`
- Write port : `XxxWriterPort`
- Repo-like : `XxxRepositoryPort` (si sémantique repo)
- External systems : `XxxClientPort` / `XxxGatewayPort` (choisir un pattern par BC)

### 3.4 Adapters (infra)

- JPA adapter : `XxxJpaAdapter`
- JDBC adapter : `XxxJdbcAdapter`
- HTTP client : `XxxHttpClient` / `XxxProviderClient`
- Cache : `XxxCacheAdapter`
- Spring Data interface : `XxxJpaRepository`
- JPA entity : `XxxJpaEntity`
- Mapper : `XxxMapper`, `XxxWebMapper`, `XxxPersistenceMapper`

### 3.5 Web controllers

- `XxxController` (par défaut)
- Si scope : `PublicXxxController`, `AdminXxxController`, `TenantXxxController`, `PlatformXxxController`
- Les noms doivent rester stables si la route change.

### 3.6 DTOs web

- Requests : `XxxRequest`
- Responses : `XxxResponse`
- List items : `XxxItemResponse`
- Éviter de fuir le terme "View" dans les DTOs web.
- Si vous utilisez des projections côté application/query : `XxxView` (application-side) et `XxxRow` pour les lignes de liste.

---

## 4) Routes & scopes

### 4.1 Controller `@RequestMapping` prefixes

- `spring.mvc.servlet.path = /api/v1` ⇒ controllers MUST NOT inclure `/api/v1` dans les mappings.
- Utiliser uniquement : `/public/...`, `/platform/...`, `/admin/...`, `/tenant/...`, `/_sdr/...`.

### 4.2 Endpoint naming

- Utiliser des noms de ressources (nouns) : `/tickets`, `/draws`, `/outlets`.
- Sous-ressources pour relations : `/tickets/{id}/cancel`.
- Actions explicites uniquement si non-CRUD : `/approve`, `/settle`, `/void`.

---

## 5) Typed IDs (wrappers)

### 5.1 Noms

- Suffixe MUST être `Id` : `TenantId`, `TicketId`, `PayoutId`.

### 5.2 Méthodes standards

- `of(UUID)`, `nullableOf(UUID)`, `parse(String)`
- Fournir `StringToXxxIdConverter`.
- MapStruct helper : `CommonIdMapper` (mapToXxxId / mapFromXxxId).

---

## 6) Events

### 6.1 Domain events

- Nom au passé : `XxxCreatedEvent`, `XxxCancelledEvent`.
- Packaging : `core.<domain>.application.event`.
- Publier **AfterCommit** uniquement.

### 6.2 Integration events

- Versionnés : `XxxHappenedV1`, `XxxExecutedV1`.
- Packaging : `features.integration.event` ou `core.<domain>.infra.integration.event`.

### 6.3 Listeners

- Classe : `XxxEventListener`
- Méthode : `onXxx(...)` (past tense)
- Les listeners ne modifient pas l'aggregate source ; ils déclenchent des commands ou des effets secondaires.

---

## 7) Persistence

### 7.1 Tables & colonnes SQL

- Tables : `snake_case`
- Colonnes : `snake_case`
- FK : `<ref>_id`
- Tenant column : `tenant_id`
- Soft delete : `deleted_at`
- Audit : `created_at`, `updated_at`
- Optimistic lock : `version`

### 7.2 Flyway

- Nom : `V###__short_snake_case_name.sql` (ex : `V040__rls_policies.sql`).

---

## 8) Tests

- Test class : `XxxTest` (unit), `XxxIT` (integration)
- Test method : camelCase (Java)
- `@DisplayName("should <expected> when <condition>")` est canonical.

---

## 9) Anti-patterns (forbidden)

- Suffixe `Impl` partout (`TicketServiceImpl`) sauf si interface nécessaire.
- Abréviations locales inventées (`TkCtl`, `TixSvc`).
- Deux noms pour même concept (`Shop` vs `Outlet`).
- Routes contenant `/api/v1` dans les controllers.

---

## 10) Touchpoints & références

- `ROUTING_AND_API_PATHS_V1.md`
- `WEB_API.md`
- `TYPED_IDS.md`
- `RLS.md`
- `TESTING.md`

---

## 11) Batch / Scheduling

### 11.1 Schedulers (MUST)

- Classe : `XxxScheduler` (ou `XxxJobScheduler` si multiples jobs)
- Méthodes : `tick()`, `runOnce()`
- Les schedulers ne contiennent AUCUNE logique métier : ils appellent des commands via `CommandBus` uniquement.

### 11.2 Batch Commands

- Scheduler déclenche des commands spécifiques : `RefreshExternalResultsWindowCommand`, `SettleDrawWindowCommand`, `RebuildLedgerCommand`.
- Règle : `Scheduler → CommandBus.send(Command)` uniquement.

---

## 12) Mapping & transformations entre couches

### 12.1 Mapper naming

- Mapper générique : `XxxMapper`
- Mapper web : `XxxWebMapper`
- Mapper persistence : `XxxPersistenceMapper`
- Un mapper = une responsabilité. Préférer MapStruct.

### 12.2 Directions autorisées

A) Persistence Entity → Application Read Model

- `XxxJpaEntity → XxxRow | XxxDetails | XxxSummary`
- Lieu : `core.<domain>.infra.persistence.mapper`
- Autorisé.

B) Read Model → Web Response

- `XxxRow | XxxDetails → XxxResponse | XxxItemResponse`
- Lieu : `*.infra.web.mapper` ou `features.*.web.mapper`.

C) Web Request → Command

- `XxxRequest → XxxCommand` (controller ou `XxxWebMapper`).

D) Command → Domain (handler)

- Handler charge l'aggregate via ports et exécute la logique.
- Le domaine ne dépend jamais des commands.

E) Web Request → Query / Criteria

- Patterns : `XxxRequest → XxxQuery` ou `query params → XxxSearchCriteria → XxxQuery`.

### 12.3 Forbidden mappings

- `JpaEntity → WebResponse`
- `WebRequest → JpaEntity`
- `WebRequest → Domain Aggregate`
- `Command → JpaEntity`
- `Controller → Repository` (direct)

### 12.4 Return types par couche

- Controller : `ApiResponse<T>` ou `ApiResponse<TchPage<T>>`
- Query handler : `XxxRow | XxxDetails | XxxSummary` ou `TchPage<XxxRow>`
- Command handler : `Void` ou `XxxResult`
- Persistence adapter : Domain aggregate ou read projection (si read adapter dédié)
- Domain : mutations internes uniquement, aucun DTO/response

### 12.5 Model naming clarification

- Web model : `XxxRequest`, `XxxResponse`
- Application read model : `XxxRow`, `XxxDetails`, `XxxSummary`
- Catalog model : `XxxView`, `XxxSummaryView`
- Interdit : `XxxDto` générique, `XxxModel` ambigu.

---

## 13) Anti-drift rule (MUST)

Si une classe ne correspond à aucun pattern défini ici :

- elle est mal nommée ou mal placée → renommer ou déplacer.

➡️ La classe doit être renommée ou déplacée.
