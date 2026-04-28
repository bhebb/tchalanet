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

### D1 – ~~Migrations additives V55–V59~~ → Réécriture des fichiers existants (RÉVISÉ)

~~On ne modifie PAS les scripts V2–V54 existants.~~

**Nouvelle décision** : on modifie directement les fichiers de migration existants (V1–V54).
Les environnements local et dev seront **recréés from scratch** (`docker compose down -v`), ce qui invalide le problème de checksum Flyway.
Avantages :

- Schéma final = une seule version linéaire propre, sans couches correctrices
- Pas de migration V55–V59 qui dupliquent la définition des tables
- `CREATE TABLE IF NOT EXISTS` + seeds uniquement — aucun `ALTER TABLE`, aucun bloc PL/pgSQL conditionnel

**Règle stricte** : chaque migration ne contient que des `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, `CREATE POLICY`, `ALTER TABLE … ENABLE ROW LEVEL SECURITY` et des `INSERT INTO` (seeds). Aucun `ADD COLUMN`, `DROP TABLE`, `RENAME COLUMN` dans les fichiers révisés.

### D2 – Tables `*_AUD` : créer uniquement là où l'audit a de la valeur (RÉVISÉ)

~~Pour les tables `*_AUD` stales…~~

**Nouvelle décision** : avant de créer ou corriger une table `*_AUD`, analyser le coût/valeur de l'audit Envers pour chaque entité `@Audited`. Critères :

- **Valeur haute** → garder `@Audited` : entités de sécurité (accès, rôles, permissions), financières (transactions, paiements), réglementaires (tickets, sessions)
- **Valeur faible / coût élevé** → retirer `@Audited` : entités de référence read-mostly (catalogues, configurations, presets), entités à fort volume d'écritures sans exigence réglementaire

Pour les entités où `@Audited` est conservé : CREATE TABLE propre dans le fichier de migration concerné.
Pour les entités où `@Audited` est retiré : supprimer l'annotation + supprimer/ne pas créer la table `*_AUD`.

### D3 – revinfo.rev : conserver INTEGER (int4)

`TchRevisionEntity.id` est `Integer` (Java) → Hibernate mappe en `int4`. On aligne la DB
sur `int4` plutôt que de changer l'entité. La migration convertit via SEQUENCE existante.
Conséquence : toutes les FK `_AUD.rev → revinfo(rev)` sont type-safe en `int4`.

### D4 – revinfo.tenantId → tenant_id

`@Column(name = "tenantId")` produit une colonne littéralement nommée `tenantId` en PostgreSQL
(insensible à la casse → `tenantid`). La DB a `tenant_id`. Correction dans l'entité Java ET dans
la migration (renommage de colonne si elle existe, ou création correcte en V57).

### D5 – Tables `*_AUD` manquantes : script complet

Pour les 5 tables `_AUD` manquantes, on fournit le SQL complet (toutes les colonnes de la table
principale + colonnes Envers standards). Format Envers strict :

- `id UUID NOT NULL` (ou clé naturelle)
- `rev INTEGER NOT NULL` (int4)
- `revtype SMALLINT`
- toutes colonnes de la table principale (toutes NULLABLE)
- `CONSTRAINT pk_xxx_aud PRIMARY KEY (id, rev)`
- `CONSTRAINT fk_xxx_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)`

### D6 – Orphans AUD : conserver

`subscription_aud`, `plan_aud`, `role_permission_aud`, `result_slot_aud` existent mais avec
schémas stales. On les renomme ou remplace par les tables correctes.
`result_slot_aud` (format non-Envers, sans FK revinfo) : à remplacer par une table Envers
conforme puisque `ResultSlotJpaEntity` est `@Audited`.

### D7 – AutonomyPolicyRuleJpaEntity, LedgerEntryJpaEntity : pas @Audited

Ces entités extends BaseTenantEntity mais ne portent pas `@Audited`. Pas de table \_AUD requise.
`tenant_game_aud` est orpheline (TenantGameJpaEntity pas @Audited) → documenter comme dette P2.

## Risks / Trade-offs

| Risque                                                            | Mitigation                                                                |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------- |
| `DROP _AUD` + `CREATE` efface l'historique Envers existant en dev | Acceptable dev-only ; doc claire pour prod                                |
| `revinfo.rev` int4 → overflow à ~2 milliards de révisions         | Suffisant pour plusieurs années ; ADR si migration vers bigint nécessaire |
| `theme_preset` n'est pas encore utilisé en prod                   | Migration idempotente (`CREATE TABLE IF NOT EXISTS`)                      |
| Migrations V2/V4/V5/V8 gardent leurs `DROP TABLE`                 | Inoffensif from scratch ; risque = altération env partiel = documenté     |
