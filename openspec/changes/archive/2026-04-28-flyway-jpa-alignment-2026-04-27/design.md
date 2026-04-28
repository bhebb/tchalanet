## Context

La base de données Tchalanet est reproduite from scratch via Flyway (`V1__` → `V54__`).
Hibernate est configuré avec `ddl-auto=validate` : il compare le schéma DB aux entités JPA au
démarrage. Hibernate Envers exige que chaque entité `@Audited` ait sa table `*_AUD` avec les
colonnes exactement alignées.

Actuellement l'état constaté est :

| Problème                                                                 | Sévérité |
| ------------------------------------------------------------------------ | -------- |
| `theme_preset` table absente en Flyway                                   | P0       |
| `user_notification` table absente en Flyway                              | P0       |
| `billing_plan_aud` absente (→ `plan_aud` stale)                          | P0       |
| `tenant_subscription_aud` absente (→ `subscription_aud` stale)           | P0       |
| `draw_channel_game_aud` absente                                          | P0       |
| `tchala_entry_aud` absente                                               | P0       |
| `pricing_odds_aud` absente                                               | P0       |
| `draw_channel_aud` colonnes stales (tenant*game_id, external*\*)         | P0       |
| `draw_result_aud` colonnes stales (numbers_extra/main, channel_code)     | P0       |
| `revinfo.rev` = `bigint`, entité = `Integer` ; `tenantId` vs `tenant_id` | P0       |
| `draw_aud` colonnes manquantes (draw_date, opened_at, closed_at, etc.)   | P1       |
| `ticket_aud` : status générique au lieu sale/result/settlement_status    | P1       |
| `page_model_template_aud` manque colonne `level`                         | P1       |
| `DROP TABLE` dans V2/V4/V5/V8 (dangereux incrémentaux)                   | P1       |
| `draw_exposure` recréé V19 ≠ schéma V14                                  | P2       |
| `address` recréée V50 ≠ schéma V2 (variable RLS stale `app.tenant_id`)   | P2       |

## Goals / Non-Goals

**Goals:**

- Toutes les migrations Flyway passent sans erreur sur DB vide.
- `ddl-auto=validate` ne lève aucune `SchemaValidationException`.
- Toutes les entités `@Audited` ont leur table `*_AUD` alignée avec colonnes Envers standards
  (`rev INTEGER NOT NULL`, `revtype SMALLINT`, FK → `revinfo(rev)`, PK `(id, rev)`).
- `revinfo` aligné avec `TchRevisionEntity` (type `int4`, colonne `tenant_id`).
- `user_notification` et `theme_preset` créés via migration dédiée.
- Policies RLS cohérentes sur toutes les tables `BaseTenantEntity`.

**Non-Goals:**

- Migration des données existantes (recréation from scratch uniquement).
- Modification des APIs publiques.
- Refactoring du domaine métier.
- Activation de Hibernate Envers sur des entités qui ne sont pas encore `@Audited`.

## Decisions

### D1 – Baseline from scratch centralisée par responsabilité

**Nouvelle décision** : comme l'environnement est recréé from scratch, on remplace les migrations
historiques divergentes par une baseline courte, lisible et centralisée par responsabilité
technique. Le problème de checksum Flyway historique n'est pas pertinent si local/dev repartent
sur une base vide.

Structure cible :

- `V001__extensions_and_rls_helpers.sql`
  - extensions PostgreSQL
  - fonctions RLS communes
  - helpers `current_tenant()`, `deleted_visibility()`, reset contexte
- `V100__create_core_tables.sql`
  - toutes les tables métier
  - colonnes, PK, UK, CHECK, FK simples
  - colonnes d'audit standard
  - éviter `ALTER TABLE` sauf cycles incompressibles
- `V101__create_audit_tables.sql`
  - toutes les tables `*_aud`
  - `revinfo`
  - nom exact attendu par Envers
- `V102__create_technical_tables.sql`
  - `processed_event`
  - `idempotency_record`
  - `stats_draw`
  - `stats_daily`
  - `stats_event_log`
  - `shedlock`
  - autres tables techniques persistées non métier
- `V103__create_indexes.sql`
  - tous les index métier
  - tous les index utiles au RLS, surtout sur `(tenant_id, ...)`
- `V104__create_triggers.sql`
  - `set_updated_at()`
  - autres triggers techniques
- `V105__configure_rls.sql`
  - `ALTER TABLE ... ENABLE ROW LEVEL SECURITY`
  - `CREATE POLICY ...`
- `V106__configure_permissions.sql`
  - owner / grants si nécessaires
- `V107__spring_batch_schema.sql`
  - tables `BATCH_*`
  - isolées des tables métier, audit et autres tables techniques

Avantages :

- Schéma final très lisible
- Responsabilité nette par fichier
- Rebuild from scratch simple à auditer
- Les tables audit sont regroupées logiquement dans un fichier dédié, pas dispersées

**Règle stricte** : chaque fichier reste mono-responsabilité. Les créations de tables métier
vont dans `V100`, les tables Envers dans `V101`, les tables techniques dans `V102`, les index dans `V103`,
les triggers dans `V104`, les policies RLS dans `V105`, les permissions dans `V106`, les tables Spring Batch dans `V107`.

Répartition explicite :

- **Core** : tables métier porteuses du modèle fonctionnel
- **Audit** : tables `*_aud` + `revinfo`
- **Technique** : tables runtime non métier (`processed_event`, `idempotency_record`, `stats_*`, `shedlock`)
- **Spring Batch** : tables `BATCH_*` uniquement

**Règle de reconstitution de l'état final** : la baseline n'est pas une copie verbatim des
migrations historiques, c'est un **fold** de leur résultat final. Toute mutation historique
(`ALTER TABLE`, `ALTER INDEX`, changement de `DEFAULT`, ajout/suppression de `CHECK`, renommage,
ajout de colonnes, correction d'un type, modification d'une policy) doit être reflétée dans la
définition finale centralisée.

Exemple normatif :

- si `status` vaut `'DRAFT'` par défaut dans une migration initiale
- puis `'ACTIVE'` dans une migration ultérieure
- alors la baseline doit déclarer directement le `DEFAULT 'ACTIVE'`

Il en va de même pour :

- nullable vs not null
- types SQL
- contraintes `CHECK`
- contraintes d'unicité
- FK
- indexes
- triggers
- policies RLS

**Règle d'alignement code Java / application** : lorsqu'une colonne métier apparaît, disparaît,
change de type, de sémantique, de nullabilité ou de valeur par défaut, l'alignement ne s'arrête
pas à la table SQL. Il faut aussi mettre à jour les éléments impactés côté code :

- entité JPA
- mapper de persistence
- repository / adapter JDBC ou JPA
- read models / projections concernées
- commands / queries / handlers si la colonne fait partie du métier
- couche web ou feature si la colonne remonte au contrat applicatif

Si la colonne est purement technique et non exposée au métier, seule la couche persistence peut
être impactée. Si c'est une colonne métier, la couche application doit être réalignée.

### D2 – Tables `*_AUD` : créer uniquement là où l'audit a de la valeur (RÉVISÉ)

~~Pour les tables `*_AUD` stales…~~

**Nouvelle décision** : avant de créer ou corriger une table `*_AUD`, analyser le coût/valeur de l'audit Envers pour chaque entité `@Audited`. Critères :

- **Valeur haute** → garder `@Audited` : entités de sécurité (accès, rôles, permissions), financières (transactions, paiements), réglementaires (tickets, sessions)
- **Valeur faible / coût élevé** → retirer `@Audited` : entités de référence read-mostly (catalogues, configurations, presets), entités à fort volume d'écritures sans exigence réglementaire

Pour les entités où `@Audited` est conservé : `CREATE TABLE` propre dans `V101__create_audit_tables.sql`.
Pour les entités où `@Audited` est retiré : supprimer l'annotation + supprimer/ne pas créer la table `*_AUD`.

### D3 – revinfo.rev : conserver INTEGER (int4)

`TchRevisionEntity.id` est `Integer` (Java) → Hibernate mappe en `int4`. On aligne la DB
sur `int4` plutôt que de changer l'entité. La migration convertit via SEQUENCE existante.
Conséquence : toutes les FK `_AUD.rev → revinfo(rev)` sont type-safe en `int4`.

### D4 – revinfo.tenantId → tenant_id

`@Column(name = "tenantId")` produit une colonne littéralement nommée `tenantId` en PostgreSQL
(insensible à la casse → `tenantid`). La DB a `tenant_id`. Correction dans l'entité Java ET dans
la migration (renommage de colonne si elle existe, ou création correcte en V57).

### D5 – Tables `*_AUD` manquantes : script centralisé complet

Pour les tables `_AUD` manquantes, on fournit le SQL complet dans `V101__create_audit_tables.sql`
(toutes les colonnes de la table principale + colonnes Envers standards). Format Envers strict :

- `id UUID NOT NULL` (ou clé naturelle)
- `rev INTEGER NOT NULL` (int4)
- `revtype SMALLINT`
- toutes colonnes de la table principale (toutes NULLABLE)
- `CONSTRAINT pk_xxx_aud PRIMARY KEY (id, rev)`
- `CONSTRAINT fk_xxx_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)`

### D6 – Orphans AUD : corriger ou supprimer dans la baseline audit

`subscription_aud`, `plan_aud`, `role_permission_aud`, `result_slot_aud` existent mais avec
schémas stales. On les remplace par les tables correctes dans `V101__create_audit_tables.sql`,
ou on retire l'audit si la valeur métier ne justifie pas la table.
`result_slot_aud` (format non-Envers, sans FK revinfo) : à remplacer par une table Envers
conforme puisque `ResultSlotJpaEntity` est `@Audited`.

### D7 – AutonomyPolicyRuleJpaEntity, LedgerEntryJpaEntity : pas @Audited

Ces entités extends BaseTenantEntity mais ne portent pas `@Audited`. Pas de table \_AUD requise.
`tenant_game_aud` est orpheline (TenantGameJpaEntity pas @Audited) → documenter comme dette P2.

### D8 – Tables techniques à couvrir explicitement

La baseline doit couvrir non seulement les tables métier et audit, mais aussi les tables
techniques réellement utilisées par le serveur et créées aujourd'hui par Flyway.

Liste minimale à prendre en compte :

- `processed_event`
- `idempotency_record`
- `stats_draw`
- `stats_daily`
- `stats_event_log`
- `shedlock`
- tables Spring Batch (`BATCH_*`) dans un fichier dédié
- fonctions SQL techniques comme `increment_draw_exposure(...)`

Ces objets doivent être répartis dans la baseline par responsabilité :

- tables métier dans `V100`
- tables techniques dans `V102`
- fonctions helpers dans `V001`
- fonctions métier/techniques additionnelles dans `V104` si elles alimentent triggers ou logique SQL
- indexes dans `V103`
- permissions dans `V106`
- tables Spring Batch dans `V107`

### D9 – Convention de nommage SQL de la baseline

La baseline applique une convention stable et searchable, alignée avec `tchalanet-server/docs/NAMING.md` :

- tables et colonnes : `snake_case`
- colonnes tenant : toujours `tenant_id`
- soft delete : `deleted_at`
- audit technique : `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `version`
- index : `ix_<table>__<colonnes>`
- contraintes uniques : `uq_<table>__<colonnes>`
- clés étrangères : `fk_<table>__<ref_table>`
- checks : `chk_<table>__<rule>`
- triggers : `trg_<table>__<action>`
- policies RLS : `<table>_rls_all` et `<table>_rls_select`
- fonctions SQL : helpers explicites sous `public.*` (`current_tenant`, `deleted_visibility`, `reset_rls_context`, `set_updated_at`)

Les annotations JPA qui pointaient encore vers `tenantId` ont été réalignées sur `tenant_id`
pour éviter un schéma SQL propre mais non validable par Hibernate.

## Risks / Trade-offs

| Risque                                                            | Mitigation                                                                       |
| ----------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Un fichier `V100` ou `V101` devient trop gros                     | Garder une structure interne stricte et des sections par domaine                 |
| Une altération historique est oubliée lors de la consolidation    | Inventaire systématique de tous les `ALTER`, puis intégration de leur état final |
| `DROP _AUD` + `CREATE` efface l'historique Envers existant en dev | Acceptable dev-only ; doc claire pour prod                                       |
| `revinfo.rev` int4 → overflow à ~2 milliards de révisions         | Suffisant pour plusieurs années ; ADR si migration vers bigint nécessaire        |
| Des policies RLS lisent encore `app.tenant_id`                    | Contrat SQL unifié dans `V001` + vérification dédiée dans `V104`                 |
| Migrations V2/V4/V5/V8 gardent leurs `DROP TABLE`                 | Elles sont remplacées par la baseline centralisée `V100` à `V105`                |
