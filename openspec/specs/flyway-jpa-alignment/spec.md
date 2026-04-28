# flyway-jpa-alignment Specification

## Purpose

TBD - created by archiving change flyway-jpa-alignment-2026-04-27. Update Purpose after archive.

## Requirements

### Requirement: Tables manquantes créées en Flyway avant ddl-auto=validate

Les tables `theme_preset` et `user_notification` SHALL être créées by Flyway avant le
démarrage de l'application. `ddl-auto=validate` SHALL réussir sans `SchemaValidationException`
sur ces deux tables.

#### Scenario: Recréation from scratch — theme_preset présente

- **WHEN** `flyway:migrate` est appliqué sur une DB vide
- **THEN** la table `theme_preset` existe avec colonnes `id`, `code`, `vendor`, `config`, `label_key`, `active`, `is_default` et colonnes d'audit BaseEntity

#### Scenario: Recréation from scratch — user_notification présente

- **WHEN** `flyway:migrate` est appliqué sur une DB vide
- **THEN** la table `user_notification` existe avec colonnes `id`, `tenant_id`, `user_id`, `type`, `category`, `display_type`, `channel`, `title`, `body`, `payload_json`, `is_read`, `read_at` et colonnes d'audit BaseTenantEntity

#### Scenario: ddl-auto=validate ne lève pas d'exception sur ces tables

- **WHEN** Spring Boot démarre avec `spring.jpa.hibernate.ddl-auto=validate`
- **THEN** aucune `SchemaValidationException` n'est levée pour `theme_preset` ni `user_notification`

---

### Requirement: Naming SQL et Flyway uniforme

La baseline SHALL appliquer des règles de nommage uniformes alignées avec `docs/NAMING.md`.

- tables en `snake_case`
- colonnes en `snake_case`
- FK nommées avec la colonne `<ref>_id`
- colonne tenant toujours `tenant_id`
- soft delete toujours `deleted_at`
- colonnes audit toujours `created_at`, `updated_at`
- index nommés `ix_<table>__<colonnes>`
- unique constraints nommées `uq_<table>__<colonnes>`
- foreign keys nommées `fk_<table>__<ref_table>`
- check constraints nommées `chk_<table>__<rule>`
- triggers nommés `trg_<table>__<action>`
- policies RLS nommées `<table>_tenant_isolation` ou `<table>_rls_all`
- fonctions helpers stables et explicites (`current_tenant`, `deleted_visibility`, `reset_rls_context`)

#### Scenario: Noms de colonnes et tables cohérents

- **WHEN** la baseline est appliquée
- **THEN** les tables et colonnes suivent `snake_case`
- **AND** aucune nouvelle colonne ne suit un style camelCase comme `tenantId`

#### Scenario: Noms d'index et contraintes cohérents

- **WHEN** la baseline est appliquée
- **THEN** les nouveaux index, contraintes et triggers suivent une convention uniforme et searchable

---

### Requirement: L'état final consolidé reflète la dernière migration historique

La baseline SHALL refléter l'état final obtenu après exécution de toutes les migrations
historiques pertinentes. Toute altération historique (`ALTER TABLE`, changement de `DEFAULT`,
ajout/modification de `CHECK`, correction de type, ajout d'index, trigger, policy ou permission)
doit être absorbée dans la définition finale des fichiers `V001`, `V100`, `V101`, `V102`,
`V103`, `V104`, `V105`, `V106`, `V107`.

#### Scenario: Valeur par défaut modifiée dans une migration ultérieure

- **GIVEN** une colonne définie dans une migration initiale avec une valeur par défaut
- **AND** cette valeur par défaut est modifiée dans une migration ultérieure
- **WHEN** la baseline from scratch est appliquée
- **THEN** la colonne porte directement la valeur par défaut la plus récente

#### Scenario: Colonne ou contrainte modifiée par ALTER

- **GIVEN** une table créée dans une migration initiale
- **AND** une migration ultérieure modifie son type, sa nullabilité, sa contrainte ou son `CHECK`
- **WHEN** la baseline from scratch est appliquée
- **THEN** la définition dans `V100__create_core_tables.sql` reflète directement l'état final consolidé

---

### Requirement: Tables techniques couvertes dans la baseline

La baseline SHALL couvrir toutes les tables techniques réellement nécessaires au runtime,
en plus des tables métier et des tables audit, dans un fichier séparé des tables core.

Liste minimale attendue :

- `processed_event`
- `idempotency_record`
- `stats_draw`
- `stats_daily`
- `stats_event_log`
- `shedlock`
- tables `BATCH_*` de Spring Batch

#### Scenario: Tables techniques présentes après recreate

- **WHEN** `flyway:migrate` est appliqué sur une DB vide
- **THEN** les tables techniques requises existent et sont compatibles avec le runtime serveur
- **AND** elles sont définies dans `V102__create_technical_tables.sql`

#### Scenario: Tables Spring Batch isolées dans leur propre migration

- **WHEN** la baseline est construite
- **THEN** les tables `BATCH_*` sont définies dans un fichier dédié `V107__spring_batch_schema.sql`
- **AND** elles ne sont pas mélangées aux tables métier, audit ou autres tables techniques

---

### Requirement: Les tables d'audit reflètent la dernière version des tables métier

Toute table `*_AUD` SHALL être alignée sur la dernière version consolidée de sa table métier
source. Si une colonne métier a évolué dans l'historique, la table d'audit doit refléter cette
version finale, pas une version intermédiaire obsolète.

#### Scenario: Table métier modifiée dans une migration ultérieure

- **GIVEN** une table métier créée dans une migration initiale
- **AND** une migration ultérieure modifie une colonne métier de cette table
- **WHEN** `V100__create_core_tables.sql` et `V101__create_audit_tables.sql` sont appliquées
- **THEN** la table métier reflète l'état final consolidé
- **AND** la table `*_AUD` correspondante reflète la même version finale pour les colonnes métier historisées

---

### Requirement: Le code Java et la couche applicative suivent les colonnes métier consolidées

Si une colonne consolidée est une colonne métier, l'alignement SHALL inclure non seulement le SQL
mais aussi le code Java et la couche applicative concernée.

#### Scenario: Colonne métier ajoutée ou modifiée

- **GIVEN** une colonne métier ajoutée, supprimée ou modifiée dans la baseline consolidée
- **WHEN** l'implémentation est finalisée
- **THEN** l'entité JPA correspondante est à jour
- **AND** les mappers et adapters de persistence concernés sont à jour
- **AND** les use cases, read models et couches feature/web impactés sont à jour si cette colonne fait partie du métier

---

### Requirement: revinfo aligné sur TchRevisionEntity

La table `revinfo` SHALL avoir :

- `rev` de type `INTEGER` (`int4`, pas `bigint`) pour correspondre à `TchRevisionEntity.id: Integer`
- une colonne nommée `tenant_id` (snake_case) et non `tenantId`
- une colonne `user_id` UUID

#### Scenario: Colonne rev de type int4

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `information_schema.columns` retourne `data_type = 'integer'` pour `revinfo.rev`

#### Scenario: Colonne tenant_id nommée correctement

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `information_schema.columns` retourne une colonne `tenant_id` (et non `tenantid`) dans `revinfo`

#### Scenario: TchRevisionEntity.@Column(name) corrigé

- **WHEN** `TchRevisionEntity` est compilé
- **THEN** `@Column(name = "tenant_id")` est présent (pas `"tenantId"`)

---

### Requirement: Tables \_AUD manquantes créées pour entités @Audited

The migration baseline MUST create missing Envers audit tables for every retained `@Audited` entity.

Les tables `billing_plan_aud`, `tenant_subscription_aud`, `draw_channel_game_aud`,
`tchala_entry_aud`, `pricing_odds_aud`, et `theme_preset_aud` SHALL être créées par Flyway.
Chaque table SHALL respecter le format Envers strict :

- `rev INTEGER NOT NULL` FK → `revinfo(rev)`,
- `revtype SMALLINT`,
- toutes les colonnes de la table principale (nullable),
- `PRIMARY KEY (id, rev)`.
- La création SHALL être centralisée dans `V101__create_audit_tables.sql`.

#### Scenario: billing_plan_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM billing_plan_aud` retourne 0 (sans erreur SQL)
- **AND** `\d billing_plan_aud` liste `rev integer`, `revtype smallint`, `id uuid`, et toutes les colonnes de `billing_plan`
- **AND** la définition est portée par `V101__create_audit_tables.sql`

#### Scenario: tenant_subscription_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM tenant_subscription_aud` retourne 0 (sans erreur SQL)

#### Scenario: draw_channel_game_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM draw_channel_game_aud` retourne 0 (sans erreur SQL)

#### Scenario: tchala_entry_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM tchala_entry_aud` retourne 0 (sans erreur SQL)

#### Scenario: pricing_odds_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM pricing_odds_aud` retourne 0 (sans erreur SQL)

---

### Requirement: Tables \_AUD stales réalignées sur les entités JPA actuelles

The migration baseline MUST replace stale audit-table definitions with columns matching the current JPA entities.

Les tables `draw_channel_aud`, `draw_result_aud`, `draw_aud`, `ticket_aud`, et
`page_model_template_aud` SHALL avoir leurs colonnes alignées avec les colonnes actuelles des
entités JPA correspondantes. Les colonnes obsolètes SHALL être supprimées.
Les colonnes manquantes SHALL être ajoutées.
Le réalignement SHALL être fait dans `V101__create_audit_tables.sql`.

#### Scenario: draw_channel_aud ne contient plus de colonnes stales

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `draw_channel_aud` ne possède PAS les colonnes `tenant_game_id`, `external_channel_code`, `external_game_key`, `external_provider`
- **AND** `draw_channel_aud` possède les colonnes `result_slot_id`, `flags`, `notes`, `code`, `name`, `timezone`, `draw_time`, `cutoff_sec`, `days_of_week`, `active`, `sort_order`

#### Scenario: draw_result_aud ne contient plus de colonnes stales

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `draw_result_aud` ne possède PAS les colonnes `numbers_extra`, `numbers_main`, `channel_code`, `draw_date`
- **AND** `draw_result_aud` possède les colonnes `source_result`, `haiti_result`, `result_slot_id`, `occurred_at`, `flags`, `quality`, `source`, `source_hash`, `fetched_at`, `override_reason`

#### Scenario: draw_aud contient toutes les colonnes de draw

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `draw_aud` possède les colonnes `draw_date`, `cutoff_at`, `opened_at`, `closed_at`, `resulted_at`, `settled_at`, `canceled_at`, `cancel_reason`, `draw_result_id`, `result_source`, `result_override_reason`, `result_overridden_at`

#### Scenario: ticket_aud reflète les statuts actuels de ticket

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `ticket_aud` possède les colonnes `sale_status`, `result_status`, `settlement_status`, `approval_request_id`, `currency`, `ticket_code`
- **AND** `ticket_aud` ne possède PAS une colonne générique `status` à la place

#### Scenario: page_model_template_aud contient la colonne level

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `page_model_template_aud` possède la colonne `level varchar(16)`

---

### Requirement: old subscription_aud et plan_aud remplacés par les tables correctes

The migration baseline MUST not create orphan audit tables for removed entity names.

Les tables `subscription_aud` (référence → entité inexistante) et `plan_aud` (référence → entité
inexistante avec colonnes stales) SHALL être supprimées ou renommées.
Les tables `billing_plan_aud` et `tenant_subscription_aud` SHALL les remplacer.
Le remplacement SHALL être porté par `V101__create_audit_tables.sql`.

#### Scenario: subscription_aud supprimée ou remplacée

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `SELECT COUNT(*) FROM subscription_aud` retourne une erreur (table does not exist) ou la table est vide/archivée
- **AND** `SELECT COUNT(*) FROM tenant_subscription_aud` retourne 0 (table existe)

#### Scenario: plan_aud supprimée ou remplacée

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `SELECT COUNT(*) FROM billing_plan_aud` retourne 0 (table existe)

---

### Requirement: result_slot_aud migré au format Envers

The migration baseline MUST define `result_slot_aud` with the standard Envers shape.

`result_slot_aud` (format V43 non-Envers : `rev bigint`, `operation char(1)`, pas de FK revinfo,
pas de PRIMARY KEY) SHALL être remplacée par une table Envers standard pour correspondre à
`ResultSlotJpaEntity @Audited`.

#### Scenario: result_slot_aud au format Envers

- **WHEN** `V101__create_audit_tables.sql` est appliquée
- **THEN** `result_slot_aud` possède `PRIMARY KEY (id, rev)`, `rev integer NOT NULL`, `revtype smallint`
- **AND** `result_slot_aud.rev` est FK vers `revinfo(rev)`
- **AND** `result_slot_aud` ne possède PAS la colonne `operation char(1)`

---

### Requirement: RLS policies cohérentes sur toutes les tables BaseTenantEntity

Toute table dont l'entité JPA extends `BaseTenantEntity` SHALL avoir `ENABLE ROW LEVEL SECURITY`
et au minimum une policy de type ALL utilisant `current_tenant()`. La variable de session
utilisée SHALL être `app.current_tenant` (via `current_tenant()`) et non l'ancienne variable
`app.tenant_id`.

#### Scenario: address RLS utilise current_tenant() et non app.tenant_id

- **WHEN** `flyway:migrate` est appliqué
- **THEN** la policy `address_tenant_isolation` utilise `current_tenant()` (fonction définie en V1)
- **AND** aucune policy sur `address` ne référence `current_setting('app.tenant_id', ...)`

#### Scenario: tenant_theme a une RLS policy

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `pg_policies` contient au moins une policy pour la table `tenant_theme`

---

### Requirement: ArchUnit détecte l'absence de tables \_AUD à la compilation

Un test ArchUnit SHALL vérifier que pour toute entité annotée `@Audited`, il existe une migration
Flyway contenant `CREATE TABLE … <table>_aud`. Le test SHALL échouer à la compilation si une
table `_AUD` est manquante ou si le nom de la table diffère de `<table_principale>_aud`.

#### Scenario: Entité @Audited sans table \_AUD en Flyway — build échoue

- **WHEN** une nouvelle entité `@Audited` est ajoutée dont la table principale est `foo`
- **AND** aucun script Flyway ne contient `CREATE TABLE … foo_aud`
- **THEN** le test `FlywayAuditAlignmentArchTest` échoue avec un message listant la table manquante

#### Scenario: Toutes les entités @Audited existantes ont leur \_AUD — build passe

- **WHEN** la baseline `V001`, `V100`, `V101`, `V102`, `V103`, `V104`, `V105`, `V106`, `V107` a été appliquée
- **THEN** `FlywayAuditAlignmentArchTest` passe sans erreur

---

### Requirement: Procédure de recréation from scratch documentée et fonctionnelle

La commande de recréation from scratch SHALL être : `docker compose down -v`, `docker compose up -d postgres`, `./mvnw flyway:migrate`, `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test`.
Cette séquence SHALL terminer sans erreur après application de la baseline `V001`, `V100`, `V101`, `V102`, `V103`, `V104`, `V105`, `V106`, `V107`.

#### Scenario: Recréation from scratch complète réussie

- **WHEN** la séquence complète (down -v → up postgres → flyway:migrate → validate test) est exécutée
- **THEN** chaque étape retourne code 0
- **AND** aucune `SchemaValidationException` n'est levée
- **AND** toutes les migrations de V1 à V59 sont présentes dans `flyway_schema_history` avec `success = true`

#### Scenario: Toutes les policies RLS actives after recreate

- **WHEN** la recréation from scratch est terminée
- **THEN** `SELECT tablename FROM pg_policies WHERE schemaname='public'` retourne au moins :
  outlet, terminal, pos_session, ticket, ticket_line, tenant_game, draw_channel, draw,
  autonomy_policy_rule, limit_definition, limit_assignment, tenant_subscription, tenant_theme,
  audit_event, app_user, app_role, tenant_user, page_model, payout, pricing_odds, ledger_entry,
  stats_draw, draw_exposure, address, app_setting, i18n_override, page_model_template
