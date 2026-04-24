---
name: backend-testing
description: >
  Use when writing or reviewing tests in tchalanet-server — enforces AssertJ-only assertions, @Nested + @DisplayName structure, in-memory ports over mocks, and Testcontainers scope for integration tests covering RLS, auth, money flows, and API contracts.
---

# Tests backend — Conventions

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/testing.md`

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
class TicketServiceTest {

  @Nested
  @DisplayName("When selling a ticket")
  class WhenSellingATicket {

    @Test
    @DisplayName("should create ticket when outlet is active")
    void shouldCreateTicketWhenOutletIsActive() {
      // given
      var command = new SellTicketCommand(ticketId, outletId, ...);

      // when
      var result = handler.handle(command);

      // then
      assertThat(result.ticketId()).isEqualTo(ticketId);
      assertThat(result.status()).isEqualTo(TicketStatus.PENDING);
    }

    @Test
    @DisplayName("should reject when outlet is closed")
    void shouldRejectWhenOutletIsClosed() {
      // ...
      assertThatThrownBy(() -> handler.handle(command))
          .isInstanceOf(OutletClosedException.class);
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
- Vérifier `ApiResponse<T>` wrapping sur 2xx
- Vérifier `ProblemDetail` (jamais wrappé) sur erreurs

### MUST NOT

- Un test d'intégration pour chaque controller/handler
- Dupliquer la couverture des tests unitaires

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
