# OpenSpec — align-core-persistence-after-entity-review

## Status

PROPOSED

## Context

Après le refactoring des entités et la revue détaillée des modèles persistence, plusieurs tables critiques doivent être réalignées avec leurs entités JPA, leurs tables Envers, leurs contraintes, leurs index, leurs foreign keys, leurs contraintes uniques et les vues read-model.

Tables concernées en priorité :

- `ledger_entry`
- `payout`
- `ticket`
- `ticket_line`
- `sales_session`
- `terminal`
- `outlet`

Ces tables sont au cœur des flux argent / ticket / session / terminal / outlet. Un désalignement entre SQL, JPA, Envers, index, FK ou vues peut produire des bugs silencieux à l’exécution, notamment sur les ventes, les paiements, les tickets, les reçus, les sessions de vente et les projections admin.

Le projet est toujours en phase pré-go-live. Donc la stratégie de migration est :

- ne pas créer de nouveau fichier `V*.sql`
- modifier les migrations d’origine
- absorber les `ALTER TABLE` récents dans les `CREATE TABLE`
- mettre à jour les fichiers Envers correspondants
- mettre à jour les index, FK, contraintes uniques et vues dans leurs fichiers d’origine

Référence normative : `docs/conventions/persistence.md`.

## Goal

Aligner complètement les tables critiques avec les entités après refactoring, sans créer de nouveaux fichiers de migration.

L’objectif est d’avoir une base de données recréable proprement depuis zéro avec :

- `flyway clean && flyway migrate`
- démarrage Spring avec `ddl-auto=validate`
- validation Envers
- tests d’architecture persistence
- `./mvnw verify`

## Non-goals

Cette spec ne doit pas :

- introduire une nouvelle stratégie de migration post-go-live
- créer un nouveau fichier `V*.sql`
- changer les invariants métier des domaines
- refactorer les handlers, commands ou queries sauf si nécessaire pour compiler
- introduire des données seed fonctionnelles non liées à la cohérence NOT NULL / FK
- modifier le modèle RLS global hors alignement des policies des tables touchées

## Normative migration rule

Tant que Tchalanet n’est pas en production :

- les changements de colonnes, types, contraintes CHECK vont dans le fichier `CREATE TABLE` original
- les colonnes `*_aud` vont dans le fichier audit / Envers original
- les nouveaux index vont dans le fichier index original
- les triggers vont dans le fichier trigger original
- les policies RLS vont dans le fichier RLS original
- les vues read-model vont dans `V108__create_read_views.sql`
- les seeds impactés vont dans le fichier `V20x` correspondant

Aucun nouveau fichier `V*.sql` ne doit être créé dans cette spec.

## Scope

### SQL / Flyway

Mettre à jour les fichiers existants correspondant à :

- schéma principal des tables core
- tables audit Envers
- index
- foreign keys
- contraintes uniques
- contraintes CHECK
- triggers audit / updated_at si applicable
- policies RLS
- vues read-model
- seeds si une nouvelle contrainte NOT NULL / FK les impacte

### Tables principales

Vérifier et aligner :

#### `ledger_entry`

- colonnes JPA ↔ SQL
- types SQL exacts
- `tenant_id` si tenant-scoped
- audit columns
- version optimistic locking si entity l’utilise
- `deleted_at` uniquement si l’entité est soft-deletable
- FK vers ticket / payout / sales session / tenant selon modèle réel
- index de recherche par tenant, ticket, payout, session, date, type
- contraintes uniques fonctionnelles si nécessaires pour idempotence ledger
- table `_aud` si entity `@Audited`

#### `payout`

- colonnes JPA ↔ SQL
- statut payout / claim / payment selon modèle réel
- montants et currency
- lien ticket
- lien outlet / terminal / sales_session si présent
- lien user / actor si présent
- audit columns
- FK et index cohérents
- contraintes uniques évitant double payout si applicable
- table `_aud` alignée

#### `ticket`

- colonnes JPA ↔ SQL
- public code / ticket number / sync status / sale origin
- tenant / outlet / terminal / sales_session / seller user
- draw / draw channel / result status / settlement status
- total amount / total payout / currency
- timestamps métier : sold_at, cancelled_at, resulted_at, settled_at selon entité
- audit columns
- FK et index cohérents
- contraintes uniques sur code public / ticket number si applicable
- table `_aud` alignée
- vues `v_ticket_summary` et `v_ticket_print` alignées

#### `ticket_line`

- colonnes JPA ↔ SQL
- lien ticket
- bet type / selection key / stake / odds / payout
- line status / result status si présent
- JSONB éventuel via converters dédiés
- contraintes CHECK montants positifs
- index par ticket
- table `_aud` alignée si audited
- vue `v_ticket_print` alignée

#### `sales_session`

- colonnes JPA ↔ SQL
- tenant / outlet / terminal / seller user
- opened_at / closed_at
- status
- opening / closing balances si présents
- audit columns
- FK et index cohérents
- unique partiel pour session ouverte par terminal si requis
- table `_aud` alignée

#### `terminal`

- colonnes JPA ↔ SQL
- tenant / outlet
- code / serial / status / capabilities
- audit columns
- FK vers outlet
- contraintes uniques par tenant/outlet/code ou serial selon modèle
- index par tenant/outlet/status
- table `_aud` alignée

#### `outlet`

- colonnes JPA ↔ SQL
- tenant
- code / name / status / zone / address si présents
- audit columns
- contraintes uniques par tenant/code
- index par tenant/status
- table `_aud` alignée

## Required changes

### 1. Reconcile each entity with its `CREATE TABLE`

For every table in scope:

1. Open the JPA entity.
2. Open the corresponding `CREATE TABLE`.
3. Compare field by field:
   - column exists
   - SQL column name matches `@Column(name = "...")`
   - nullability matches entity constraint
   - length / precision / scale matches entity annotation
   - enum storage matches entity mapping
   - JSONB columns use the expected type
   - embedded / converted fields are represented correctly
   - optimistic lock `version` exists if entity inherits it
   - audit columns exist if inherited
   - tenant column exists for tenant-scoped entities

Acceptance:

- no entity column is missing from SQL
- no SQL column is orphaned unless explicitly documented as legacy/system column
- `ddl-auto=validate` passes

### 2. Reconcile Envers `_aud` tables

For every `@Audited` entity in scope:

1. Open the primary table definition.
2. Open the `_aud` table definition.
3. Ensure `_aud` has the same business columns as the primary table.
4. Ensure column types and nullability are compatible with Envers.
5. Ensure `rev`, `revtype`, and audit FK/indexes are present.
6. Remove stale audit columns that no longer exist in the primary table unless intentionally retained.

Acceptance:

- every audited primary column has a corresponding `_aud` column
- Envers validation passes
- no mismatch between primary and audit table

### 3. Reconcile foreign keys

For each table in scope:

- verify all `*_id` columns that represent relationships have the right FK
- verify FK target table and column
- verify delete behavior is intentional
- avoid cascade deletes on financial / ticket / ledger records unless explicitly justified
- ensure FK names are stable and grep-friendly

Special attention:

- `ticket_line.ticket_id -> ticket.id`
- `ticket.sales_session_id -> sales_session.id`
- `ticket.terminal_id -> terminal.id`
- `ticket.outlet_id -> outlet.id`
- `sales_session.terminal_id -> terminal.id`
- `terminal.outlet_id -> outlet.id`
- `payout.ticket_id -> ticket.id`
- `ledger_entry.ticket_id -> ticket.id` if present
- `ledger_entry.payout_id -> payout.id` if present

Acceptance:

- FK constraints match entity relationships and domain ownership
- no accidental cross-domain hard dependency is introduced beyond persistence integrity already accepted
- no FK points to a deleted/renamed table or column

### 4. Reconcile unique constraints

Review all unique business keys and idempotency keys.

Expected candidates:

- outlet code unique per tenant
- terminal code unique per tenant or outlet
- terminal serial unique if globally assigned
- ticket public code unique per tenant or globally, depending contract
- offline / sync keys unique if already modeled
- payout uniqueness to prevent duplicate payout for the same ticket/claim
- ledger idempotency key unique if present

Acceptance:

- unique constraints reflect the entity and business key model
- constraints are named explicitly
- no duplicate uniqueness exists in both table constraint and index unless intentional

### 5. Reconcile indexes

Update the existing index migration file.

Required index review:

- tenant-scoped access paths
- FK columns
- status filters
- date/time ordering
- list screens and admin pages
- ticket lookup by public code
- session lookup by terminal/status
- payout lookup by ticket/status
- ledger lookup by ticket/payout/session/occurred_at

Acceptance:

- every major FK has an index unless clearly unnecessary
- list endpoints have supporting indexes
- no obsolete index references a removed column
- no duplicate index with equivalent column order

### 6. Reconcile CHECK constraints

For money/status-critical tables:

- amounts must be non-negative or positive according to semantics
- payout amount cannot be negative
- ticket total amount cannot be negative
- stake amount must be positive
- enum-like text columns have CHECK constraints only if project convention uses them
- timestamps ordering constraints may be added if stable and not over-constraining

Acceptance:

- CHECK constraints match domain model and do not block legitimate lifecycle transitions
- constraints are named explicitly
- no business rule is incorrectly pushed into SQL if it belongs in domain logic

### 7. Reconcile RLS policies

For every tenant-scoped table in scope:

- ensure `tenant_id` exists
- ensure RLS is enabled
- ensure policy uses `current_tenant()`
- ensure soft-delete visibility participates if table has `deleted_at`
- ensure platform/super-admin access is intentional
- ensure no Java-side tenant filter is introduced

Acceptance:

- tenant-scoped tables are protected by RLS
- global tables remain non-RLS only if explicitly global
- policy names are stable and match conventions
- RLS tests / startup validation pass

### 8. Reconcile read-model views

Update `V108__create_read_views.sql` when touched tables affect views.

Known impacted views:

- `v_ticket_summary`
- `v_ticket_print`

Potentially impacted if joins changed:

- admin/session/outlet/terminal projections if they exist
- payout list / receipt views if SQL views exist

Acceptance:

- all referenced columns exist
- joins still match FK model
- view output matches application projections
- `flyway migrate` can create views from scratch

### 9. Reconcile seeds

If a changed column is:

- NOT NULL
- FK-backed
- part of a unique constraint
- used by startup bootstrap

then update the relevant `V20x` seed file.

Acceptance:

- clean database can be created from scratch
- seeds do not violate NOT NULL / FK / unique constraints

### 10. Remove obsolete patch migrations if present

If previous temporary migrations added `ALTER TABLE`, `CREATE INDEX`, `ADD CONSTRAINT`, or audit fixes for these same tables:

- absorb them into the original migration file
- delete the temporary patch migration only if it is not needed pre-go-live
- do not create a replacement migration

Acceptance:

- migration history remains clean for pre-go-live
- no duplicate DDL remains

## Files likely touched

The exact names may differ in the repository. The agent must locate the existing files before editing.

Expected categories:

```text
tchalanet-server/src/main/resources/db/migration/
  V100__*.sql   # base tables / CREATE TABLE
  V101__*.sql   # audit / Envers tables
  V103__*.sql   # indexes
  V104__*.sql   # triggers
  V105__*.sql   # RLS policies
  V108__create_read_views.sql
  V20x__*.sql   # seeds if impacted
```

Expected Java categories:

```text
core/**/infra/persistence/**JpaEntity.java
core/**/infra/persistence/**Repository.java
core/**/infra/persistence/**Mapper.java
core/**/application/query/model/**.java
core/**/infra/web/**Response.java
```

## Acceptance criteria

### Build / validation

The change is complete only when all pass:

```bash
./mvnw clean verify
```

And, where available:

```bash
flyway clean
flyway migrate
```

Spring Boot startup with:

```text
hibernate.hbm2ddl.auto=validate
```

must pass.

### SQL alignment

- [ ] `ledger_entry` table matches entity
- [ ] `ledger_entry_aud` matches audited columns if audited
- [ ] `payout` table matches entity
- [ ] `payout_aud` matches audited columns if audited
- [ ] `ticket` table matches entity
- [ ] `ticket_aud` matches audited columns if audited
- [ ] `ticket_line` table matches entity
- [ ] `ticket_line_aud` matches audited columns if audited
- [ ] `sales_session` table matches entity
- [ ] `sales_session_aud` matches audited columns if audited
- [ ] `terminal` table matches entity
- [ ] `terminal_aud` matches audited columns if audited
- [ ] `outlet` table matches entity
- [ ] `outlet_aud` matches audited columns if audited

### Constraints

- [ ] FK constraints are present and correct
- [ ] unique constraints are present and correct
- [ ] CHECK constraints match stable structural rules
- [ ] obsolete constraints are removed
- [ ] constraint names are explicit and stable

### Indexes

- [ ] FK columns indexed where useful
- [ ] tenant/status/date list indexes reviewed
- [ ] ticket lookup indexes reviewed
- [ ] payout lookup indexes reviewed
- [ ] session/terminal/outlet lookup indexes reviewed
- [ ] no obsolete or duplicate indexes remain

### RLS

- [ ] tenant-scoped tables have `tenant_id`
- [ ] tenant-scoped tables have RLS enabled
- [ ] policies use `current_tenant()`
- [ ] soft-delete visibility is respected when `deleted_at` exists
- [ ] no Java-side tenant filtering is introduced

### Views / projections

- [ ] `v_ticket_summary` updated if ticket/terminal/outlet/draw columns changed
- [ ] `v_ticket_print` updated if ticket/ticket_line columns changed
- [ ] payout/session/outlet/terminal projections updated if affected
- [ ] application read models still match view columns

### Seeds

- [ ] seed files updated if NOT NULL/FK/unique columns changed
- [ ] clean DB bootstrap works

## Agent instructions

Before editing:

1. Locate the current entity classes for all tables in scope.
2. Locate existing Flyway files by searching table names.
3. Do not create a new migration file.
4. If a new migration seems necessary, stop and report why, because pre-go-live policy says to modify the original migration.
5. Produce a table-by-table diff summary in the PR description.

During editing:

1. Edit `CREATE TABLE` definitions first.
2. Edit `_aud` tables second.
3. Edit indexes third.
4. Edit FK/unique/check constraints according to existing file organization.
5. Edit RLS policies.
6. Edit views.
7. Edit seeds only if required.
8. Run validation.

PR summary must include:

```text
Pre-go-live migration strategy:
- No new V*.sql file created.
- Changes absorbed into original CREATE TABLE / AUD / INDEX / RLS / VIEW files.

Tables aligned:
- ledger_entry
- payout
- ticket
- ticket_line
- sales_session
- terminal
- outlet

Validation:
- ./mvnw clean verify
- flyway clean && flyway migrate
- ddl-auto=validate
```

## Risks

### Risk: hidden mismatch between entity and `_aud`

Mitigation:

- compare primary table and audit table column by column
- run Envers / ddl validation

### Risk: view breakage

Mitigation:

- update `V108__create_read_views.sql`
- validate all views after clean migration

### Risk: duplicate indexes / constraints

Mitigation:

- search by column set, not only by index name
- remove obsolete duplicate DDL from original files

### Risk: accidental post-go-live style migration

Mitigation:

- no new `V*.sql`
- absorb patches into source migrations

### Risk: RLS regression

Mitigation:

- verify policies for every tenant-scoped table
- ensure no application-level tenant filtering is added

## Out of scope follow-up

Potential follow-up specs after this alignment:

- `align-payout-read-models-after-persistence-review`
- `align-sales-ticket-print-view`
- `add-archunit-flyway-entity-audit-alignment-checks`
- `document-ledger-payout-ticket-persistence-contract`
