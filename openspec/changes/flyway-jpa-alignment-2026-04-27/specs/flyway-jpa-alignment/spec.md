# Spec — Flyway JPA Alignment (flyway-jpa-alignment)

## Status

**PROPOSED**

---

## Inventaire

### Tableau entités / tables

| Entité JPA                  | Table SQL            | Base             | @Audited | \_AUD Flyway                                              | RLS                   |
| --------------------------- | -------------------- | ---------------- | -------- | --------------------------------------------------------- | --------------------- |
| TenantJpaEntity             | tenant               | BaseEntity       | ✗        | —                                                         | ✗                     |
| TenantRegistryJpaEntity     | tenant               | BaseEntity       | ✗        | —                                                         | ✗                     |
| AddressJpaEntity            | address              | BaseTenantEntity | ✗        | —                                                         | ✓ (V50, stale policy) |
| AppUserJpaEntity            | app_user             | BaseEntity       | ✓        | app_user_aud ✓                                            | ✓ (V40)               |
| AppRoleEntity               | app_role             | BaseEntity       | ✓        | app_role_aud ✓                                            | ✓ (V40)               |
| AppRolePermissionEntity     | role_permission      | —                | ✗        | role_permission_aud (orpheline, @Audited absent)          | ✗                     |
| PermissionEntity            | permission           | AuditableEntity  | ✓        | permission_aud ✓                                          | ✗                     |
| TenantUserJpaEntity         | tenant_user          | BaseTenantEntity | ✓        | tenant_user_aud ✓                                         | ✓ (V40)               |
| UserPreferenceJpaEntity     | user_preference      | BaseEntity       | ✓        | user_preference_aud ✓                                     | ✗                     |
| SettingEntity               | app_setting          | BaseTenantEntity | ✓        | app_setting_aud ✓ (colonnes)                              | ✓ (V41)               |
| I18nOverrideEntity          | i18n_override        | BaseEntity       | ✓        | i18n_override_aud ✓                                       | ✓ (V41)               |
| ThemePresetJpaEntity        | theme_preset         | BaseEntity       | ✓        | theme_preset_aud ✗ **P0**                                 | ✗                     |
| TenantThemeJpaEntity        | tenant_theme         | BaseTenantEntity | ✗        | —                                                         | ✗ (pas de RLS V40)    |
| OutletEntity                | outlet               | BaseTenantEntity | ✓        | outlet_aud ✓                                              | ✓ (V40)               |
| TerminalJpaEntity           | terminal             | BaseTenantEntity | ✓        | terminal_aud ✓                                            | ✓ (V40)               |
| PosSessionJpaEntity         | pos_session          | BaseTenantEntity | ✓        | pos_session_aud ✓                                         | ✓ (V40)               |
| PosSessionTotalsJpaEntity   | pos_session_totals   | BaseTenantEntity | ✓        | pos_session_totals_aud ✓                                  | ✓ (V40)               |
| GameJpaEntity               | game                 | BaseEntity       | ✓        | game_aud ✓                                                | ✗                     |
| ResultSlotJpaEntity         | result_slot          | BaseEntity       | ✓        | result_slot_aud (V43, format ≠ Envers) **P1**             | ✗                     |
| DrawChannelEntity           | draw_channel         | BaseTenantEntity | ✓        | draw_channel_aud (colonnes stales) **P0**                 | ✓ (V40)               |
| DrawChannelGameEntity       | draw_channel_game    | BaseTenantEntity | ✓        | draw_channel_game_aud ✗ **P0**                            | ✓ (V40)               |
| TenantGameJpaEntity         | tenant_game          | BaseTenantEntity | ✗        | tenant_game_aud (orpheline) P2                            | ✓ (V40)               |
| DrawJpaEntity               | draw                 | BaseTenantEntity | ✓        | draw_aud (colonnes manquantes) **P1**                     | ✓ (V40)               |
| DrawResultJpaEntity         | draw_result          | BaseEntity       | ✓        | draw_result_aud (colonnes stales) **P0**                  | ✗                     |
| TicketEntity                | ticket               | BaseTenantEntity | ✓        | ticket_aud (colonnes manquantes) **P1**                   | ✓ (V40)               |
| TicketLineEntity            | ticket_line          | BaseEntity\*     | ✓        | ticket_line_aud ✓                                         | ✓ (V40)               |
| PayoutJpaEntity             | payout               | BaseTenantEntity | ✓        | payout_aud ✓                                              | ✓ (V40)               |
| PricingOddsEntity           | pricing_odds         | BaseTenantEntity | ✓        | pricing_odds_aud ✗ **P0**                                 | ✓ (V40)               |
| LedgerEntryJpaEntity        | ledger_entry         | BaseTenantEntity | ✗        | —                                                         | ✓ (V40)               |
| LimitDefinitionJpaEntity    | limit_definition     | BaseTenantEntity | ✓        | limit_definition_aud ✓                                    | ✓ (V40)               |
| LimitAssignmentJpaEntity    | limit_assignment     | BaseTenantEntity | ✓        | limit_assignment_aud ✓                                    | ✓ (V40)               |
| DrawExposureJpaEntity       | draw_exposure        | BaseTenantEntity | ✗        | —                                                         | ✓ (V40)               |
| AutonomyPolicyRuleJpaEntity | autonomy_policy_rule | BaseTenantEntity | ✗        | —                                                         | ✓ (V40)               |
| PlanJpaEntity               | billing_plan         | BaseEntity       | ✓        | billing_plan_aud ✗ (plan_aud stale) **P0**                | ✗                     |
| SubscriptionJpaEntity       | tenant_subscription  | BaseEntity       | ✓        | tenant_subscription_aud ✗ (subscription_aud stale) **P0** | ✗                     |
| PageModelJpaEntity          | page_model           | BaseTenantEntity | ✓        | page_model_aud ✓                                          | ✓ (V40)               |
| PageModelTemplateEntity     | page_model_template  | BaseEntity       | ✓        | page_model_template_aud (manque `level`) **P1**           | ✓ (V41)               |
| TchalaEntryJpaEntity        | tchala_entry         | BaseEntity       | ✓        | tchala_entry_aud ✗ **P0**                                 | ✗                     |
| TchalaEntryNumberJpaEntity  | tchala_entry_number  | —                | ✗        | —                                                         | ✗                     |
| AuditEventJpaEntity         | audit_event          | BaseTenantEntity | ✗        | —                                                         | ✓ (V40)               |
| IdempotencyRecordJpaEntity  | idempotency_record   | BaseTenantEntity | ✗        | —                                                         | ✗                     |
| PosSessionJpaEntity         | pos_session          | BaseTenantEntity | ✓        | pos_session_aud ✓                                         | ✓ (V40)               |
| NotificationEntity          | user_notification    | BaseTenantEntity | ✗        | — table manquante **P0**                                  | ✗                     |
| StatsDailyEntity            | stats_daily          | —                | ✗        | —                                                         | ✗                     |
| StatsDrawEntity             | stats_draw           | —                | ✗        | —                                                         | ✓ (V40)               |
| StatsEventLogEntity         | stats_event_log      | —                | ✗        | —                                                         | ✗                     |
| TchRevisionEntity           | revinfo              | —                | —        | —                                                         | ✗                     |

\*TicketLineEntity extends BaseEntity mais la table SQL a `tenant_id` (composite FK pour RLS) — anti-pattern documenté P2.

---

## ADDED Requirements

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

### Requirement: revinfo aligné sur TchRevisionEntity

La table `revinfo` SHALL avoir :

- `rev` de type `INTEGER` (`int4`, pas `bigint`) pour correspondre à `TchRevisionEntity.id: Integer`
- une colonne nommée `tenant_id` (snake_case) et non `tenantId`
- une colonne `user_id` UUID

#### Scenario: Colonne rev de type int4

- **WHEN** la migration V57 est appliquée
- **THEN** `information_schema.columns` retourne `data_type = 'integer'` pour `revinfo.rev`

#### Scenario: Colonne tenant_id nommée correctement

- **WHEN** la migration V57 est appliquée
- **THEN** `information_schema.columns` retourne une colonne `tenant_id` (et non `tenantid`) dans `revinfo`

#### Scenario: TchRevisionEntity.@Column(name) corrigé

- **WHEN** `TchRevisionEntity` est compilé
- **THEN** `@Column(name = "tenant_id")` est présent (pas `"tenantId"`)

---

### Requirement: Tables \_AUD manquantes créées pour entités @Audited

Les tables `billing_plan_aud`, `tenant_subscription_aud`, `draw_channel_game_aud`,
`tchala_entry_aud`, `pricing_odds_aud`, et `theme_preset_aud` SHALL être créées par Flyway.
Chaque table SHALL respecter le format Envers strict :

- `rev INTEGER NOT NULL` FK → `revinfo(rev)`,
- `revtype SMALLINT`,
- toutes les colonnes de la table principale (nullable),
- `PRIMARY KEY (id, rev)`.

#### Scenario: billing_plan_aud présente et alignée

- **WHEN** `flyway:migrate` est appliqué
- **THEN** `SELECT COUNT(*) FROM billing_plan_aud` retourne 0 (sans erreur SQL)
- **AND** `\d billing_plan_aud` liste `rev integer`, `revtype smallint`, `id uuid`, et toutes les colonnes de `billing_plan`

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

Les tables `draw_channel_aud`, `draw_result_aud`, `draw_aud`, `ticket_aud`, et
`page_model_template_aud` SHALL avoir leurs colonnes alignées avec les colonnes actuelles des
entités JPA correspondantes. Les colonnes obsolètes SHALL être supprimées.
Les colonnes manquantes SHALL être ajoutées.

#### Scenario: draw_channel_aud ne contient plus de colonnes stales

- **WHEN** la migration V59 est appliquée
- **THEN** `draw_channel_aud` ne possède PAS les colonnes `tenant_game_id`, `external_channel_code`, `external_game_key`, `external_provider`
- **AND** `draw_channel_aud` possède les colonnes `result_slot_id`, `flags`, `notes`, `code`, `name`, `timezone`, `draw_time`, `cutoff_sec`, `days_of_week`, `active`, `sort_order`

#### Scenario: draw_result_aud ne contient plus de colonnes stales

- **WHEN** la migration V59 est appliquée
- **THEN** `draw_result_aud` ne possède PAS les colonnes `numbers_extra`, `numbers_main`, `channel_code`, `draw_date`
- **AND** `draw_result_aud` possède les colonnes `source_result`, `haiti_result`, `result_slot_id`, `occurred_at`, `flags`, `quality`, `source`, `source_hash`, `fetched_at`, `override_reason`

#### Scenario: draw_aud contient toutes les colonnes de draw

- **WHEN** la migration V59 est appliquée
- **THEN** `draw_aud` possède les colonnes `draw_date`, `cutoff_at`, `opened_at`, `closed_at`, `resulted_at`, `settled_at`, `canceled_at`, `cancel_reason`, `draw_result_id`, `result_source`, `result_override_reason`, `result_overridden_at`

#### Scenario: ticket_aud reflète les statuts actuels de ticket

- **WHEN** la migration V59 est appliquée
- **THEN** `ticket_aud` possède les colonnes `sale_status`, `result_status`, `settlement_status`, `approval_request_id`, `currency`, `ticket_code`
- **AND** `ticket_aud` ne possède PAS une colonne générique `status` à la place

#### Scenario: page_model_template_aud contient la colonne level

- **WHEN** la migration V59 est appliquée
- **THEN** `page_model_template_aud` possède la colonne `level varchar(16)`

---

### Requirement: old subscription_aud et plan_aud remplacés par les tables correctes

Les tables `subscription_aud` (référence → entité inexistante) et `plan_aud` (référence → entité
inexistante avec colonnes stales) SHALL être supprimées ou renommées.
Les tables `billing_plan_aud` et `tenant_subscription_aud` SHALL les remplacer.

#### Scenario: subscription_aud supprimée ou remplacée

- **WHEN** la migration V58 est appliquée
- **THEN** `SELECT COUNT(*) FROM subscription_aud` retourne une erreur (table does not exist) ou la table est vide/archivée
- **AND** `SELECT COUNT(*) FROM tenant_subscription_aud` retourne 0 (table existe)

#### Scenario: plan_aud supprimée ou remplacée

- **WHEN** la migration V58 est appliquée
- **THEN** `SELECT COUNT(*) FROM billing_plan_aud` retourne 0 (table existe)

---

### Requirement: result_slot_aud migré au format Envers

`result_slot_aud` (format V43 non-Envers : `rev bigint`, `operation char(1)`, pas de FK revinfo,
pas de PRIMARY KEY) SHALL être remplacée par une table Envers standard pour correspondre à
`ResultSlotJpaEntity @Audited`.

#### Scenario: result_slot_aud au format Envers

- **WHEN** la migration V59 est appliquée
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

- **WHEN** toutes les migrations correctives (V55–V59) ont été appliquées
- **THEN** `FlywayAuditAlignmentArchTest` passe sans erreur

---

### Requirement: Procédure de recréation from scratch documentée et fonctionnelle

La commande de recréation from scratch SHALL être : `docker compose down -v`, `docker compose up -d postgres`, `./mvnw flyway:migrate`, `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test`.
Cette séquence SHALL terminer sans erreur après application de V55–V59.

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
