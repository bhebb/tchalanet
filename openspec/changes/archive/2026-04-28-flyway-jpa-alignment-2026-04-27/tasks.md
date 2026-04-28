## 1. Analyse coût/valeur des tables `@Audited` (obligatoire avant toute migration)

> **Contexte** : Envers écrit une ligne dans `*_AUD` à chaque INSERT/UPDATE/DELETE. Pour les entités à fort volume ou de type catalogue (read-mostly), le coût stockage + I/O est significatif sans valeur réglementaire réelle.

- [x] 1.1 Lister toutes les entités annotées `@Audited` dans `com.tchalanet.server` :

  ```bash
  grep -r "@Audited" tchalanet-server/src/main/java --include="*.java" -l
  ```

- [x] 1.2 Pour chaque entité trouvée, remplir la grille de décision ci-dessous :

  | Entité                               | Table                 | Volume écritures | Besoin réglementaire   | Décision  |
  | ------------------------------------ | --------------------- | ---------------- | ---------------------- | --------- |
  | `TchRevisionEntity`                  | `revinfo`             | —                | infrastructure Envers  | ✅ garder |
  | `DrawChannelEntity`                  | `draw_channel`        | faible           | config opér.           | ✅ garder |
  | `DrawResultJpaEntity`                | `draw_result`         | moyen            | résultats officiels    | ✅ garder |
  | `DrawJpaEntity`                      | `draw`                | moyen            | traçabilité tirages    | ✅ garder |
  | `ResultSlotJpaEntity`                | `result_slot`         | faible           | config catalogue       | ✅ garder |
  | `PlanJpaEntity`                      | `billing_plan`        | très faible      | contrats facturation   | ✅ garder |
  | `SubscriptionJpaEntity`              | `tenant_subscription` | faible           | historique abonnements | ✅ garder |
  | `DrawChannelGameEntity`              | `draw_channel_game`   | faible           | config catalogue       | ✅ garder |
  | `TchalaEntryJpaEntity`               | `tchala_entry`        | moyen            | contenu éditorial      | ✅ garder |
  | `PricingOddsEntity`                  | `pricing_odds`        | faible           | paramètres financiers  | ✅ garder |
  | `ThemePresetJpaEntity`               | `theme_preset`        | très faible      | config UI              | ✅ garder |
  | `PageModelTemplateEntity`            | `page_model_template` | faible           | config UI              | ✅ garder |
  | Autres entités `@Audited` existantes | voir `V101`           | variable         | sécurité/finance/ops   | ✅ garder |

  **Règles de décision** :

  - ✅ **Garder `@Audited`** si : entité financière, entité réglementaire (ticket, session, payout), entité de sécurité (rôle, permission, accès) — ou exigence légale explicite
  - ❌ **Retirer `@Audited`** si : catalogue read-mostly (config, preset, template), fort volume sans valeur légale, entité opérationnelle où les logs applicatifs suffisent

- [x] 1.3 Valider la grille avec le product owner avant d'avancer

---

## 2. Corrections entités Java

- [x] 2.1 `TchRevisionEntity` : corriger `@Column(name = "tenantId")` → `@Column(name = "tenant_id")`
- [x] 2.2 Pour chaque entité classée ❌ en tâche 1.2 : retirer l'annotation `@Audited` (aucune entité classée ❌ dans cette passe)
- [x] 2.3 Vérifier `./mvnw compile` passe après les suppressions d'annotations

---

## 3. Cartographie et plan de baseline from scratch

> **Principe** : local et dev sont recréés from scratch → on remplace les migrations divergentes par une baseline centralisée. Chaque fichier a une responsabilité unique : helpers/extensions, tables core, tables audit, index, triggers, RLS, permissions.

- [x] 3.1 Inventorier les migrations historiques V1–V54 à remplacer par la baseline cible `V001`, `V100`, `V101`, `V102`, `V103`, `V104`, `V105`, `V106`, `V107`
- [x] 3.1 bis Inventorier **tous** les `ALTER TABLE`, `ALTER INDEX`, `ALTER SEQUENCE`, `ALTER POLICY`, changements de `DEFAULT`, contraintes `CHECK`, triggers et permissions dans les migrations existantes
- [x] 3.1 ter Pour chaque objet SQL, produire son **état final consolidé** : la dernière mutation historique gagne et doit être intégrée directement dans la baseline

- [x] 3.2 Identifier les duplications de tables :

  - `draw_exposure` : défini dans V14 **et** V19 avec schémas différents → conserver uniquement la définition dans V14 (la plus ancienne), supprimer le bloc dans V19 ou aligner le schéma sur la définition canonique
  - `address` : défini dans V2 **et** V50 avec variable RLS stale (`app.tenant_id`) → conserver dans V2, supprimer le bloc dans V50, corriger la variable RLS en `current_tenant()` directement dans V2

- [x] 3.3 Tables entièrement absentes de Flyway → les intégrer dans `V100__create_core_tables.sql` si métier, dans `V101__create_audit_tables.sql` si Envers, ou dans `V102__create_technical_tables.sql` si table technique
- [x] 3.3 bis Lister explicitement les tables core à couvrir dans `V100__create_core_tables.sql`
- [x] 3.3 ter Lister explicitement les tables techniques à couvrir dans `V102__create_technical_tables.sql` : `processed_event`, `idempotency_record`, `stats_draw`, `stats_daily`, `stats_event_log`, `shedlock`
- [x] 3.3 quater Isoler les tables Spring Batch dans `V107__spring_batch_schema.sql` et ne pas les mélanger aux autres tables techniques

- [x] 3.4 Lister les tables `*_AUD` à créer (uniquement pour les entités classées ✅ en tâche 1.2) et les tables `*_AUD` stales à corriger, puis les intégrer dans `V101__create_audit_tables.sql`

---

## 4. Découpage cible : baseline centralisée recommandée

- [x] 4.1 Créer ou ajuster les fichiers suivants :

  - `V001__extensions_and_rls_helpers.sql`
  - `V100__create_core_tables.sql`
  - `V101__create_audit_tables.sql`
  - `V102__create_technical_tables.sql`
  - `V103__create_indexes.sql`
  - `V104__create_triggers.sql`
  - `V105__configure_rls.sql`
  - `V106__configure_permissions.sql`
  - `V107__spring_batch_schema.sql`

  > Objectif : une baseline lisible, centralisée et stable pour un rebuild from scratch.

- [x] 4.2 Dans `V001__extensions_and_rls_helpers.sql` : standardiser `current_tenant()`, `deleted_visibility()`, reset contexte
- [x] 4.3 Dans `V100__create_core_tables.sql` : créer toutes les tables métier avec PK/UK/CHECK/FK simples et colonnes d'audit standard
- [x] 4.3 bis Dans `V100__create_core_tables.sql` : ne garder que les tables métier, avec leur état final consolidé
- [x] 4.4 Dans `V101__create_audit_tables.sql` : créer `revinfo` et toutes les tables `*_AUD` retenues au format Envers strict
- [x] 4.5 Dans `V102__create_technical_tables.sql` : créer `processed_event`, `idempotency_record`, `stats_draw`, `stats_daily`, `stats_event_log`, `shedlock`, avec leur état final consolidé
- [x] 4.6 Dans `V103__create_indexes.sql` : créer tous les index métier et RLS, en priorité ceux sur `(tenant_id, ...)`
- [x] 4.7 Dans `V104__create_triggers.sql` : créer `set_updated_at()`, `increment_draw_exposure(...)` si conservée comme fonction technique, et les triggers techniques
- [x] 4.8 Dans `V105__configure_rls.sql` : activer RLS et créer les policies sur toutes les tables `BaseTenantEntity`
- [x] 4.9 Dans `V106__configure_permissions.sql` : définir owner / grants si nécessaire
- [x] 4.10 Dans `V107__spring_batch_schema.sql` : conserver les tables `BATCH_*` dans un fichier séparé des autres migrations
- [x] 4.11 Ajouter au design SQL une convention de naming explicite pour tables, colonnes, index, contraintes, triggers, fonctions et policies
- [x] 4.12 Pour chaque colonne métier modifiée par consolidation, tracer les impacts Java : entité JPA, mapper, adapter persistence, use cases, read models, couche web/feature si exposée

---

## 5. Nettoyage des fichiers de migration existants

- [x] 5.1 Reprendre les définitions utiles des migrations historiques sans conserver les divergences (`address`, `draw_exposure`, tables audit stales, etc.)
- [x] 5.2 Supprimer de la baseline toute référence à `current_setting('app.tenant_id', ...)` au profit de `public.current_tenant()` et `public.deleted_visibility()`
- [x] 5.3 Vérifier que la baseline ne contient aucun `DROP TABLE`, aucun `ALTER TABLE ADD/DROP COLUMN`, aucun bloc PL/pgSQL conditionnel de rattrapage
- [x] 5.4 Vérifier que chaque `ALTER` historique a été absorbé dans une définition finale ou dans le bon fichier cible (`V102`, `V103`, `V104`, `V105`, `V106`, `V107`) sans perte de comportement
- [x] 5.5 Vérifier que chaque colonne métier consolidée est alignée côté code Java et couche applicative quand elle porte un sens métier

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
- [ ] 6.6 bis Vérifier qu'aucune policy ne référence encore `app.tenant_id` :
  ```sql
  SELECT schemaname, tablename, policyname
  FROM pg_policies
  WHERE qual ILIKE '%app.tenant_id%' OR with_check ILIKE '%app.tenant_id%';
  ```
- [ ] 6.6 ter Vérifier qu'aucune divergence historique n'a été perdue :
      comparer un inventaire des colonnes/defaults/checks/indexes/triggers/policies attendu depuis les migrations historiques avec la baseline reconstruite
- [x] 6.7 Vérifier qu'aucun `DROP TABLE` ne subsiste dans la baseline cible
- [ ] 6.8 Reproduire la validation sur **dev** (recréation complète de l'env dev)

---

## 7. ArchUnit — FlywayAuditAlignmentArchTest

- [x] 7.1 Créer `FlywayAuditAlignmentArchTest.java` dans le package tests d'architecture
- [x] 7.2 Le test scanne toutes les classes `@Audited` dans `com.tchalanet.server`
- [x] 7.3 Pour chaque entité `@Audited`, récupérer le `@Table(name=…)` → vérifier qu'un `CREATE TABLE.*<table>_aud` existe dans les fichiers `db/migration`
- [x] 7.4 Fail avec message clair si table `_AUD` manquante pour une entité `@Audited`
- [x] 7.5 Ajouter un contrôle ciblé ou une checklist de revue pour vérifier qu'une colonne métier ajoutée/supprimée dans SQL est aussi reflétée dans l'entité et la couche applicative concernée

---

## 8. Checklist DoD (Definition of Done)

- [ ] 8.1 ✅ `./mvnw flyway:migrate` passe sur DB vide sans erreur
- [ ] 8.2 ✅ `ddl-auto=validate` ne lève aucune `SchemaValidationException`
- [x] 8.3 ✅ Aucun `DROP TABLE` dans la baseline cible
- [x] 8.4 ✅ Aucun bloc `ALTER TABLE ADD/DROP COLUMN` ni PL/pgSQL conditionnel dans les migrations
- [x] 8.5 ✅ `draw_exposure` défini une seule fois (V14) ; `address` défini une seule fois (V2) avec `current_tenant()`
- [x] 8.6 ✅ `revinfo.rev` est `integer` (int4) ; colonne nommée `tenant_id`
- [x] 8.7 ✅ `TchRevisionEntity.@Column(name="tenant_id")` corrigé
- [x] 8.8 ✅ Toutes les entités `@Audited` (après nettoyage tâche 1) ont leur `*_AUD` avec format Envers strict
- [x] 8.9 ✅ Entités classées ❌ : `@Audited` retiré + pas de table `*_AUD` pour elles
- [x] 8.10 ✅ `theme_preset` et `user_notification` créés dans `V100__create_core_tables.sql`
- [x] 8.11 ✅ RLS active sur toutes les tables `BaseTenantEntity`
- [x] 8.12 ✅ `FlywayAuditAlignmentArchTest` passe
- [ ] 8.13 ✅ Validation from scratch reproductible sur dev
- [x] 8.14 ✅ Les tables audit sont regroupées dans `V101__create_audit_tables.sql`, séparées des tables métier et des tables techniques
- [x] 8.15 ✅ Toutes les altérations historiques pertinentes sont absorbées dans l'état final de la baseline
- [x] 8.16 ✅ Les tables techniques (`processed_event`, `idempotency_record`, `stats_*`, `shedlock`) sont explicitement couvertes dans `V102__create_technical_tables.sql`
- [x] 8.17 ✅ Les tables Spring Batch sont isolées dans `V107__spring_batch_schema.sql`
- [x] 8.18 ✅ Toute colonne métier consolidée est alignée côté SQL, entité JPA et couche applicative concernée
