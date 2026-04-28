## Context

`JdbcSubscriptionStatsReader` contient 3 requêtes JDBC natives qui référencent `FROM subscription` alors que la table réelle est `tenant_subscription` (mappée par `SubscriptionJpaEntity`). Erreur runtime garantie à chaque appel aux stats.

`ExternalBillingProviderAdapter` est code mort : `@Deprecated`, vide, importe un package `catalog.billing` inexistant.

## Goals / Non-Goals

**Goals:**

- Corriger les 3 requêtes SQL (`subscription` → `tenant_subscription`)
- Supprimer `ExternalBillingProviderAdapter`
- Test d'intégration Testcontainers validant le reader sans erreur

**Non-Goals:**

- Refonte du reader en Spring Data/JPQL
- Implémentation d'un vrai billing provider
- Correction de `createdBy = "system"` (sujet séparé)

## Decisions

### D1 — Constante TABLE_NAME

Introduire `private static final String TABLE = "tenant_subscription"` dans le reader plutôt que de corriger chaque string individuellement. Réduit le risque de dérive future.

### D2 — Suppression ExternalBillingProviderAdapter

Classe vide, aucun consommateur identifié, package importé inexistant. Suppression directe sans remplacement.

## Risks / Trade-offs

- **[Risque] Quasi-nul** : correction de 3 strings hardcodées. Aucun impact fonctionnel.
- **[Trade-off]** Le test d'intégration requiert Testcontainers (configuration existante dans le projet, coût acceptable).

## Migration Plan

1. Corriger les 3 requêtes dans `JdbcSubscriptionStatsReader` (utiliser la constante `TABLE`)
2. Supprimer `ExternalBillingProviderAdapter`
3. Vérifier la compilation
4. Ajouter `JdbcSubscriptionStatsReaderIT` (Testcontainers)
5. `./mvnw clean verify`
