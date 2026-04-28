## Status: DRAFT

## 1. Correction des requêtes SQL

- [x] 1.1 Ouvrir `JdbcSubscriptionStatsReader` et identifier les 3 requêtes ciblant `subscription`
- [x] 1.2 Introduire `private static final String TABLE = "tenant_subscription";`
- [x] 1.3 Remplacer chaque occurrence de `FROM subscription` par `FROM " + TABLE + "` (ou utiliser la constante dans un template String)
- [x] 1.4 Vérifier qu'aucune autre requête hardcodée dans le fichier ne référence `subscription`

## 2. Suppression du code mort

- [x] 2.1 Vérifier 0 consommateur de `ExternalBillingProviderAdapter` (`grep -r "ExternalBillingProviderAdapter" tchalanet-server/src/`)
- [x] 2.2 Supprimer `ExternalBillingProviderAdapter.java`
- [x] 2.3 Vérifier la compilation (`./mvnw compile -pl tchalanet-server`)

## 3. Test d'intégration

- [x] 3.1 Créer `JdbcSubscriptionStatsReaderIT` (Testcontainers PostgreSQL)
  - Insérer des lignes dans `tenant_subscription`
  - Appeler les 3 méthodes du reader
  - Vérifier que les résultats sont cohérents et qu'aucune exception n'est levée

## 4. Vérification finale

- [x] 4.1 `./mvnw clean verify -pl tchalanet-server` → build vert + tous tests (7 échecs ArchUnit pre-existants, non liés à cette change)
- [x] 4.2 Mettre à jour CHANGELOG (`FIX: JdbcSubscriptionStatsReader table name subscription → tenant_subscription`)
