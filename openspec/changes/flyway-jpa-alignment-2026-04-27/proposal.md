## Why

Les migrations Flyway et les entités JPA ont divergé silencieusement : tables manquantes,
tables `_AUD` absentes ou désalignées, colonnes nommées différemment, policies RLS partielles.
`hibernate.hbm2ddl.auto=validate` échoue en recréation from scratch, et Hibernate Envers ne peut
pas persister ses révisions sur plusieurs entités `@Audited`. La dette est critique pour
garantir l'auditabilité et la reproductibilité de l'environnement.

## What Changes

- **Analyse audit** : évaluation coût/valeur de chaque entité `@Audited` — retrait de `@Audited` sur les entités catalogue/config sans exigence réglementaire.
- **Réécriture des migrations existantes** (pas de nouveaux fichiers V55–V59) : V2, V4, V5, V8 nettoyés (`DROP TABLE` supprimés) ; `draw_exposure` dédupliqué (V14 canonique, V19 nettoyé) ; `address` dédupliqué (V2 canonique, V50 nettoyé, RLS `current_tenant()` corrigé) ; tous les `CREATE TABLE` rendus idempotents (`IF NOT EXISTS`).
- **Un seul fichier d'ajout** `V55__consolidation.sql` : `theme_preset`, `user_notification` + toutes les tables `*_AUD` retenues après analyse, au format Envers strict — uniquement `CREATE TABLE IF NOT EXISTS` et seeds. Aucun `ALTER TABLE`, aucun `DROP TABLE`, aucun PL/pgSQL conditionnel.
- **Correction Java** : `TchRevisionEntity.@Column(name="tenant_id")` ; retrait `@Audited` sur les entités non-retenues.

## Capabilities

### New Capabilities

- `flyway-jpa-alignment` : Alignement complet Flyway ↔ JPA — tables manquantes, tables `_AUD`
  conformes Envers, colonnes normalisées, `ddl-auto=validate` vert from scratch.

### Modified Capabilities

_(Aucun changement de requirement fonctionnel — alignment purement technique.)_

## Impact

- Backend : réécriture ciblée des migrations V2/V4/V5/V8/V14/V19/V50 + 1 fichier `V55__consolidation.sql` ; corrections entités Java (`TchRevisionEntity` + retrait `@Audited` sur entités catalogue).
- CI : `./mvnw flyway:migrate && ./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` DOIT passer vert from scratch.
- Aucun changement d'API publique.
- Envers persiste les révisions uniquement sur les entités où l'audit a une valeur réglementaire ou financière démontrée.
