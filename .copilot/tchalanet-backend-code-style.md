# Tchalanet – Backend Code Style & Patterns (for Copilot)

> **IMPORTANT**: Any backend code you generate for this repository MUST follow these rules.

---

## 1. Architectural Principles

- Use **Hexagonal Architecture (Ports & Adapters)**:
  - `domain` = domain model + business rules (no Spring dependencies).
  - `application` = use cases, command/query handlers, orchestration.
  - `application.port.in` = interfaces exposed to adapters (e.g. web controllers, batch schedulers).
  - `application.port.out` = interfaces to infrastructure (repositories, external APIs).
  - `infra.persistence` = DB adapters (Postgres).
  - `infra.external` = external API adapters (US lotteries, RSS, etc.).
  - `web` = HTTP controllers (REST).

- Use **CQRS-style separation**:
  - **Commands**: change state, transactional, return void/ID/summary.
  - **Queries**: read-only, no side effects, return DTOs optimized for views.

- Prefer a **rich domain model** (no anemic domain):
  - Domain entities expose behaviors like `draw.close()`, `ticket.addLine()`, etc.
  - Business rules & validation live inside domain objects, not controllers/adapters.

- Keep **ports small and focused**:
  - Prefer splitting interfaces into readers/writers:
    - `DrawReaderPort` / `DrawWriterPort`
    - `TicketReaderPort` / `TicketWriterPort`
  - A use case that only reads should depend only on reader ports.

---

## 2. Commands, Queries, Command Handlers & Command Bus

### 2.1 Commands vs Queries

- A **command** expresses an intent to change state, e.g.:

  ```java
  public record CreateDrawCommand(
      UUID tenantId,
      String channelCode,
      LocalDate scheduledDate
  ) {}
  ```

- A **query** reads state only, e.g.:

  ```java
  public record ListUpcomingDrawsQuery(
      UUID tenantId,
      String channelCode,
      int limit
  ) {}
  ```

- Use `…CommandHandler` and `…QueryHandler` naming.

### 2.2 Command Handlers

- Command handlers live in `application` layer, e.g.:

  ```java
  public interface CreateDrawCommandHandler {
      UUID handle(CreateDrawCommand command);
  }
  ```

- Implementation:

  ```java
  @Service
  public class CreateDrawUseCase implements CreateDrawCommandHandler {

      private final DrawWriterPort drawWriter;
      private final TenantReaderPort tenantReader;

      public CreateDrawUsecase(
          DrawWriterPort drawWriter,
          TenantReaderPort tenantReader
      ) {
          this.drawWriter = drawWriter;
          this.tenantReader = tenantReader;
      }

      @Override
      public UUID handle(CreateDrawCommand command) {
          // load tenant, validate, create domain Draw, persist via port
          return /* new draw id */;
      }
  }
  ```

### 2.3 Query Handlers

- Same idea for queries:

  ```java
  public interface ListUpcomingDrawsQueryHandler {
      List<UpcomingDrawDto> handle(ListUpcomingDrawsQuery query);
  }
  ```

- These should **never** mutate state.

### 2.4 Command Bus (Optional, but encouraged)

- When you need extra decoupling, controllers should depend on a **CommandBus** or **QueryBus**:

  ```java
  public interface CommandBus {
      <R> R dispatch(Object command);
  }
  ```

- The bus locates the proper handler and can decorate calls (transactions, logging, auth).
- Copilot can generate a simple implementation with a `Map<Class<?>, CommandHandler>`.

---

## 3. Domain Model & Aggregates

- Domain objects should be **immutable when possible** (Java `record` + builders), or at least encapsulate invariants.
- Avoid “anemic” entities: no big services that orchestrate primitive getters/setters.
- Prefer expressive operations:

  ```java
  public final class Draw {
      // fields...

      public Draw close(LocalDateTime now) {
          if (status != DrawStatus.SCHEDULED) {
              throw new DrawAlreadyClosedException(id);
          }
          if (now.isBefore(cutoffAt)) {
              throw new DrawTooEarlyToCloseException(id);
          }
          return new Draw(id, /* same fields, new status CLOSED */);
      }
  }
  ```

- Use **domain exceptions** (checked or runtime) to enforce rules.

---

## 4. Multi-Tenant & Persistence

- All operational tables must support multi-tenancy:
  - `tenant_id` column for tenant-specific data.
  - RLS policies enforced in DB (handled outside of Copilot usually, but queries should always filter by tenant).
- Soft delete via `deleted_at` (timestamp).
- Audit fields on most tables:
  - `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`.

- Seeds and schema evolution via **Flyway**.
- Use **Spring Data JPA** or plain repositories via ports in `application.port.out`.

Example of split ports:

```java
public interface DrawReaderPort {
    Optional<Draw> findById(UUID tenantId, UUID drawId);
    List<Draw> findUpcoming(UUID tenantId, String channelCode, int limit);
}

public interface DrawWriterPort {
    Draw save(Draw draw);
}
```

---

## 5. HTTP & External APIs

- ❌ **Never use `RestTemplate`.**
- ✅ Always use **`WebClient`** with typed DTOs.

- External ports example:

  ```java
  public interface ExternalDrawResultPort {

      record DrawExternalQuery(
          UUID tenantId,
          String channelCode,
          LocalDate drawDate
      ) {}

      record ExternalDrawResult(
          String channelCode,
          LocalDate drawDate,
          List<String> numbers,
          Map<String, Object> raw
      ) {}

      Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query);
  }
  ```

- Adapters for NY & Florida must:
  - Live in `infra.external`.
  - Use `WebClient` injected via configuration.
  - Map JSON to typed DTOs (e.g. `NyResultDto`, `FloridaResultDto`).
  - Avoid generic `JsonNode` parsing when the schema is known.

Example of adapter skeleton:

```java
public final class NyOfficialLotteryAdapter implements ExternalDrawResultPort {

    private static final Logger log = LoggerFactory.getLogger(NyOfficialLotteryAdapter.class);

    private final WebClient client;

    public NyOfficialLotteryAdapter(WebClient client) {
        this.client = client;
    }

    @Override
    public Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query) {
        return client.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("$where", "draw_date='" + query.drawDate() + "' AND game_type='NUMBERS'")
                .build())
            .retrieve()
            .bodyToFlux(NyResultDto.class)
            .filter(dto -> matchesChannel(dto, query.channelCode()))
            .next()
            .map(dto -> toExternalResult(query, dto))
            .onErrorResume(e -> {
                log.warn("NY API error: channel={} date={} message={}",
                    query.channelCode(), query.drawDate(), e.getMessage());
                return Mono.empty();
            })
            .blockOptional();
    }

    private boolean matchesChannel(NyResultDto dto, String channelCode) {
        // ex: "MIDDAY" vs "EVENING"
        return true;
    }

    private ExternalDrawResult toExternalResult(DrawExternalQuery query, NyResultDto dto) {
        List<String> numbers = parseNumbers(dto.winning_numbers());
        return new ExternalDrawResult(
            query.channelCode(),
            query.drawDate(),
            numbers,
            Map.of("raw", dto)
        );
    }

    private List<String> parseNumbers(String winningNumbers) {
        // ex: "12 34 56" -> ["12","34","56"]
        return List.of();
    }
}
```

- Configuration class:

```java
@Configuration
@EnableConfigurationProperties(UsLotteryApiProperties.class)
public class UsLotteryConfig {

    @Bean
    WebClient nyLotteryWebClient(WebClient.Builder builder, UsLotteryApiProperties props) {
        return builder.baseUrl(props.nyBaseUrl()).build();
    }

    @Bean
    ExternalDrawResultPort nyOfficialLotteryAdapter(WebClient nyLotteryWebClient) {
        return new NyOfficialLotteryAdapter(nyLotteryWebClient);
    }
}
```

---

## 6. Controllers (Web)

- Controllers must be **thin**, no business logic.
- They must:
  - Parse HTTP requests into Commands/Queries.
  - Delegate to a command/query handler (or CommandBus/QueryBus).
  - Map domain/DTO results into HTTP responses.

Example:

```java
@RestController
@RequestMapping("/api/draws")
class DrawController {

    private final CreateDrawCommandHandler createDrawHandler;
    private final ListUpcomingDrawsQueryHandler listUpcomingHandler;

    DrawController(
        CreateDrawCommandHandler createDrawHandler,
        ListUpcomingDrawsQueryHandler listUpcomingHandler
    ) {
        this.createDrawHandler = createDrawHandler;
        this.listUpcomingHandler = listUpcomingHandler;
    }

    @PostMapping
    public ResponseEntity<CreateDrawResponseDto> create(@RequestBody CreateDrawRequestDto body) {
        var command = new CreateDrawCommand(body.tenantId(), body.channelCode(), body.scheduledDate());
        var id = createDrawHandler.handle(command);
        return ResponseEntity.ok(new CreateDrawResponseDto(id));
    }

    @GetMapping("/upcoming")
    public List<UpcomingDrawDto> upcoming(@RequestParam UUID tenantId,
                                          @RequestParam String channelCode,
                                          @RequestParam(defaultValue = "10") int limit) {
        var query = new ListUpcomingDrawsQuery(tenantId, channelCode, limit);
        return listUpcomingHandler.handle(query);
    }
}
```

---

## 7. Logging & Error Handling

- Use structured logging via SLF4J:

  ```java
  log.warn("External provider failure: provider={} channel={} date={} message={}",
      "NY_OFFICIAL", query.channelCode(), query.drawDate(), e.getMessage());
  ```

- Avoid generic `e.printStackTrace()` or logs sans contexte.

- Use exception handlers (`@ControllerAdvice`) for mapping domain/application exceptions to HTTP status codes.

---

## 8. Testing

- For domain logic (payout rules, draw transitions), write **unit tests** without Spring.
- For adapters HTTP, use `MockWebServer` or WebClient’s `ExchangeFunction` mocks.
- For queries/commands, write tests that:
  - Arrange fake port implementations.
  - Call handler.
  - Assert on results & interactions.

---

## 9. Summary for Copilot

When generating backend code for Tchalanet:

1. Always respect **Hexagonal Architecture + CQRS** (commands/queries).
2. Use **CommandHandlers** and **QueryHandlers** instead of one giant service.
3. Prefer rich domain objects with business methods.
4. Split repository ports into readers/writers.
5. Use `WebClient` + DTOs for external APIs, never `RestTemplate`.
6. Keep controllers thin, mapping HTTP ↔ command/query DTOs.
7. Add structured logging & explicit error handling.
8. Generate code that looks like it belongs in a modern, clean, multi-tenant SaaS backend.
