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
common/      # Technical Shared Kernel — primitives, bus, context, typed IDs
catalog/     # Simple DDD / Reference Catalog — référentiels read-mostly
platform/    # Application Service Module — audit, identity, tenantconfig, notification…
core/        # Clean Architecture / Hexagonal / CQRS — domaines critiques
features/    # Vertical Slice / BFF Leaf Module — orchestration UI/écrans
```

Chaque couche a un **archetype interne distinct** — ne pas appliquer le pattern hexagonal à catalog ou features.

Voir : `docs/ARCHITECTURE.md`, `docs/modules/` (un fichier par archetype).

---

## 3.1) Appels vers `platform/`

**Règle** : injecter l'interface `XxxApi` du package `platform.<capability>.api`. Ne jamais importer `platform.<capability>.internal`.

```java
// CORRECT — injecter via api/
private final AuditApi auditApi;
private final AccessControlApi acl;

// INTERDIT — dépendance sur internal/
private final AuditServiceImpl auditService; // ❌
```

**Transactions** : les services platform rejoignent la transaction de l'appelant par défaut. `platform.audit` peut utiliser `REQUIRES_NEW`.

**Événements** :
- Platform peut écouter les événements core.
- Core **ne doit pas** écouter les événements platform.
- Collaboration intra-platform (platform → platform) : utiliser des événements ou un ADR.

Voir : `docs/modules/platform.md`, `openspec/context/78-platform-rules.md`.

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

## 4.3 Catalog endpoints

- **Catalog read APIs** : injected via `@Inject XCatalog` (from `catalog/*/api/`)
- **Catalog admin CRUD** : `/api/v1/platform/` (SUPER_ADMIN role)
- **Catalog endpoints** : admin CRUD controllers (thin, via AdminService)

Voir : `openspec/context/75-catalog-rules.md`.

---

## 5) Catalog (Reference Data Layer)

Structure et règles : voir **ARCHITECTURE.md §1.6**.

**Règle d'usage depuis `core/` ou `features/`** : injecter uniquement `XCatalog` depuis `catalog/<name>/api/`. Ne jamais importer `internal.*`, ne jamais écrire dans un catalog depuis core.

```java
// ✅ Inject only the XCatalog interface
private final DrawChannelCatalog catalog;

var channel = catalog.findById(q.channelId()).orElseThrow(...);
```

**Nommage des DTOs catalog** : `*View` (détail), `*SummaryView` (liste), `*Row` (cas spécifique), `*SearchCriteria` (filtres).

Voir : `docs/modules/catalog.md`, `openspec/context/75-catalog-rules.md`.

---

## 6) Controllers — Validation, Audit, Sécurité

### 6.1 Structure canonique du controller

```java
@RestController
@RequestMapping("/api/v1/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets • Tenant")                      // ← Documentation
public class TicketController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  @Operation(summary = "Sell a ticket", description = "Sell a ticket to a customer")
  @PreAuthorize("hasPermission('ticket.sell')")      // ← Permission (méthode)
  @AuditLog(                                          // ← Audit
      entity = "ticket",
      action = "SELL",
      idExpression = "#result.ticketId()",
      detailsExpression = "T(java.util.Map).of('amount', #request.amount())"
  )
  public ApiResponse<TicketResultDto> sellTicket(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody SellTicketRequest request    // ← Validation
  ) {
    var cmd = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        TerminalId.of(request.terminalId()),
        request.amount()
    );

    var result = commandBus.execute(cmd);            // ← Dispatch
    return ApiResponse.success(toDto(result));
  }
}
```

### 6.2 Admin controllers avec hiérarchie @PreAuthorize

**Pour les endpoints ADMIN**, utiliser une hiérarchie :

```java
@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")  // ← Classe : autorités
@Tag(name = "Payouts • Admin")                                    // ← Documentation
@Validated
public class PayoutAdminController {

  private final CommandBus commandBus;
  private final PayoutWebMapper mapper;

  @PostMapping("/{payoutId}/approve")
  @Operation(summary = "Approve a payout", description = "Admin approves a pending payout")
  @PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")         // ← Méthode : permission
  @AuditLog(entity = "payout", action = "APPROVE", idExpression = "#payoutId")
  public ApiResponse<PayoutWorkflowResponse> approve(
      @CurrentContext TchRequestContext ctx,
      @PathVariable PayoutId payoutId
  ) {
    var result = commandBus.execute(
        new ApprovePayoutCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId()
        )
    );
    return ApiResponse.success(mapper.toResponse(result));
  }

  @PostMapping("/{payoutId}/reject")
  @Operation(summary = "Reject a payout", description = "Admin rejects a pending payout")
  @PreAuthorize("hasPermission(null, 'PAYOUT_REJECT')")          // ← Méthode : permission
  @AuditLog(entity = "payout", action = "REJECT", idExpression = "#payoutId")
  public ApiResponse<PayoutWorkflowResponse> reject(
      @CurrentContext TchRequestContext ctx,
      @PathVariable PayoutId payoutId,
      @Valid @RequestBody RejectPayoutRequest body              // ← Validation
  ) {
    var result = commandBus.execute(
        new RejectPayoutCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId(),
            body.reason()
        )
    );
    return ApiResponse.success(mapper.toResponse(result));
  }
}
```

**Structure hiérarchique @PreAuthorize** :

```
Classe  : @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
           ↓
           Méthode 1: @PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")
           Méthode 2: @PreAuthorize("hasPermission(null, 'PAYOUT_REJECT')")
           ...
```

### 6.3 Swagger / OpenAPI Documentation

**Sur la classe** :

```java
@RestController
@RequestMapping("/api/v1/tenant/tickets")
@Tag(name = "Tickets • Tenant", description = "Ticket selling and management")
public class TicketController { }
```

**Sur chaque méthode** :

```java
@GetMapping("/{id}")
@Operation(
    summary = "Get ticket details",
    description = "Retrieve details of a specific ticket",
    tags = {"Tickets • Tenant"}
)
public ApiResponse<TicketDetailDto> getTicket(...) { }
```

**Variables de documentation disponibles** :

- `@Tag(name = "...", description = "...")` — groupe d'endpoints
- `@Operation(summary, description, tags)` — description détaillée
- `@Parameter(description)` — documentation des paramètres
- Généré automatiquement dans `/api-docs`, `/swagger-ui.html`

### 6.3 Jakarta Bean Validation (OBLIGATOIRE)

**Sur les modèles REQUEST** :

```java
public record SellTicketRequest(
    @NotBlank(message = "Terminal ID required")
    @Schema(description = "ID of the terminal", example = "550e8400-e29b-41d4-a716-446655440000")
    String terminalId,

    @NotNull(message = "Amount required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(description = "Sale amount in cents", example = "10000")
    BigDecimal amount,

    @Email(message = "Invalid email format")
    @Schema(description = "Customer email", example = "customer@example.com")
    String notificationEmail
) {}

public record RejectPayoutRequest(
    @NotBlank(message = "Reason required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    @Schema(description = "Rejection reason", example = "Insufficient documentation")
    String reason
) {}
```

**Sur les COMMANDS/QUERIES** :

```java
public record SellTicketCommand(
    TenantId tenantId,
    TerminalId terminalId,
    @NotNull(message = "Amount required")
    @DecimalMin(value = "0.01", inclusive = true)
    BigDecimal amount
) {}
```

**Annotations Jakarta courantes** :

- `@NotNull` — ne pas null
- `@NotBlank` — non null et non vide
- `@NotEmpty` — collection non vide
- `@Min(n)` / `@Max(n)` — min/max entiers
- `@DecimalMin(n)` / `@DecimalMax(n)` — min/max décimaux
- `@Size(min, max)` — taille collection/string
- `@Email` — format email
- `@Pattern(regexp)` — regex
- `@Digits(integer, fraction)` — format numérique
- `@Valid` — validation récursive

**Règles** :

- ✅ Utiliser `@Valid` sur `@RequestBody` dans controller
- ✅ Utiliser Jakarta constraints sur request models ET commands
- ✅ Ajouter `@Schema` pour documentation Swagger
- ✅ Garder les `require*` custom SEULEMENT pour logique métier
- ❌ PAS de `require*` pour ce qui est exprimable en Bean Validation

Voir : `docs/conventions/web_api.md`

### 6.4 Sécurité — @PreAuthorize hiérarchie

**TOUS les endpoints d'écriture (POST, PUT, DELETE, PATCH) DOIVENT avoir l'audit**.

```java
@PostMapping
@AuditLog(
    entity = "ticket",                           // QUOI (entité affectée)
    action = "SELL",                             // QUELLE ACTION
    idExpression = "#result.ticketId()",         // ID de l'entité (SpEL)
    detailsExpression = "T(java.util.Map).of('amount', #request.amount())"  // contexte
)
public ApiResponse<TicketDto> sellTicket(...) { }
```

**Variables SpEL disponibles** :

- `#paramName` → paramètres de méthode
- `#result` → résultat de la méthode (success case)
- `#error` → exception levée (error case)

**Règles** :

- ✅ Utiliser `@AuditLog` sur tous les endpoints d'écriture
- ✅ Success → audit logué **après commit** (via AfterCommit)
- ✅ Error → audit logué **immédiatement** en transaction séparée (REQUIRES_NEW)
- ✅ Failures d'audit ne doivent JAMAIS casser l'opération principale
- ❌ PAS de logique d'audit dans le handler (c'est au web layer)

Voir : `docs/conventions/persistence/audit.md`

### 6.5 Réponses & Erreurs

**Réponses 2xx** :

- **Contrat** : `ApiResponse<T>`
- **Legacy** : retouner DTO brut ; `ApiResponseBodyAdvice` auto-wrap
- **Nouveau code** : retourner `ApiResponse.success(...)` explicitement

**Erreurs 4xx/5xx** :

- **Contrat** : `ProblemDetail` (RFC7807) en `application/problem+json`
- **Pratique** : lever `ProblemRest.*(...)` / `ProblemRestException` depuis handlers
- **Interdit** : mettre erreur dans `ApiResponse`

Voir : `docs/conventions/web_api.md`, `docs/conventions/api_response.md`.

### 6.6 Pagination

Toute liste suit la pagination standard (`TchPageRequest`, `@TchPaging`).

```java
@GetMapping
public ApiResponse<TchPage<TicketItemDto>> listTickets(
    @CurrentContext TchRequestContext ctx,
    @TchPaging(
        allowedSort = {"occurredAt", "status"},
        defaultSort = {"occurredAt,desc"}
    ) TchPageRequest page
) {
  var q = new ListTicketsQuery(ctx.effectiveTenantIdRequired(), page.pageable());
  var results = queryBus.ask(q);
  return ApiResponse.success(results);
}
```

Voir : `docs/conventions/pagination.md`.

---

## 7) CQRS & Handlers (Command/Query)

```java
// ✅ Define as immutable record
public record CancelTicketCommand(
    TenantId tenantId,
    TicketId ticketId,
    String reason
) {}

// ✅ Implement as @UseCase + CommandHandler
@UseCase
@RequiredArgsConstructor
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx  // ← CRITICAL: marks write transaction
  public CancelResult handle(CancelTicketCommand cmd) {
    // 1. Validate invariants (via domain service or port)
    var ticket = writer.findAndValidate(cmd.tenantId(), cmd.ticketId());

    // 2. Mutate (usually via domain aggregate or port)
    ticket.cancel(cmd.reason());
    var saved = writer.save(ticket);

    // 3. Publish side-effects AFTER commit (not during)
    AfterCommit.run(() -> events.publish(
        new TicketCancelledEvent(cmd.tenantId(), cmd.ticketId())
    ));

    return new CancelResult(saved.id(), saved.status());
  }
}
```

**Rules**:

- ✅ Commands = immutable records
- ✅ Handler = `@UseCase` + `CommandHandler<C, R>`
- ✅ Write operations = `@TchTx` annotation
- ✅ Events = published via `AfterCommit.run(...)`
- ✅ Return result DTO, never domain entity
- ❌ NO nested transactions
- ❌ NO state mutations after return
- ❌ NO events during transaction (only after commit)

See: `docs/conventions/command_query_handlers.md`, `docs/conventions/idempotency.md`

### Queries: Read-only questions

```java
// ✅ Define as immutable record
public record GetTicketDetailQuery(
    TenantId tenantId,
    TicketId ticketId
) {}

// ✅ Implement as @UseCase + QueryHandler
@UseCase
@RequiredArgsConstructor
public class GetTicketDetailHandler implements QueryHandler<GetTicketDetailQuery, TicketDetailView> {

  private final TicketReaderPort reader;

  @Override
  // ← NO @TchTx needed — read-only
  public TicketDetailView handle(GetTicketDetailQuery q) {
    // Pure read, no mutations, no side-effects
    return reader.findDetailBy(q.tenantId(), q.ticketId());
  }
}
```

**Rules**:

- ✅ Queries = immutable records
- ✅ Handler = `@UseCase` + `QueryHandler<Q, R>`
- ✅ Read-only (no mutations)
- ✅ Return view/DTO, never domain entity
- ✅ MAY use projections for UI models
- ❌ MUST NOT publish events
- ❌ MUST NOT call CommandBus
- ❌ NO transaction annotation needed

See: `docs/conventions/command_query_handlers.md`

---

## 8) Inter-Domain Calls (Cross-Module Integration)

When domain A needs data from domain B, use stable queries.

```java
// ❌ WRONG — accessing another domain's persistence
@Service
class MyBadService {
  private final TicketRepository repo;  // ← FORBIDDEN from another domain

  void doSomething() {
    repo.findById(...);  // ← VIOLATES hexagonal rules
  }
}

// ✅ CORRECT — requesting via stable query API
@UseCase
@RequiredArgsConstructor
public class MyGoodHandler implements CommandHandler<MyCommand, MyResult> {

  private final QueryBus queryBus;

  @Override
  @TchTx
  public MyResult handle(MyCommand cmd) {
    // Ask Sales domain for ticket data
    var ticketView = queryBus.ask(
        new GetTicketQuery(cmd.tenantId(), cmd.ticketId())
    );

    // Use the result
    if (ticketView.status() == TicketStatus.PENDING) {
      // ...
    }

    return new MyResult(...);
  }
}
```

**Rules for cross-domain reads**:

- ✅ Use `QueryBus.ask(new GetXxxQuery(...))`
- ✅ Query must be exposed by the owning domain
- ✅ Result is a read-model DTO (safe snapshot)
- ❌ NEVER access repositories from another domain
- ❌ NEVER depend on another domain's JPA entities
- ❌ NEVER bypass QueryBus

### Cross-domain WRITES: Use Events + CommandBus

When domain A's action triggers work in domain B, publish an event.

**Example: Ticket sale triggers draw settlement**

```java
// Step 1: Sales domain completes a ticket sale
@UseCase
@RequiredArgsConstructor
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public TicketResult handle(SellTicketCommand cmd) {
    // 1. Sell the ticket (write to sales persistence)
    var ticket = writer.sell(cmd.tenantId(), cmd.drawId(), cmd.amount());

    // 2. Publish event AFTER commit (not during transaction)
    AfterCommit.run(() -> events.publish(
        new TicketSoldEvent(
            cmd.tenantId(),
            ticket.drawId(),
            ticket.id(),
            cmd.amount()
        )
    ));

    return new TicketResult(ticket.id());
  }
}

// Step 2: Draw domain subscribes to the event
@Component
@RequiredArgsConstructor
public class TicketSoldEventListener {
  private final CommandBus commandBus;

  // ← Automatically called AFTER the Sale transaction commits
  @EventListener(TicketSoldEvent.class)
  @Transactional  // ← Separate transaction for Draw operations
  public void onTicketSold(TicketSoldEvent event) {
    try {
      // Execute Draw domain's command with tenant context
      commandBus.execute(new ApplyTicketToDrawCommand(
          event.tenantId(),
          event.drawId(),
          event.ticketId(),
          event.amount()
      ));
    } catch (Exception e) {
      // Idempotent: log and continue, don't rollback
      logger.error("Failed to apply ticket to draw", e);
    }
  }
}
```

**Rules for cross-domain effects**:

- ✅ Source domain publishes event in `AfterCommit.run()`
- ✅ Target domain subscribes via `@EventListener`
- ✅ Listener has its own `@Transactional` (separate transaction)
- ✅ Listener executes a CommandBus command (idempotent)
- ✅ Listener sets tenant context from event
- ✅ Listener tolerates failures gracefully (idempotent listener)
- ❌ NO direct handler-to-handler calls
- ❌ NO nested transactions
- ❌ NO bypassing CommandBus in listeners

### Feature Orchestration across domains

Features orchestrate cross-domain flows (no business logic, pure composition).

```java
// Feature service: orchestrate multi-step flow
@Service
@RequiredArgsConstructor
public class CheckoutService {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public CheckoutResultResponse checkout(CheckoutRequest req) {
    var ctx = TchContext.current();

    // Step 1: Get ticket from Sales
    var ticket = queryBus.ask(
        new GetTicketQuery(ctx.tenantId(), req.ticketId())
    );

    // Step 2: Reserve payout from Draw
    var payout = commandBus.execute(new ReservePayoutCommand(
        ctx.tenantId(),
        ticket.drawId(),
        req.amount()
    ));

    // Step 3: Complete sale in Sales
    commandBus.execute(new CompleteSaleCommand(
        ctx.tenantId(),
        req.ticketId(),
        payout.id()
    ));

    // Step 4: Return aggregated response
    return new CheckoutResultResponse(
        ticket.id(),
        payout.amount(),
        payout.status()
    );
  }
}
```

See: `docs/conventions/inter_domain_calls.md`, `docs/conventions/idempotency.md`

---

## 9) Cache (règles)

- **L2** = Redis
- TTL via `CacheSpecProvider` (pas hardcodé)
- Pas de cache sur opérations critiques d'argent

Voir : `docs/conventions/cache.md`.

---

## 10) Sécurité, Contexte & RLS

**DO** :

- Lire le tenant via `@CurrentContext TchRequestContext`
- Utiliser `ctx.effectiveTenantId()` ou `ctx.tenantId()` (wrapper)
- Laisser RLS filtrer les données
- Pour les batch/jobs : appeler `TchContext.set(ctx)` manuellement

**DON'T** :

- Lire le JWT dans un controller ou un handler
- Résoudre un tenant UUID ailleurs que dans `TchContextFilter`
- Passer un tenantId depuis le client vers la DB
- Utiliser `@RequestScope` pour le contexte
- Manipuler `SecurityContextHolder` hors sécurité

Voir : `docs/conventions/context.md`, `docs/conventions/security_permissions.md`, `docs/conventions/rls.md`.

---

## 11) SDR (Spring Data REST)

- CRUD admin "simple" sur référentiels (`catalog`)
- endpoints sous `/api/v1/_sdr/**`
- sécurité : roles admin/superadmin

**Interdit** :

- exposer des entités core critiques via SDR

Voir : `docs/conventions/routing_and_path.md`, `docs/conventions/security_permissions.md`.

---

## 12) Persistence & migrations

- `ddl-auto=validate`
- Tables tenant-scoped : `tenant_id` + indexes + policies RLS
- Envers si audit requis

Voir : `docs/conventions/persistence.md`, `docs/conventions/jpa_entities.md`, `docs/conventions/rls.md`, `docs/conventions/audit.md`.

---

## 13) Tests (standard)

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

## 14) Guide opératoire — comment ajouter une feature

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
- [ ] `DOMAIN_*.md` / `CATALOG_*.md` / `PLATFORM_*.md` / `FEATURE_*.md` mis à jour si API du domaine change
