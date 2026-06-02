# Skill — Java Test Data for Tchalanet

Use this skill for Java backend tests that need realistic but controlled test data.

## Position

Use Instancio as a helper, not as the source of business truth.

Recommended stack:

1. Explicit fixtures/builders for domain-critical objects.
2. Instancio for broad DTO/read-model/entity-shaped data where random completeness helps.
3. Hand-written values for edge cases, state transitions, money, time, IDs, and security context.

## When to use Instancio

Good:

- large request/response DTOs;
- read models with many non-essential fields;
- mapper tests where most fields should be populated;
- fuzz-like unit tests for null/blank/collection variety;
- avoiding boilerplate in non-critical nested objects.

Avoid or override heavily:

- money values;
- statuses and state transitions;
- tenant/user/terminal/session IDs;
- timezone/date cutoffs;
- idempotency keys and request hashes;
- permissions/roles;
- expected payout amounts;
- JPA entities with lifecycle/audit fields unless testing mapping specifically.

## Determinism rule

Random test data must be reproducible.

If using Instancio:

- prefer `@ExtendWith(InstancioExtension.class)` for JUnit integration;
- use a fixed seed for tests where failure must be replayed;
- override all fields that matter to the scenario;
- never assert exact random values unless explicitly set.

## Suggested dependency policy

Add only test-scoped dependencies.

Prefer the stable Instancio line unless the project explicitly accepts RC dependencies.

Example Maven dependency management idea:

```xml
<properties>
  <instancio.version>5.5.1</instancio.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.instancio</groupId>
      <artifactId>instancio-bom</artifactId>
      <version>${instancio.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.instancio</groupId>
    <artifactId>instancio-junit</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Before adding it, update `VERSIONS.md` and the parent POM dependency list.

## Tchalanet fixture pattern

Create small test fixture helpers close to the relevant test source set.

Example names:

```text
SalesTestData
PayoutTestData
TenantTestData
OperationalContextTestData
TicketLineTestData
```

Rules:

- fixtures return domain/application types, not JPA entities, unless persistence tests need entities;
- use typed IDs;
- accept overrides for important fields;
- use deterministic Clock/Instant values;
- avoid massive generic `TestDataFactory` dumping ground.

## Example: Instancio with controlled overrides

```java
@ExtendWith(InstancioExtension.class)
class TicketReceiptMapperTest {

  @Test
  void mapsReceiptView(@Seed(1234) long seed) {
    var view = Instancio.of(TicketReceiptView.class)
        .withSeed(seed)
        .set(field(TicketReceiptView::tenantDisplayName), "Tchalanet Test")
        .set(field(TicketReceiptView::currency), CurrencyCode.HTG)
        .create();

    var out = mapper.toResponse(view);

    assertThat(out.tenantDisplayName()).isEqualTo("Tchalanet Test");
  }
}
```

## Example: fixture for business rule

```java
class SalesTestData {
  static SellTicketCommand validSellCommand() {
    return new SellTicketCommand(
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        TerminalId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        Money.of("25.00", CurrencyCode.HTG)
    );
  }
}
```

For business-rule tests, explicit beats random.
