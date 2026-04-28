# Subscription Stats — Specification

## Requirements

### Requirement: JdbcSubscriptionStatsReader cible la table tenant_subscription

`JdbcSubscriptionStatsReader` SHALL exécuter toutes ses requêtes SQL sur la table `tenant_subscription`. Aucune requête ne DOIT référencer une table `subscription` (inexistante en base).

> La constante `private static final String TABLE = "tenant_subscription"` est la source de vérité du nom de table dans cette classe.

#### Scenario: Appel aux stats sans erreur runtime

- **WHEN** `GetPlatformSubscriptionStatsQueryHandler` est invoqué
- **THEN** aucune `PSQLException` de type "relation does not exist" n'est levée

#### Scenario: Stats de souscriptions retournées correctement

- **WHEN** la table `tenant_subscription` contient des enregistrements
- **THEN** `readPlatformStats()` retourne les agrégats corrects : total, active, pastDue, canceled, byPlan

#### Scenario: Table vide — agrégats à zéro

- **WHEN** la table `tenant_subscription` est vide
- **THEN** `readPlatformStats()` retourne total=0, active=0, pastDue=0, canceled=0, byPlan=[]

### Requirement: ExternalBillingProviderAdapter absente du codebase

Le code mort `ExternalBillingProviderAdapter` (classe `@Deprecated` vide référençant `catalog.billing` inexistant) ne DOIT pas exister dans le codebase.

#### Scenario: Compilation sans ExternalBillingProviderAdapter

- **WHEN** le projet est compilé
- **THEN** aucune référence à `ExternalBillingProviderAdapter` n'existe
