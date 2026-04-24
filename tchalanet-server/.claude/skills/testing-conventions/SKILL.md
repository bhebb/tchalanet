---
name: testing-conventions
description: >
  Déclencher pour toute création ou modification de tests.
  Indispensable si la tâche concerne : tests unitaires, tests d'intégration,
  JUnit 5, AssertJ, Testcontainers, @Nested, @DisplayName, in-memory ports,
  mocks, ou tout ce qui valide le comportement du code Tchalanet.
---

# Conventions de tests — Tchalanet

## Stack de test

| Type                | Outils                                                                 |
| ------------------- | ---------------------------------------------------------------------- |
| Tests unitaires     | JUnit 5 + AssertJ + in-memory ports                                    |
| Tests d'intégration | JUnit 5 + AssertJ + Testcontainers (PostgreSQL)                        |
| Framework           | `@Nested` + `@DisplayName` obligatoires                                |
| Assertions          | **AssertJ uniquement** — `org.junit.jupiter.api.Assertions.*` interdit |

## Structure obligatoire — test unitaire

```java
@DisplayName("TicketDomainService")
class TicketDomainServiceTest {

    private TicketDomainService service;
    private InMemoryTicketRepository ticketRepo; // in-memory port, pas de mock

    @BeforeEach
    void setUp() {
        ticketRepo = new InMemoryTicketRepository();
        service = new TicketDomainService(ticketRepo);
    }

    @Nested
    @DisplayName("When placing a ticket")
    class WhenPlacingATicket {

        @Test
        @DisplayName("should set status to PLACED when ticket is PENDING")
        void shouldSetStatusToPlacedWhenPending() {
            // given
            var ticket = TicketFixture.pending();

            // when
            var result = service.place(ticket, DrawCalendarFixture.active());

            // then
            assertThat(result.status()).isEqualTo(TicketStatus.PLACED);
            assertThat(ticketRepo.findById(result.id())).isPresent();
        }

        @Test
        @DisplayName("should throw TicketAlreadyPlacedException when ticket is not PENDING")
        void shouldThrowWhenNotPending() {
            // given
            var ticket = TicketFixture.placed();

            // when / then
            assertThatThrownBy(() -> service.place(ticket, DrawCalendarFixture.active()))
                .isInstanceOf(TicketAlreadyPlacedException.class);
        }
    }
}
```

## Règles AssertJ

```java
// ✅ AssertJ uniquement
assertThat(result).isEqualTo(expected);
assertThat(list).hasSize(3).contains(item);
assertThat(optional).isPresent().contains(value);
assertThatThrownBy(() -> sut.method()).isInstanceOf(MyException.class);
assertThat(result.field()).isNotNull().satisfies(f -> {
    assertThat(f.value()).isEqualTo(42);
});

// ❌ Interdit
assertEquals(expected, actual);           // JUnit assertions
assertTrue(condition);
assertNotNull(value);
Assertions.assertEquals(...);             // import JUnit
```

## In-memory ports — préférés aux mocks

```java
// ✅ Port in-memory pour les tests unitaires
class InMemoryTicketRepository implements TicketReaderPort, TicketWriterPort {
    private final Map<TicketId, Ticket> store = new HashMap<>();

    @Override public Optional<Ticket> findById(TicketId id) { return Optional.ofNullable(store.get(id)); }
    @Override public Ticket save(Ticket t) { store.put(t.id(), t); return t; }
    public int count() { return store.size(); }
}

// Mocks (Mockito) acceptés uniquement si l'in-memory est disproportionné
```

## Tests d'intégration — périmètre strict

Réservés exclusivement à :

- Sécurité / authentification (OAuth2, Keycloak, rôles)
- **RLS / isolation tenant** (vérifier que les données d'un tenant ne fuient pas)
- Argent / settlement (calculs financiers critiques)
- Batch critique (comportements transactionnels)
- Contraintes DB (unicité, FK, check constraints)

```java
// ✅ Testcontainers pour PostgreSQL quand le comportement SQL compte
@SpringBootTest
@Testcontainers
class TicketRlsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.1");

    @Test
    @DisplayName("should not return tickets from another tenant")
    void shouldIsolateTenantData() {
        // given — tickets de 2 tenants différents insérés
        // when — query avec TchContext du tenant A
        // then — seulement les tickets du tenant A visibles
        assertThat(result).allMatch(t -> t.tenantId().equals(tenantA));
    }
}
```

## Vérifications HTTP en intégration

```java
// ✅ Vérifier le wrapping ApiResponse<T> sur 2xx
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody().data()).isNotNull();

// ✅ Vérifier ProblemDetail sur les erreurs
assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
assertThat(errorResponse.getBody().getType()).contains("ticket-not-found");
```

## Fixtures — conventions

```java
// ✅ Classes de fixtures par aggregate (dans src/test/java)
class TicketFixture {
    public static Ticket pending() { return new Ticket(TicketId.of(UUID.randomUUID()), ...); }
    public static Ticket placed()  { return pending().place(...); }
}

class DrawCalendarFixture {
    public static DrawCalendar active() { ... }
    public static DrawCalendar closed() { ... }
}
```

## Checklist avant tout test

- [ ] `@Nested` + `@DisplayName` utilisés
- [ ] AssertJ uniquement (0 import `org.junit.jupiter.api.Assertions`)
- [ ] Tests unitaires : in-memory ports préférés aux mocks
- [ ] Tests d'intégration : uniquement pour les cas listés ci-dessus
- [ ] Fixtures dans des classes dédiées (`XxxFixture`)
- [ ] Format `given / when / then` respecté
- [ ] Nommage : `should<Expected>When<Condition>`
