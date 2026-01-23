# TCHALANET — PLAYBOOK (SERVER)

Ce document est la **source opérationnelle** pour contribuer au backend.
Il complète `ARCHITECTURE.md` (structure) et pointe vers `docs/conventions/*` (normes détaillées).

> Source de vérité versions/runtime : `VERSIONS.md` (pas de versions hardcodées ici).

---

## 0) Principes fondamentaux (non négociables)

- Lisibilité > abstraction
- Domain-first : le code raconte l'histoire métier
- Pas de logique métier dans les controllers
- Les décisions structurantes sont documentées (ARCHITECTURE / conventions), jamais devinées
- Si tu dévies d'une convention : documente le pourquoi et rends-le reproductible

---

## 1) Règles incontournables (Do / Don't)

### DO

- Respecter `ARCHITECTURE.md`, `PLAYBOOK.md`, `NAMING.md`, `AGENTS.md`, `VERSIONS.md`
- Utiliser les **Typed IDs** partout (hors persistence)
- Passer par **CommandBus / QueryBus**
- Publier les side-effects **after-commit**
- Garder les controllers thin
- Respecter `TchRequestContext` + RLS (tenant jamais client)
- Utiliser pagination standard
- Utiliser JUnit5 + AssertJ
- Suivre les conventions de nommage (voir `NAMING.md`)

### DON'T

- UUID brut dans domain/application/dtos
- Logique métier dans controllers
- Inventer un pattern d'architecture
- Cross-domain write dans la transaction critique
- Exposer des entités core via SDR
- Wrapper des erreurs dans `ApiResponse`

---

## 2) Typed IDs (WRAPPERS OBLIGATOIRES)

Règle d'or : hors persistence, on n'utilise jamais `UUID`.

- **Domain/Application/DTOs** : wrappers (`TenantId`, `TicketId`, `OutletId`, …)
- **UUID brut** : uniquement JPA entities, repositories/adapters JDBC, Flyway/SQL

Voir : `docs/conventions/typed_ids.md`.

---

## 3) Organisation du code (rappel)

```text
common/      # transversal technique
catalog/     # référentiels / lookup / read-mostly
core/        # domaines critiques (hexagonal/CQRS)
features/    # orchestration UI/BFF
```

Voir : `docs/ARCHITECTURE.md`.

---

## 4) API : préfixe unique `/api/v1` (et future v2)

### 4.1 Préfixe canonique

Tous les endpoints HTTP sont servis sous :

- `${app.base-path}/${app.api-version}` (par défaut `/api/v1`)
- Ex : `spring.mvc.servlet.path=/api/v1`

On ne crée pas de routes "nues" (`/tenant`, `/admin`, …) comme convention.

Voir : `docs/conventions/routing_and_path.md`.

### 4.2 Scopes

- **PUBLIC** : `/api/v1/public/**`
- **TENANT** : `/api/v1/tenant/**`
- **ADMIN** : `/api/v1/admin/**` (tenant-scoped)
- **PLATFORM** : `/api/v1/platform/**`
- **SDR** : `/api/v1/_sdr/**` (internal platform)

---

## 5) Controllers (règles)

- **Autorisés** : `features/*/infra/web`, `core/*/infra/web`, `catalog/*/infra/web`
- **Interdits** : `domain`, `application`

Controller = mapping + validation + injection context + dispatch.

### 5.1 Réponses 2xx

- **Contrat** : `ApiResponse<T>`
- **Legacy** : un controller peut encore retourner un DTO brut ; `ApiResponseBodyAdvice` auto-wrap.
- **Nouveau code** : retourner `ApiResponse.success(...)` explicitement (target convention).

### 5.2 Erreurs 4xx/5xx

- **Contrat** : `ProblemDetail` (RFC7807) en `application/problem+json`
- **Pratique** : lever `ProblemRest.*(...)` / `ProblemRestException`
- **Interdit** : mettre une erreur dans `ApiResponse`

Voir : `docs/conventions/web_api.md`, `docs/conventions/api_response.md`.

### 5.3 Pagination

Toute liste suit la pagination standard (`TchPageRequest`, `@TchPaging`).

Voir : `docs/conventions/pagination.md`.

---

## 6) CQRS & Handlers (Command/Query)

- Command/Query = `record`
- Handler = `@UseCase` + implémentation `CommandHandler` / `QueryHandler`
- Commands qui écrivent : `@TchTx`
- Lecture simple : pas forcément de transaction

Voir : `docs/conventions/command_query_handlers.md`, `docs/conventions/idempotency.md`.

---

## 7) Events & AfterCommit (règle)

Principe : le domaine publie, l'infra réagit, publication **après commit**.

```java
AfterCommit.run(() -> publisher.publish(event));
```

**Cas typiques** :

- Sale → ledger + stats
- Payout → ledger + notification
- DrawResultApplied → settlement tickets

---

## 8) Cache (règles)

- **L1** = Caffeine
- **L2** = Redis
- TTL via `CacheSpecProvider` (pas hardcodé)
- Pas de cache sur opérations critiques d'argent

Voir : `docs/conventions/cache.md`.

---

## 9) Sécurité, Contexte & RLS

**Vérités** :

## Request Context — DO / DON'T

### DO

- Lire le tenant via `@CurrentContext TchRequestContext`
- Utiliser `ctx.effectiveTenantId()` ou `ctx.tenantId()` (wrapper)
- Laisser RLS filtrer les données
- Pour les batch/jobs : appeler `TchContext.set(ctx)` manuellement

### DON'T

- Lire le JWT dans un controller ou un handler
- Résoudre un tenant UUID ailleurs que dans `TchContextFilter`
- Passer un tenantId depuis le client vers la DB
- Utiliser `@RequestScope` pour le contexte
- Manipuler `SecurityContextHolder` hors sécurité

Voir : `docs/conventions/context.md`, `docs/conventions/security_permissions.md`, `docs/conventions/rls.md`.

---

## 10) SDR (Spring Data REST)

**Usage acceptable** :

- CRUD admin "simple" sur référentiels (`catalog`)
- endpoints sous `/api/v1/_sdr/**`
- sécurité : roles admin/superadmin

**Interdit** :

- exposer des entités core critiques via SDR

Voir : `docs/conventions/routing_and_path.md`, `docs/conventions/security_permissions.md`.

---

## 11) Persistence & migrations

- Flyway obligatoire
- `ddl-auto=validate`
- Tables tenant-scoped : `tenant_id` + indexes + policies RLS
- Envers si audit requis

Voir : `docs/conventions/persistence.md`, `docs/conventions/jpa_entities.md`, `docs/conventions/rls.md`, `docs/conventions/audit.md`.

---

## 12) Tests (standard)

- JUnit 5 + AssertJ uniquement
- `@Nested` pour scénarios, `@DisplayName` recommandé
- Tester la logique métier, pas Spring (sauf intégration nécessaire)

Voir : `docs/conventions/testing.md`.

**Exemple** :

```java
@Nested
@DisplayName("When ticket is pending")
class WhenTicketIsPending {
  @Test
  @DisplayName("Should reject sale")
  void shouldRejectSale() {
    assertAll(
      () -> assertThat(result.status()).isEqualTo(REJECTED),
      () -> assertThat(result.reason()).isNotNull()
    );
  }
}
```

---

## Guide opératoire — comment ajouter une feature

### A) Choisir où mettre le code

**A. Argent/ticket/tirage/fraude/conformité ?**
➡️ `core/<domain>`

**B. Orchestration / BFF / dashboards / agrégation multi-domaines ?**
➡️ `features/<slice>`

**C. Référentiel lookup/read-mostly ?**
➡️ `catalog/<name>` (ou `core/<domain>` si critique + fortement métier)

**D. Technique transversal ?**
➡️ `common/`

---

### B) Template controller (Typed IDs + /api/v1)

```java
@RestController
@RequestMapping("/api/v1/tenant/draws")
@RequiredArgsConstructor
public class DrawQueryController {

  private final QueryBus queryBus;

  @GetMapping
  public ApiResponse<TchPage<DrawItemDto>> list(
      @CurrentContext TchRequestContext ctx,
      @TchPaging(
          allowedSort = {"occurredAt", "status"},
          defaultSort = {"occurredAt,desc"}
      ) TchPageRequest page,
      @RequestParam(required = false) String channel
  ) {
    var q = new ListDrawsQuery(ctx.effectiveTenantIdRequired(), channel, page.pageable());
    var out = queryBus.ask(q);
    return ApiResponse.success(out);
  }
}
```

---

### C) Template handler (Typed IDs + after-commit)

```java
public record CancelTicketCommand(TenantId tenantId, TicketId ticketId, String reason) {}

@UseCase
@RequiredArgsConstructor
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public CancelResult handle(CancelTicketCommand c) {
    var res = writer.cancel(c.tenantId(), c.ticketId(), c.reason());
    AfterCommit.run(() -> events.publish(new TicketCancelledEvent(c.tenantId(), c.ticketId())));
    return res;
  }
}
```

---

### D) Definition of Done — PR prête

- [ ] build + tests OK
- [ ] migrations Flyway OK
- [ ] pas de fuite tenant (context/RLS)
- [ ] scope/path correct (`/api/v1/...`)
- [ ] 2xx = `ApiResponse` ; erreurs = `ProblemDetail`
- [ ] pagination standard si liste
- [ ] after-commit si side effects
- [ ] pas de UUID brut hors persistence
- [ ] docs mises à jour si pattern nouveau (ARCHI / conventions)
