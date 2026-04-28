## Why

Les migrations Flyway et les entités JPA ont divergé silencieusement : tables manquantes,
tables `_AUD` absentes ou désalignées, colonnes nommées différemment, policies RLS partielles.
`hibernate.hbm2ddl.auto=validate` échoue en recréation from scratch, et Hibernate Envers ne peut
pas persister ses révisions sur plusieurs entités `@Audited`. La dette est critique pour
garantir l'auditabilité et la reproductibilité de l'environnement.

## What Changes

- **Analyse audit** : évaluation coût/valeur de chaque entité `@Audited` — retrait de `@Audited` sur les entités catalogue/config sans exigence réglementaire.
- **Réécriture / consolidation from scratch** : les migrations historiques divergentes sont remplacées par une baseline lisible et centralisée, structurée par responsabilité technique.
- **Structure cible recommandée** :
  - `V001__extensions_and_rls_helpers.sql` : `CREATE EXTENSION`, helpers `current_tenant()`, `deleted_visibility()`, reset contexte
  - `V100__create_core_tables.sql` : tables métier uniquement, PK/UK/CHECK/FK simples, colonnes d'audit standard
  - `V101__create_audit_tables.sql` : toutes les tables `*_aud` + `revinfo`, au nom exact attendu par Envers
  - `V102__create_technical_tables.sql` : tables techniques séparées du core (`processed_event`, `idempotency_record`, `stats_draw`, `stats_daily`, `stats_event_log`, `shedlock`)
  - `V103__create_indexes.sql` : tous les index métier et RLS, notamment sur `tenant_id`
  - `V104__create_triggers.sql` : `set_updated_at()` et triggers techniques
  - `V105__configure_rls.sql` : `ENABLE ROW LEVEL SECURITY` + `CREATE POLICY`
  - `V106__configure_permissions.sql` : owners / grants si nécessaires
  - `V107__spring_batch_schema.sql` : tables `BATCH_*` isolées des autres migrations
- **Tables core visées** : `tenant`, `address`, `app_user`, `user_preference`, `permission`, `app_role`, `role_permission`, `tenant_user`, `game`, `tenant_game`, `result_slot`, `draw_channel`, `draw_channel_game`, `draw_result`, `draw`, `outlet`, `terminal`, `pos_session`, `pos_session_totals`, `pricing_odds`, `ticket`, `ticket_line`, `ticket_settlement`, `payout`, `billing_plan`, `tenant_subscription`, `theme_preset`, `tenant_theme`, `page_model_template`, `page_model`, `audit_event`, `autonomy_policy_rule`, `limit_definition`, `limit_assignment`, `draw_exposure`, `ledger_entry`, `tchala_entry`, `tchala_entry_number`, `user_notification`
- **Tables techniques visées** : `processed_event`, `idempotency_record`, `stats_draw`, `stats_daily`, `stats_event_log`, `shedlock`
- **Tables auditées visées** : `app_user_aud`, `app_role_aud`, `permission_aud`, `tenant_user_aud`, `user_preference_aud`, `app_setting_aud`, `i18n_override_aud`, `theme_preset_aud`, `outlet_aud`, `terminal_aud`, `pos_session_aud`, `pos_session_totals_aud`, `game_aud`, `result_slot_aud`, `draw_channel_aud`, `draw_channel_game_aud`, `draw_aud`, `draw_result_aud`, `ticket_aud`, `ticket_line_aud`, `payout_aud`, `pricing_odds_aud`, `limit_definition_aud`, `limit_assignment_aud`, `billing_plan_aud`, `tenant_subscription_aud`, `page_model_aud`, `page_model_template_aud`, `tchala_entry_aud`
- **Règle de consolidation des altérations historiques** : toute colonne, contrainte, valeur par défaut, enum simulé par `CHECK`, index, trigger ou policy doit refléter l'état final le plus récent des migrations historiques. Si une table est créée en `V1` puis modifiée en `V2`, la baseline doit embarquer directement l'état de `V2`.
- **Couverture complète des tables techniques** : la baseline doit aussi intégrer les tables non métier mais nécessaires au runtime, notamment `processed_event`, `idempotency_record`, `stats_draw`, `stats_daily`, `stats_event_log`, `shedlock`, dans un fichier séparé du core, ainsi que les tables Spring Batch dans un fichier dédié, et les fonctions techniques comme `increment_draw_exposure(...)`.
- **Correction Java et couche applicative** : `TchRevisionEntity.@Column(name="tenant_id")` ; retrait `@Audited` sur les entités non-retenues ; et pour toute colonne métier conservée ou modifiée, alignement de l'entité JPA, des mappers, des adapters de persistence, et de la couche applicative concernée.

## Capabilities

### New Capabilities

- `flyway-jpa-alignment` : Alignement complet Flyway ↔ JPA — tables manquantes, tables `_AUD`
  conformes Envers, colonnes normalisées, `ddl-auto=validate` vert from scratch.

### Modified Capabilities

_(Aucun changement de requirement fonctionnel — alignment purement technique.)_

## Impact

- Backend : remplacement des migrations divergentes par une baseline from-scratch `V001`, `V100` à `V107`, avec séparation explicite `core / audit / technique / spring-batch`, plus corrections entités Java et couche applicative quand une colonne métier évolue.
- CI : `./mvnw flyway:migrate && ./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` DOIT passer vert from scratch.
- Aucun changement d'API publique.
- Envers persiste les révisions uniquement sur les entités où l'audit a une valeur réglementaire ou financière démontrée.
