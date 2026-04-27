---
name: backend-testing
description: Use when writing or reviewing tests in tchalanet-server — enforces AssertJ-only assertions, @Nested + @DisplayName structure, in-memory ports over mocks, fixtures conventions, Testcontainers scope for integration tests covering RLS, auth, money flows, and API contracts.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Tests backend — Conventions

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/testing.md`

## Stack de test

| Type                | Outils                                                                 |
| ------------------- | ---------------------------------------------------------------------- |
| Tests unitaires     | JUnit 5 + AssertJ + in-memory ports                                    |
| Tests d'intégration | JUnit 5 + AssertJ + Testcontainers (PostgreSQL)                        |
| Framework           | `@Nested` + `@DisplayName` obligatoires                                |
| Assertions          | **AssertJ uniquement** — `org.junit.jupiter.api.Assertions.*` interdit |

---

## Règle de décision

```
Logique validable avec des ports in-memory ?
  → TEST UNITAIRE

Correctness dépend de PostgreSQL (RLS, contraintes, fonctions, tx) ?
  → TEST D'INTÉGRATION

Non critique / non à risque élevé ?
  → PAS de test d'intégration
```

---

## Tests unitaires (défaut)

### Structure canonique

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
            var ticket = TicketFixture.placed();
            assertThatThrownBy(() -> service.place(ticket, DrawCalendarFixture.active()))
                .isInstanceOf(TicketAlreadyPlacedException.class);
        }
    }
}
```

### MUST

- **AssertJ uniquement** — jamais `org.junit.jupiter.api.Assertions.*`
- `@Nested` pour regrouper les scénarios
- `@DisplayName("should <expected> when <condition>")` sur les méthodes
- Méthode en camelCase : `shouldCreateTicketWhenOutletIsActive`
- Ports in-memory (fakes) préférés aux mocks Mockito
- `assertAll(...)` pour grouper des assertions liées

### MUST NOT

- Tester le wiring Spring dans les tests unitaires
- Mocker tout (on teste des mocks, pas la logique)
- Asserter des détails d'implémentation (appels privés, ordre interne)

---

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
assertEquals(expected, actual);  // JUnit assertions
assertTrue(condition);
assertNotNull(value);
```

---

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

---

## Tests d'intégration (critique uniquement)

### Scopes autorisés

| Scope                         | Justification                          |
| ----------------------------- | -------------------------------------- |
| Sécurité / auth / permissions | JWT claims, scope routing, Keycloak    |
| RLS / isolation tenant        | Prévention de fuite cross-tenant       |
| Argent / settlement           | Ledger, payout, idempotency            |
| Batch / pipelines critiques   | Fetch/apply/settlement de résultats    |
| Contraintes DB                | Unique keys, triggers, Envers metadata |
| Contrat API clé               | Response envelope + ProblemDetail      |

### MUST

- **Testcontainers** pour PostgreSQL quand le comportement SQL compte
- Tests stables, déterministes (pas de dépendance au temps ou à l'aléatoire)
- Asserter l'isolation tenant explicitement pour les tables multi-tenant

```java
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

### MUST NOT

- Un test d'intégration pour chaque controller/handler
- Dupliquer la couverture des tests unitaires

---

## Vérifications HTTP en intégration

```java
// ✅ Vérifier le wrapping ApiResponse<T> sur 2xx
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody().data()).isNotNull();

// ✅ Vérifier ProblemDetail sur les erreurs
assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
assertThat(errorResponse.getBody().getType()).contains("ticket-not-found");
```

---

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

---

## Suite minimale recommandée

- **RLS isolation** : tenant A ne peut pas lire les données de tenant B
- **deleted_visibility** : comportements active / deleted / all
- **AfterCommit** : side effects uniquement après commit
- **Idempotency** : clé dupliquée ne produit pas de double effet financier
- **ApiResponse vs ProblemDetail** : 2xx wrappé, erreurs non wrappées

---

## Nommage des classes de test

| Type               | Suffixe   |
| ------------------ | --------- |
| Test unitaire      | `XxxTest` |
| Test d'intégration | `XxxIT`   |

---

## Checklist avant tout test

- [ ] `@Nested` + `@DisplayName` utilisés
- [ ] AssertJ uniquement (0 import `org.junit.jupiter.api.Assertions`)
- [ ] Tests unitaires : in-memory ports préférés aux mocks
- [ ] Tests d'intégration : uniquement pour les cas listés ci-dessus
- [ ] Fixtures dans des classes dédiées (`XxxFixture`)
- [ ] Format `given / when / then` respecté
- [ ] Nommage : `should<Expected>When<Condition>`
