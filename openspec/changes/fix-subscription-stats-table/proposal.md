## Why

`JdbcSubscriptionStatsReader` exécute des requêtes SQL hardcodées ciblant la table `subscription`, alors que l'entité `SubscriptionJpaEntity` est mappée sur la table `tenant_subscription`. Cette erreur provoque un crash runtime (`Table "subscription" does not exist`) à chaque appel aux statistiques de souscriptions, identifié lors de l'audit du 2026-04-27.

## What Changes

- Correction des 3 requêtes SQL dans `JdbcSubscriptionStatsReader` : remplacer `subscription` par `tenant_subscription`.
- Suppression de `ExternalBillingProviderAdapter` (classe vide marquée deprecated, code mort référençant `catalog.billing` inexistant).
- Ajout d'un test d'intégration vérifiant que le reader retourne des stats sans erreur.

## Capabilities

### New Capabilities

<!-- aucune nouvelle capability -->

### Modified Capabilities

<!-- aucun changement de contrat -->

## Impact

- `core.subscription/infra/persistence/JdbcSubscriptionStatsReader` — 3 requêtes SQL à corriger.
- `core.subscription/infra/provider/ExternalBillingProviderAdapter` — fichier à supprimer.
- Tests : ajouter un test d'intégration `JdbcSubscriptionStatsReaderIT`.
- **Aucun breaking change sur l'API HTTP.**
