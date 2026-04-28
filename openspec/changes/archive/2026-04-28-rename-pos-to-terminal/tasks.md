## 1. SQL migrations — édition in-place

- [x] 1.1 `V8__core_pos.sql` — remplacer `pos_session` → `sales_session` dans le `CREATE TABLE`, les `DROP TABLE IF EXISTS`, les index (`ux_pos_session_open_per_terminal`, `ix_pos_session_tenant_terminal`, `ix_pos_session_tenant_opened_at`) et le trigger (`trg_pos_session_updated_at`)
- [x] 1.2 `V8__core_pos.sql` — remplacer `pos_session_totals` → `sales_session_totals` dans le `CREATE TABLE`, la FK vers `sales_session(id)`, les index (`ux_pos_session_totals_session_id`, `ix_pos_session_totals_tenant`, `ix_pos_session_totals_tenant_session`) et le trigger (`trg_pos_session_totals_updated_at`)
- [x] 1.3 `V43__audit_table.sql` — remplacer `pos_session_aud` → `sales_session_aud` : `CREATE TABLE`, contraintes (`pos_session_aud_pkey` → `sales_session_aud_pkey`, `pos_session_aud_status_check` → `sales_session_aud_status_check`), `OWNER TO`, `GRANT`
- [x] 1.4 `V43__audit_table.sql` — remplacer `pos_session_totals_aud` → `sales_session_totals_aud` : `CREATE TABLE`, contrainte (`pos_session_totals_aud_pkey` → `sales_session_totals_aud_pkey`), `OWNER TO`, `GRANT`
- [x] 1.5 `V40__rls_policies.sql` — remplacer `'pos_session'` → `'sales_session'` dans le tableau de tables soumises aux politiques RLS
- [x] 1.6 `V35__seed_outlet_terminal_pos.sql` — remplacer toutes les occurrences de `pos_session` (INSERT, SELECT, RAISE NOTICE, commentaires) par `sales_session`
- [x] 1.7 Vérification : `grep -r "pos_session" src/main/resources/db/migration/` → zéro résultat

## 2. Package Java — `core.pos` → `core.terminal`

- [x] 2.1 IntelliJ "Move Package" sur `com.tchalanet.server.core.pos` → `com.tchalanet.server.core.terminal` (met à jour les déclarations `package` et tous les imports)
- [x] 2.2 Vérifier que les 28 fichiers sont bien sous `core/terminal/` et qu'aucun fichier ne reste sous `core/pos/`
- [x] 2.3 Vérifier : `grep -r "com.tchalanet.server.core.pos" src/` → zéro résultat

## 3. Renommage des classes `PosSession*` dans `core.session`

- [x] 3.1 Renommer `PosSession` → `SalesSession` (`core.session.domain.model`)
- [x] 3.2 Renommer `PosSessionStatus` → `SalesSessionStatus` (`core.session.domain.model`)
- [x] 3.3 Renommer `PosSessionTotals` → `SalesSessionTotals` (`core.session.domain.model`)
- [x] 3.4 Renommer `PosSessionReaderPort` → `SalesSessionReaderPort` (`core.session.application.port.out`)
- [x] 3.5 Renommer `PosSessionWriterPort` → `SalesSessionWriterPort` (`core.session.application.port.out`)
- [x] 3.6 Renommer `PosSessionTotalsReaderPort` → `SalesSessionTotalsReaderPort` (`core.session.application.port.out`)
- [x] 3.7 Renommer `PosSessionTotalsWriterPort` → `SalesSessionTotalsWriterPort` (`core.session.application.port.out`)
- [x] 3.8 Renommer `PosSessionTotalsAggregatePort` → `SalesSessionTotalsAggregatePort` (`core.session.application.port.out`)
- [x] 3.9 Renommer `RecomputePosSessionTotalsCommand` → `RecomputeSalesSessionTotalsCommand` (`core.session.application.command.model`)
- [x] 3.10 Renommer `PosSessionJpaEntity` → `SalesSessionJpaEntity` (`core.session.infra.persistence.entity`)
- [x] 3.11 Renommer `PosSessionTotalsJpaEntity` → `SalesSessionTotalsJpaEntity` (`core.session.infra.persistence.entity`)
- [x] 3.12 Renommer `PosSessionJpaRepository` → `SalesSessionJpaRepository` (`core.session.infra.persistence.repository`)
- [x] 3.13 Renommer `PosSessionTotalsJpaRepository` → `SalesSessionTotalsJpaRepository` (`core.session.infra.persistence.repository`)
- [x] 3.14 Renommer `PosSessionStatusConverter` → `SalesSessionStatusConverter` (`core.session.infra.persistence.converter`)
- [x] 3.15 Renommer `PosSessionMapper` → `SalesSessionMapper` (`core.session.infra.persistence.mapper`)
- [x] 3.16 Renommer `PosSessionTotalsMapper` → `SalesSessionTotalsMapper` (`core.session.infra.persistence.mapper`)
- [x] 3.17 Renommer `PosSessionRepositoryAdapter` → `SalesSessionRepositoryAdapter` (`core.session.infra.persistence.adapter`)
- [x] 3.18 Renommer `PosSessionTotalsReaderAdapter` → `SalesSessionTotalsReaderAdapter` (`core.session.infra.persistence.adapter`)
- [x] 3.19 Renommer `PosSessionTotalsWriterAdapter` → `SalesSessionTotalsWriterAdapter` (`core.session.infra.persistence.adapter`)
- [x] 3.20 Renommer `PosSessionTotalsAggregateAdapter` → `SalesSessionTotalsAggregateAdapter` (`core.session.infra.persistence.adapter`)
- [x] 3.21 Renommer `PosSessionTotalsProjectionListener` → `SalesSessionTotalsProjectionListener` (`core.session.infra.event`)
- [x] 3.22 Renommer `PosSessionController` → `SalesSessionController` (`core.session.infra.web`)
- [x] 3.23 Renommer `PosSessionTotalsController` → `SalesSessionTotalsController` (`core.session.infra.web`)
- [x] 3.24 Renommer `PosSessionResponse` → `SalesSessionResponse` (`core.session.infra.web.model`)

## 4. Vérification des annotations Java

- [x] 4.1 Ouvrir `SalesSessionJpaEntity` — vérifier que `@Table(name = "sales_session")` est correctement mis à jour (pas d'occurrence stale `pos_session`)
- [x] 4.2 Ouvrir `SalesSessionTotalsJpaEntity` — vérifier `@Table(name = "sales_session_totals")`
- [x] 4.3 Chercher `@AuditTable` dans `core.session` — si trouvé avec `"pos_session_aud"`, mettre à jour en `"sales_session_aud"` (idem pour `_totals`) — N/A : annotation non présente
- [x] 4.4 Vérifier : `grep -r "PosSession" src/main/java/` → zéro résultat

## 5. Tests — renommage

- [x] 5.1 Renommer `PosSessionControllerTest` → `SalesSessionControllerTest` — N/A : fichier inexistant avant la change
- [x] 5.2 Renommer `PosSessionRepoPortTest` → `SalesSessionRepoPortTest` — N/A : fichier inexistant avant la change
- [x] 5.3 Renommer `PosSessionMapperTest` → `SalesSessionMapperTest` — N/A : fichier inexistant avant la change
- [x] 5.4 Renommer les fichiers de fixtures JSON/YAML référencés par ces tests — N/A : pas de fixtures associées
- [x] 5.5 Vérifier : `grep -r "PosSession" src/test/java/` → zéro résultat

## 6. Documentation — mise à jour in-place

- [x] 6.1 Déplacer `core/pos/DOMAIN_POS.md` → `core/terminal/DOMAIN_TERMINAL.md`; mettre à jour le titre et toutes les références `core.pos` / `PosSession*`
- [x] 6.2 `core/session/DOMAIN_SESSION.md` — remplacer `PosSession` → `SalesSession`, `PosSessionRepoPort` → `SalesSessionReaderPort`
- [x] 6.3 `core/sales/DOMAIN_SALES.md` — remplacer `PosSessionTotalsProjectionListener` → `SalesSessionTotalsProjectionListener`
- [x] 6.4 `tchalanet-server/docs/audit/2026-04-26-sales-pipeline-audit.md` — remplacer toutes les occurrences `pos_session` / `PosSession*`
- [x] 6.5 `tchalanet-server/docs/audit/audit-core-tenantuser-2026-04-27.md` — remplacer toutes les occurrences `pos_session` / `PosSession*`
- [x] 6.6 `tchalanet-docs/docs/00-audit/GAPS.md` — remplacer `PosSessionController` → `SalesSessionController`
- [x] 6.7 `tchalanet-docs/docs/00-audit/AUDIT-SALES.md` — remplacer toutes les occurrences `PosSession*` et `pos_session`
- [x] 6.8 `tchalanet-docs/docs/02-functional/domains/pos.md` — mettre à jour le lien vers `DOMAIN_TERMINAL.md`
- [x] 6.9 `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` — remplacer `PosSessionReaderPort` → `SalesSessionReaderPort`, `PosSessionTotalsProjectionListener` → `SalesSessionTotalsProjectionListener`
- [x] 6.10 `tchalanet-docs/docs/02-functional/flows/verify-ticket.md` — remplacer `core.pos` → `core.terminal`, mettre à jour le lien vers `DOMAIN_TERMINAL.md`

## 7. Gates de validation

- [ ] 7.1 `./mvnw compile` — exit 0, zéro erreur de symbole non résolu
- [ ] 7.2 `docker compose down -v && docker compose up -d postgres` — DB recréée from scratch
- [ ] 7.3 `./mvnw flyway:migrate` — exit 0
- [ ] 7.4 `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` — exit 0
- [x] 7.5 `grep -r "pos_session\|PosSession\|core\.pos" src/ docs/` (hors `openspec/changes/`) → zéro résultat
