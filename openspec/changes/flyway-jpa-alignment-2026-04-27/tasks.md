## 1. Analyse coût/valeur des tables `@Audited` (obligatoire avant toute migration)

> **Contexte** : Envers écrit une ligne dans `*_AUD` à chaque INSERT/UPDATE/DELETE. Pour les entités à fort volume ou de type catalogue (read-mostly), le coût stockage + I/O est significatif sans valeur réglementaire réelle.

- [ ] 1.1 Lister toutes les entités annotées `@Audited` dans `com.tchalanet.server` :

  ```bash
  grep -r "@Audited" tchalanet-server/src/main/java --include="*.java" -l
  ```

- [ ] 1.2 Pour chaque entité trouvée, remplir la grille de décision ci-dessous :

  | Entité                                                | Table                 | Volume écritures | Besoin réglementaire   | Décision  |
  | ----------------------------------------------------- | --------------------- | ---------------- | ---------------------- | --------- |
  | `TchRevisionEntity`                                   | `revinfo`             | —                | infrastructure Envers  | ✅ garder |
  | `DrawChannelJpaEntity`                                | `draw_channel`        | faible           | config opér.           | à évaluer |
  | `DrawResultJpaEntity`                                 | `draw_result`         | moyen            | résultats officiels    | à évaluer |
  | `DrawJpaEntity`                                       | `draw`                | moyen            | traçabilité tirages    | à évaluer |
  | `ResultSlotJpaEntity`                                 | `result_slot`         | faible           | config catalogue       | à évaluer |
  | `BillingPlanJpaEntity`                                | `billing_plan`        | très faible      | contrats facturation   | à évaluer |
  | `TenantSubscriptionJpaEntity`                         | `tenant_subscription` | faible           | historique abonnements | à évaluer |
  | `DrawChannelGameJpaEntity`                            | `draw_channel_game`   | faible           | config catalogue       | à évaluer |
  | `TchalaEntryJpaEntity`                                | `tchala_entry`        | moyen            | contenu éditorial      | à évaluer |
  | `PricingOddsJpaEntity`                                | `pricing_odds`        | faible           | paramètres financiers  | à évaluer |
  | `ThemePresetJpaEntity`                                | `theme_preset`        | très faible      | config UI              | à évaluer |
  | `PageModelTemplateJpaEntity`                          | `page_model_template` | faible           | config UI              | à évaluer |
  | _(compléter avec toutes les entités trouvées en 1.1)_ |                       |                  |                        |           |

  **Règles de décision** :

  - ✅ **Garder `@Audited`** si : entité financière, entité réglementaire (ticket, session, payout), entité de sécurité (rôle, permission, accès) — ou exigence légale explicite
  - ❌ **Retirer `@Audited`** si : catalogue read-mostly (config, preset, template), fort volume sans valeur légale, entité opérationnelle où les logs applicatifs suffisent

- [ ] 1.3 Valider la grille avec le product owner avant d'avancer

---

## 2. Corrections entités Java

- [ ] 2.1 `TchRevisionEntity` : corriger `@Column(name = "tenantId")` → `@Column(name = "tenant_id")`
- [ ] 2.2 Pour chaque entité classée ❌ en tâche 1.2 : retirer l'annotation `@Audited` (et `@AuditTable` le cas échéant)
- [ ] 2.3 Vérifier `./mvnw compile` passe après les suppressions d'annotations

---

## 3. Cartographie et plan de consolidation des migrations existantes

> **Principe** : local et dev sont recréés from scratch → on peut réécrire les fichiers existants. Aucun nouveau fichier de migration. Chaque fichier ne doit contenir que `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, `CREATE POLICY`, `ALTER TABLE … ENABLE/FORCE ROW LEVEL SECURITY`, `CREATE SEQUENCE IF NOT EXISTS`, et `INSERT INTO` (seeds). Aucun `ALTER TABLE ADD/DROP COLUMN`, aucun `DROP TABLE`, aucun bloc PL/pgSQL conditionnel.

- [ ] 3.1 Identifier les fichiers contenant des `DROP TABLE` à supprimer : V2, V4, V5, V8 — supprimer ces statements dans chaque fichier concerné, remplacer par `CREATE TABLE IF NOT EXISTS`

- [ ] 3.2 Identifier les duplications de tables :

  - `draw_exposure` : défini dans V14 **et** V19 avec schémas différents → conserver uniquement la définition dans V14 (la plus ancienne), supprimer le bloc dans V19 ou aligner le schéma sur la définition canonique
  - `address` : défini dans V2 **et** V50 avec variable RLS stale (`app.tenant_id`) → conserver dans V2, supprimer le bloc dans V50, corriger la variable RLS en `current_tenant()` directement dans V2

- [ ] 3.3 Tables entièrement absentes de Flyway → identifier le fichier logique le plus proche thématiquement où les ajouter (ou créer un seul fichier regroupant les ajouts — voir tâche 4)

- [ ] 3.4 Lister les tables `*_AUD` à créer (uniquement pour les entités classées ✅ en tâche 1.2) et les tables `*_AUD` stales à corriger (colonnes désalignées)

---

## 4. Regroupement : un fichier de consolidation unique

- [ ] 4.1 Créer **un seul** fichier `V55__consolidation.sql` contenant **uniquement** :

  - `CREATE TABLE IF NOT EXISTS theme_preset (…)` — schéma complet
  - `CREATE TABLE IF NOT EXISTS user_notification (…)` — schéma complet + RLS
  - `CREATE TABLE IF NOT EXISTS revinfo (…)` avec `rev integer` et `tenant_id` — ou ALTER si revinfo existe déjà dans un fichier antérieur (à vérifier en 3.x) ; sinon dans le fichier qui le crée
  - Pour chaque table `*_AUD` retenue (liste issue de 3.4) : `CREATE TABLE IF NOT EXISTS <table>_aud (…)` au format Envers strict (`id, rev integer, revtype smallint, …colonnes principales nullable…, PK (id,rev), FK rev → revinfo`)
  - `ALTER TABLE <table> ENABLE ROW LEVEL SECURITY` pour les tables tenant manquantes
  - Seeds de données de référence si nécessaires

  > Ce fichier est le **seul ajout** au schéma. Tout le reste est une correction de fichiers existants.

- [ ] 4.2 Corriger `revinfo` dans le fichier qui le crée originellement : s'assurer que `rev` est `integer` (pas bigint), colonne nommée `tenant_id`

---

## 5. Nettoyage des fichiers de migration existants

- [ ] 5.1 **V2** : supprimer les `DROP TABLE IF EXISTS` ; vérifier et corriger le schéma `address` (variable RLS `current_tenant()`) ; ajouter `IF NOT EXISTS` sur tous les `CREATE TABLE`
- [ ] 5.2 **V4** : supprimer les `DROP TABLE IF EXISTS` ; ajouter `IF NOT EXISTS`
- [ ] 5.3 **V5** : supprimer les `DROP TABLE IF EXISTS` ; ajouter `IF NOT EXISTS`
- [ ] 5.4 **V8** : supprimer les `DROP TABLE IF EXISTS` ; ajouter `IF NOT EXISTS`
- [ ] 5.5 **V14** : vérifier le schéma `draw_exposure` — c'est la version canonique ; ajouter `IF NOT EXISTS`
- [ ] 5.6 **V19** : supprimer ou commenter le bloc `CREATE TABLE draw_exposure` (déjà défini en V14)
- [ ] 5.7 **V50** : supprimer le bloc `CREATE TABLE address` (déjà défini en V2, corrigé en 5.1)
- [ ] 5.8 Passer en revue tous les fichiers V1–V54 : remplacer les `CREATE TABLE` sans `IF NOT EXISTS` par `CREATE TABLE IF NOT EXISTS` (sécurité idempotency)

---

## 6. Validation from scratch

- [ ] 6.1 `docker compose -f tchalanet-infra/compose/... down -v` (supprime tous les volumes Postgres)
- [ ] 6.2 `docker compose up -d postgres` + attendre healthcheck
- [ ] 6.3 `cd tchalanet-server && ./mvnw flyway:migrate` — vérifier code retour 0 et qu'aucune migration n'est en état `FAILED`
- [ ] 6.4 `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` — vérifier code retour 0 (zéro `SchemaValidationException`)
- [ ] 6.5 Vérifier dans Postgres que les tables `*_AUD` retenues existent et ont les bonnes colonnes :
  ```sql
  SELECT table_name FROM information_schema.tables
  WHERE table_name LIKE '%_aud' ORDER BY 1;
  ```
- [ ] 6.6 Vérifier les policies RLS :
  ```sql
  SELECT tablename, policyname FROM pg_policies WHERE schemaname='public' ORDER BY 1;
  ```
- [ ] 6.7 Vérifier qu'aucun `DROP TABLE` ne subsiste dans V2/V4/V5/V8 :
  ```bash
  grep -i "DROP TABLE" tchalanet-server/src/main/resources/db/migration/V2__*.sql \
    tchalanet-server/src/main/resources/db/migration/V4__*.sql \
    tchalanet-server/src/main/resources/db/migration/V5__*.sql \
    tchalanet-server/src/main/resources/db/migration/V8__*.sql
  ```
- [ ] 6.8 Reproduire la validation sur **dev** (recréation complète de l'env dev)

---

## 7. ArchUnit — FlywayAuditAlignmentArchTest

- [ ] 7.1 Créer `FlywayAuditAlignmentArchTest.java` dans le package tests d'architecture
- [ ] 7.2 Le test scanne toutes les classes `@Audited` dans `com.tchalanet.server`
- [ ] 7.3 Pour chaque entité `@Audited`, récupérer le `@Table(name=…)` → vérifier qu'un `CREATE TABLE.*<table>_aud` existe dans les fichiers `db/migration`
- [ ] 7.4 Fail avec message clair si table `_AUD` manquante pour une entité `@Audited`

---

## 8. Checklist DoD (Definition of Done)

- [ ] 8.1 ✅ `./mvnw flyway:migrate` passe sur DB vide sans erreur
- [ ] 8.2 ✅ `ddl-auto=validate` ne lève aucune `SchemaValidationException`
- [ ] 8.3 ✅ Aucun `DROP TABLE` dans V2/V4/V5/V8
- [ ] 8.4 ✅ Aucun bloc `ALTER TABLE ADD/DROP COLUMN` ni PL/pgSQL conditionnel dans les migrations
- [ ] 8.5 ✅ `draw_exposure` défini une seule fois (V14) ; `address` défini une seule fois (V2) avec `current_tenant()`
- [ ] 8.6 ✅ `revinfo.rev` est `integer` (int4) ; colonne nommée `tenant_id`
- [ ] 8.7 ✅ `TchRevisionEntity.@Column(name="tenant_id")` corrigé
- [ ] 8.8 ✅ Toutes les entités `@Audited` (après nettoyage tâche 1) ont leur `*_AUD` avec format Envers strict
- [ ] 8.9 ✅ Entités classées ❌ : `@Audited` retiré + pas de table `*_AUD` pour elles
- [ ] 8.10 ✅ `theme_preset` et `user_notification` créés (dans V55 consolidation)
- [ ] 8.11 ✅ RLS active sur toutes les tables `BaseTenantEntity`
- [ ] 8.12 ✅ `FlywayAuditAlignmentArchTest` passe
- [ ] 8.13 ✅ Validation from scratch reproductible sur dev
