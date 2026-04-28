## ADDED Requirements

### Requirement: JdbcSubscriptionStatsReader requête table correcte

`JdbcSubscriptionStatsReader` SHALL exécuter toutes ses requêtes SQL sur la table `tenant_subscription`. Aucune requête ne DOIT référencer une table `subscription` (inexistante).

#### Scenario: Appel aux stats sans erreur runtime

- **WHEN** `GetPlatformSubscriptionStatsQueryHandler` est invoqué
- **THEN** aucune `PSQLException` de type "relation does not exist" n'est levée

#### Scenario: Stats de souscriptions retournées correctement

- **WHEN** la table `tenant_subscription` contient des enregistrements
- **THEN** les méthodes du reader retournent les agrégats correspondants (total, actives, expirées, etc.)

### Requirement: ExternalBillingProviderAdapter supprimée

Le code mort `ExternalBillingProviderAdapter` (classe `@Deprecated` vide référençant `catalog.billing` inexistant) DOIT être supprimé du codebase.

#### Scenario: Compilation sans ExternalBillingProviderAdapter

- **WHEN** le projet est compilé
- **THEN** aucune référence à `ExternalBillingProviderAdapter` n'existe
