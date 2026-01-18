# Règles IA — Agents Copilot / ChatGPT

**Version**: 1.0.0 | **Date**: 2026-01-17

---

## Règles obligatoires (MUST)

Avant **toute** génération de code ou modification :

### 1. Lecture obligatoire (ordre)

1. **`AGENTS.md`** (racine du repo) — règles backend Spring Boot / Java
2. **`.specify/constitution/constitution.md`** — workflow SDD et organisation
3. **`tchalanet-docs/docs/00-guidelines/constitution.md`** — constitution centrale (cette doc)
4. **`.specify/index.md`** — organisation `.specify/` (si travail sur spec/plan/tasks)

### 2. Identification module technique

Identifier le module cible :

- **Backend** → lire `.specify/server/index.md` + fichiers techniques (`web_api.md`, `persistence.md`, etc.)
- **Web** → lire `.specify/web/index.md` + fichiers techniques (à créer : `routing.md`, `state.md`, etc.)
- **Mobile** → lire `.specify/mobile/index.md` + fichiers techniques (à créer)
- **Infra** → lire `.specify/infra/index.md` + fichiers techniques (à créer)

### 3. Identification domaine fonctionnel

Identifier le domaine métier :

- **Sales** → lire `tchalanet-docs/docs/02-functional/domains/sales.md`
- **Payout** → lire `02-functional/domains/payout.md`
- **Ledger** → lire `02-functional/domains/ledger.md`
- **Draws** → lire `02-functional/domains/draws.md`
- **PageModel** → lire `02-functional/domains/pagemodel.md`

### 4. Respecter placement modules

**MUST** respecter l'architecture hexagonale + CQRS :

| Module             | Rôle                                                   | Allowed                           |
| ------------------ | ------------------------------------------------------ | --------------------------------- |
| `common/`          | Technique transversal (context, paging, errors, cache) | Aucune dépendance métier          |
| `core/<domain>`    | Domaines critiques (sales, payout, ledger, draws)      | `common`, autres `core` via ports |
| `features/<slice>` | BFF / orchestration / pages                            | `common`, `core` (via handlers)   |
| `catalog/<name>`   | Référentiels / lookup                                  | `common`                          |

**MUST NOT** placer logique métier critique dans `features/` ou `catalog/`.

### 5. Ne pas inventer de patterns

**MUST** suivre les patterns documentés dans :

- `ARCHITECTURE.md` (racine)
- `.specify/server/*.md`, `.specify/web/*.md`, etc.
- `tchalanet-docs/docs/01-architecture/`

**Si pattern non documenté** → proposer max 2 options, demander validation.

### 6. Gestion ambiguïté

Si une règle est ambiguë ou contradictoire :

1. Proposer **max 2 options** avec rationale
2. Appliquer la **solution la plus safe** (conservatrice)
3. Documenter la décision dans la spec feature ou ADR

**MUST NOT** procéder silencieusement avec un choix non validé.

---

## Règles backend (Spring Boot / Java)

### Code style

- **MUST** use Java 25 + Spring Boot 4.x
- **MUST** prefer `var` for local variables (when type obvious)
- **MUST NOT** use wildcard imports (`import x.*;`)
- **MUST** prefer small methods, explicit names

### Dependency Injection

- **MUST** use constructor injection
- **MUST NOT** use field injection (`@Autowired` on field)

### Tests

- **MUST** use AssertJ (`assertThat`)
- **MUST NOT** use Hamcrest or `org.junit.jupiter.api.Assertions.*`
- **SHOULD** use `@Nested` for scenarios
- **SHOULD** use `@DisplayName` for test methods

### API Controllers

- **MUST** return `ApiResponse<T>` (not raw DTO)
- **MUST** use `TchPage<T>` for lists (not Spring `Page`)
- **MUST** use `@TchPaging TchPageRequest` for pagination
- **MUST** use typed ID wrappers (`TicketId`, `TenantId`) in controllers
- **MUST NOT** accept `UUID` in controllers (except infra endpoints)
- **MUST** dispatch via `CommandBus` / `QueryBus`

### Persistence

- **MUST** use `UUID` only in JPA entities, repositories, Flyway
- **MUST** use typed wrappers everywhere else (domain, commands, queries, handlers, DTOs)
- **MUST** extend `BaseTenantEntity` for tenant-scoped tables
- **MUST** extend `BaseEntity` for global/platform tables
- **MUST** use Flyway for migrations (no `ddl-auto=update`)

Détails : voir `.specify/server/*.md`

---

## Règles frontend (Angular / Ionic)

### Web (Angular + Nx)

- **MUST** use standalone components (Angular 20+)
- **MUST** use Material + Tailwind + DaisyUI
- **MUST** use NgRx for state management
- **MUST** use lazy loading for routes
- **MUST** use `ngx-translate` for i18n
- **SHOULD** follow mobile-first design (breakpoints 480/768/1024)

### Mobile (Ionic + Angular)

- **MUST** use Ionic components
- **MUST** use Capacitor for native features
- **SHOULD** implement offline-first strategies (when applicable)
- **MUST** test on iOS/Android devices or simulators

Détails : voir `.specify/web/*.md` et `.specify/mobile/*.md` (à créer)

---

## Règles fonctionnelles (métier)

### Domaines critiques (MUST core/)

- **Sales** (ventes, tickets, limites)
- **Payout** (paiements gagnants, split payments)
- **Ledger** (journal comptable append-only)
- **Draws** (tirages, résultats, slots)
- **Limits** (règles métier, contraintes)
- **AccessControl** (permissions, rôles)

### Features (orchestration BFF)

- **PageModel** (résolution pages dynamiques)
- **Home** (agrégation multi-domaines)
- **Dashboards** (analytics)

**MUST NOT** mettre de logique métier critique dans `features/`.

---

## Workflow génération code

### Étape 1 : Lecture context

1. Lire `AGENTS.md`, constitution, `.specify/index.md`
2. Identifier module technique + domaine fonctionnel
3. Lire docs techniques (`server/web_api.md`, `persistence.md`, etc.)
4. Lire docs métier (`02-functional/domains/<domain>.md`)

### Étape 2 : Génération

- Respecter conventions documentées
- Utiliser patterns existants
- Placer code dans bon module (`common`, `core`, `features`, `catalog`)
- Ne pas inventer de patterns

### Étape 3 : Validation

- Vérifier erreurs de compilation
- Vérifier tests unitaires passent
- Vérifier respect conventions (IDs, enveloppes, paging, etc.)
- Documenter décisions si nécessaire

### Étape 4 : Documentation

- Si nouvelle règle → mettre à jour doc (centrale ou proche du code)
- Si exception → justifier dans spec ou ADR
- Pas de règle implicite

---

## Checklist avant commit (agent IA)

- [ ] Lecture `AGENTS.md`, constitution, `.specify/index.md`
- [ ] Module technique identifié et doc lue
- [ ] Domaine fonctionnel identifié et doc lue
- [ ] Code placé dans bon module (`common`, `core`, `features`, `catalog`)
- [ ] Conventions respectées (IDs, API, tests, persistence)
- [ ] Tests unitaires ajoutés (si applicable)
- [ ] Pas de patterns inventés
- [ ] Documentation mise à jour (si nouvelle règle ou exception)
- [ ] Compilation et tests passent

---

## Exemples interdits

### ❌ Retourner `Page<T>` Spring directement

```java
// INTERDIT
@GetMapping
public Page<TicketDto> list(Pageable pageable) {
  return service.findAll(pageable);
}
```

### ✅ Utiliser `ApiResponse<TchPage<T>>`

```java
// CORRECT
@GetMapping
public ApiResponse<TchPage<TicketDto>> list(
    @TchPaging(allowedSort = {"createdAt"}) TchPageRequest pageReq) {
  var result = queryBus.send(new ListTicketsQuery(pageReq));
  return ApiResponse.success(result);
}
```

### ❌ Accepter `UUID` en controller

```java
// INTERDIT
@GetMapping("/{id}")
public ApiResponse<TicketDto> get(@PathVariable UUID id) { ... }
```

### ✅ Utiliser typed wrapper

```java
// CORRECT
@GetMapping("/{id}")
public ApiResponse<TicketDto> get(@PathVariable TicketId id) { ... }
```

### ❌ Logique métier dans feature

```java
// INTERDIT (dans features/home)
if (ticket.getAmount() > limits.getMaxAmount()) {
  throw new LimitExceededException();
}
```

### ✅ Logique métier dans core

```java
// CORRECT (dans core/sales)
public class IssueTicketHandler {
  @TchTx
  public TicketView handle(IssueTicketCommand cmd) {
    limitService.validateTicket(cmd); // logique métier
    // ...
  }
}
```

---

## Références rapides

| Sujet           | Doc                                                                 |
| --------------- | ------------------------------------------------------------------- |
| Constitution    | `tchalanet-docs/docs/00-guidelines/constitution.md`                 |
| Règles backend  | `AGENTS.md` (racine) + `.specify/server/index.md`                   |
| API REST        | `.specify/server/web_api.md`                                        |
| Persistence     | `.specify/server/persistence.md`                                    |
| IDs typés       | `.specify/server/typed_ids.md`                                      |
| Domaines métier | `tchalanet-docs/docs/02-functional/domains/`                        |
| Architecture    | `ARCHITECTURE.md` (racine) + `tchalanet-docs/docs/01-architecture/` |
| Workflow SDD    | `.specify/index.md`                                                 |

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-01-17
