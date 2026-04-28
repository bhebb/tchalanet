## 1. SQL migrations — tables principales (in-place)

- [ ] 1.1 `V5__core_terminal.sql` — ADD `kind varchar(16) NOT NULL DEFAULT 'PHYSICAL'`, `owner_agent_id uuid NULL`, `status varchar(16) NOT NULL DEFAULT 'ACTIVE'`
- [ ] 1.2 `V5__core_terminal.sql` — ADD contraintes `chk_terminal_kind`, `chk_terminal_status`, `chk_terminal_kind_owner`
- [ ] 1.3 `V5__core_terminal.sql` — CREATE UNIQUE INDEX `ux_terminal_one_active_virtual_per_agent ON terminal(tenant_id, owner_agent_id) WHERE kind='VIRTUAL' AND status='ACTIVE' AND deleted_at IS NULL`
- [ ] 1.4 `V4__core_outlet_table.sql` — ADD `kind varchar(16) NOT NULL DEFAULT 'FIXED'`, `owner_agent_id uuid NULL`
- [ ] 1.5 `V4__core_outlet_table.sql` — ADD contraintes `chk_outlet_kind`, `chk_outlet_kind_owner`
- [ ] 1.6 `V4__core_outlet_table.sql` — CREATE INDEX `ix_outlet_owner_agent ON outlet(tenant_id, owner_agent_id) WHERE deleted_at IS NULL AND owner_agent_id IS NOT NULL`
- [ ] 1.7 `V8__core_pos.sql` (→ `sales_session`) — ADD colonnes z-report : `opening_float numeric(18,2) NOT NULL DEFAULT 0`, `closing_amount numeric(18,2) NULL`, `expected_amount numeric(18,2) NULL`, `variance numeric(18,2) NULL`, `variance_note text NULL`, `closed_by uuid NULL`, `tickets_count bigint NOT NULL DEFAULT 0`, `total_stake_htg numeric(18,2) NOT NULL DEFAULT 0`
- [ ] 1.8 `V8__core_pos.sql` — DROP CONSTRAINT `chk_sales_session_status` + ADD avec `CHECK (status IN ('OPEN','CLOSED','ABORTED'))`
- [ ] 1.9 `V8__core_pos.sql` — DROP CONSTRAINT `chk_sales_session_close` + ADD avec condition incluant `ABORTED`
- [ ] 1.10 `V3__core_settings.sql` — ADD `agent_id uuid NULL`, `is_overridable_by_outlet boolean NOT NULL DEFAULT false`, `is_overridable_by_terminal boolean NOT NULL DEFAULT false`, `is_overridable_by_agent boolean NOT NULL DEFAULT false`
- [ ] 1.11 `V3__core_settings.sql` — DROP CONSTRAINT `ck_app_setting_level` + ADD avec `CHECK (level IN ('GLOBAL','TENANT','OUTLET','TERMINAL','AGENT'))`
- [ ] 1.12 `V3__core_settings.sql` — DROP CONSTRAINT `chk_app_setting_scope` (ou équivalent) + ADD avec la branche `level='AGENT'`
- [ ] 1.13 `V3__core_settings.sql` — DROP INDEX `ux_app_setting_active_scope` + CREATE UNIQUE INDEX incluant `COALESCE(agent_id, '00000000-...'::uuid)`

## 2. SQL migrations — tables \_aud Envers (V43, in-place)

- [ ] 2.1 `V43__audit_table.sql` — `terminal_aud` : ADD `kind varchar(16) NULL`, `owner_agent_id uuid NULL`, `status varchar(16) NULL`
- [ ] 2.2 `V43__audit_table.sql` — `outlet_aud` : ADD `kind varchar(16) NULL`, `owner_agent_id uuid NULL`
- [ ] 2.3 `V43__audit_table.sql` — `app_setting_aud` : ADD `agent_id uuid NULL`, `is_overridable_by_outlet boolean NULL`, `is_overridable_by_terminal boolean NULL`, `is_overridable_by_agent boolean NULL`
- [ ] 2.4 `V43__audit_table.sql` — `app_setting_aud` : DROP CONSTRAINT `app_setting_aud_level_check` + ADD avec `CHECK (level IN ('GLOBAL','TENANT','OUTLET','TERMINAL','AGENT'))`
- [ ] 2.5 `V43__audit_table.sql` — `sales_session_aud` (post-rename) : ADD `opening_float numeric(18,2) NULL`, `closing_amount numeric(18,2) NULL`, `expected_amount numeric(18,2) NULL`, `variance numeric(18,2) NULL`, `variance_note text NULL`, `closed_by uuid NULL`, `tickets_count bigint NULL`, `total_stake_htg numeric(18,2) NULL`
- [ ] 2.6 `V43__audit_table.sql` — `sales_session_aud` : DROP CONSTRAINT `sales_session_aud_status_check` + ADD avec `CHECK (status IN ('OPEN','CLOSED','ABORTED'))`

## 3. SQL migrations — seed permissions (V32, in-place)

- [ ] 3.1 `V32__seed_iam_roles_permissions.sql` — ADD permission keys terminal : `terminal.read`, `terminal.write`, `terminal.self.write` (avec `ON CONFLICT DO NOTHING`)
- [ ] 3.2 `V32__seed_iam_roles_permissions.sql` — ADD permission keys outlet : `outlet.read`, `outlet.write`, `outlet.self.write`
- [ ] 3.3 `V32__seed_iam_roles_permissions.sql` — ADD permission keys settings : `setting.read`, `setting.global.write`, `setting.tenant.write`, `setting.outlet.write`, `setting.terminal.write`, `setting.self.write`, `setting.restore`, `setting.hard_delete`
- [ ] 3.4 `V32__seed_iam_roles_permissions.sql` — ADD permission keys session : `session.abort` (compléter `session.read` si absent)
- [ ] 3.5 `V32__seed_iam_roles_permissions.sql` — ADD permission keys sales : `sales.place`, `sales.approve`, `sales.history.self.read`, `sales.history.outlet.read`, `sales.history.tenant.read`, `sales.results.margin.read`, `sales.cancel.self`, `sales.cancel.any`
- [ ] 3.6 `V32__seed_iam_roles_permissions.sql` — ADD permission keys limits/autonomy : `limit.read`, `limit.write`, `autonomy.read`, `autonomy.write`
- [ ] 3.7 `V32__seed_iam_roles_permissions.sql` — ADD permission keys print/sync : `print.test`, `sync.trigger`
- [ ] 3.8 `V32__seed_iam_roles_permissions.sql` — ADD role-permission rows pour chaque clé × rôle par défaut (AGENT, TENANT_ADMIN, OPERATOR, SUPER_ADMIN) avec `ON CONFLICT DO NOTHING`
- [ ] 3.9 Vérification : `grep -r "terminal\.read\|outlet\.read\|setting\.global" src/main/resources/db/migration/V32` — toutes les clés présentes

## 4. `core.terminal` — domain extension

- [ ] 4.1 Créer enum `TerminalKind { PHYSICAL, VIRTUAL }` dans `core.terminal.domain.model`
- [ ] 4.2 Créer enum `TerminalStatus { ACTIVE, DISABLED, ARCHIVED }` dans `core.terminal.domain.model`
- [ ] 4.3 Ajouter `kind` (final), `ownerAgentId` (final), `status` dans l'agrégat `Terminal` + factory methods `createPhysical` / `createVirtual`
- [ ] 4.4 Ajouter méthodes `disable()`, `enable()`, `archive()`, `rename()`, `reassignToOutlet()` avec invariants (`ensureNotArchived`)
- [ ] 4.5 Ajouter `findActiveVirtualForAgent(AppUserId)` dans `TerminalReaderPort`; ajouter `listByOutlet(OutletId, TerminalStatus)` si absent
- [ ] 4.6 Créer handler `ProvisionVirtualTerminalCommandHandler` : `findActiveVirtualForAgent` → retourner si existant, sinon `createVirtual` + `save`
- [ ] 4.7 Créer handler `ChangeTerminalStatusCommandHandler` + publier `TerminalStatusChangedEvent`
- [ ] 4.8 Mettre à jour `TerminalJpaEntity` : ajouter `kind`, `ownerAgentId`, `status` + `@Enumerated(STRING)` + `@Table(name="terminal")` vérifié
- [ ] 4.9 Mettre à jour `TerminalMapper` pour mapper les nouveaux champs
- [ ] 4.10 Vérifier `@AuditTable` sur `TerminalJpaEntity` — doit pointer `terminal_aud` (pas de valeur stale)

## 5. `core.outlet` — domain extension

- [ ] 5.1 Créer enum `OutletKind { FIXED, MOBILE, VIRTUAL }` dans `core.outlet.domain.model`
- [ ] 5.2 Créer enum `OutletField` avec classification `isOperational()` / `isJuridical()` (tous les champs du spec 02)
- [ ] 5.3 Ajouter `kind`, `ownerAgentId` dans l'agrégat `Outlet` + invariant kind/owner
- [ ] 5.4 Créer handler `UpdateOutletCommandHandler` — boucle sur `Map<OutletField,Object>`, vérification par champ selon rôle/kind/ownership
- [ ] 5.5 Créer handler `ReassignOutletOwnerCommandHandler` — mettre à jour `ownerAgentId` + publier `OutletKindOrOwnerChangedEvent`
- [ ] 5.6 Créer handler `ChangeOutletStatusCommandHandler` — vérifier `status` présent dans la table outlet (OQ-2) + publier `OutletStatusChangedEvent`
- [ ] 5.7 Créer query handler `GetMyOutletQueryHandler`
- [ ] 5.8 Mettre à jour `OutletJpaEntity` : ajouter `kind`, `ownerAgentId` + `@Enumerated(STRING)`
- [ ] 5.9 Vérifier `@AuditTable` sur `OutletJpaEntity` — doit pointer `outlet_aud`

## 6. `core.session` — SalesSession extension

- [ ] 6.1 Ajouter `ABORTED` dans `SalesSessionStatus` enum
- [ ] 6.2 Ajouter champs z-report dans `SalesSession` : `openingFloat`, `closingAmount`, `expectedAmount`, `variance`, `varianceNote`, `closedBy`, `ticketsCount`, `totalStakeHtg`
- [ ] 6.3 Implémenter `SalesSession.close(closingAmount, varianceNote, by, clock)` — calcul `expectedAmount` et `variance`
- [ ] 6.4 Implémenter `SalesSession.abort(reason, by, clock)`
- [ ] 6.5 Implémenter `SalesSession.recordTicket(stake)` (package-private, appelé par le listener)
- [ ] 6.6 Mettre à jour `OpenSalesSessionCommandHandler` — valider les 7 règles du spec 03 (rôle AGENT, outlet, terminal ACTIVE, kind VIRTUAL owner check, pas de double-open, openingFloat >= 0)
- [ ] 6.7 Créer handler `CloseSalesSessionCommandHandler` + publier `SalesSessionClosedEvent`
- [ ] 6.8 Créer handler `AbortSalesSessionCommandHandler` + publier `SalesSessionAbortedEvent`
- [ ] 6.9 Créer handler `AbortSalesSessionsForUserCommandHandler` (bulk abort)
- [ ] 6.10 Créer handler `AbortSalesSessionsForTerminalCommandHandler` (bulk abort)
- [ ] 6.11 Créer query handler `GetSessionCloseSnapshotQueryHandler` → `SessionCloseSnapshot`
- [ ] 6.12 Mettre à jour `SalesSessionJpaEntity` — ajouter les 8 nouveaux champs + `@Table(name="sales_session")` vérifié
- [ ] 6.13 Vérifier `@AuditTable` sur `SalesSessionJpaEntity` — doit pointer `sales_session_aud`

## 7. `catalog.settings` — cascade extension

- [ ] 7.1 Créer enum `SettingKey` avec tous les couples `(namespace, key)` du spec 04 (26 clés minimum) + champs `category`, `maxLevel`, `validator`, `defaultValue`, `auditPolicy`
- [ ] 7.2 Créer `SettingRegistryValidator` `@Component` — au démarrage, requêter toutes les lignes `app_setting` actives et vérifier que chaque `(namespace, setting_key)` est dans l'enum; fail-fast sinon
- [ ] 7.3 Auditer `V36__seed_app_settings_batch.sql` — vérifier que toutes les clés seedées existent dans `SettingKey` (OQ-1)
- [ ] 7.4 Ajouter `agentId`, `isOverridableByOutlet`, `isOverridableByTerminal`, `isOverridableByAgent` dans `AppSettingJpaEntity`
- [ ] 7.5 Implémenter `SettingsResolverPort` — ordre AGENT→TERMINAL→OUTLET→TENANT→GLOBAL avec 3 gates + ownership exception VIRTUAL (D6)
- [ ] 7.6 Mettre à jour `UpsertSettingCommandHandler` — valider valeur via `SettingKey.validator` + publier `SettingChangedEvent(UPSERT)`
- [ ] 7.7 Créer handlers `SoftDeleteSettingCommandHandler`, `RestoreSettingCommandHandler`, `HardDeleteSettingCommandHandler` + publier `SettingChangedEvent`
- [ ] 7.8 Créer query handler `GetEffectiveSettingQueryHandler` — retourner `EffectiveSetting<T>` avec `editable` calculé côté serveur (D7)
- [ ] 7.9 Créer query handler `GetEffectiveNamespaceQueryHandler`
- [ ] 7.10 Vérifier `@AuditTable` sur `AppSettingJpaEntity` — doit pointer `app_setting_aud`

## 8. `core.accesscontrol` — vérification après seed

- [ ] 8.1 Vérifier que les handlers qui vérifient `terminal.write`, `outlet.self.write`, `setting.self.write`, `session.abort` etc. utilisent les constantes Java et non des strings hardcodées
- [ ] 8.2 S'assurer que le rôle `AGENT` ne donne pas implicitement les droits d'autres rôles (test unitaire ou ArchUnit)

## 9. Événements et listeners cross-domaine

- [ ] 9.1 Ajouter champ `UUID eventId` (généré à publication) dans tous les records d'événements — terminal, outlet, session, settings (spec 06)
- [ ] 9.2 Créer `TerminalCacheEvictListener` — écoute `TerminalCreatedEvent`, `TerminalStatusChangedEvent`, `TerminalReassignedEvent`; évicte `core.terminal.by_id` et `core.terminal.virtual_for_agent`; idempotent via `ProcessedEventPort(handler_key="terminal.cache-evict")`
- [ ] 9.3 Créer `OutletCacheEvictListener` — écoute Outlet\* events; évicte caches outlet; `handler_key="outlet.cache-evict"`
- [ ] 9.4 Créer `SettingsCacheEvictListener` — écoute `SettingChangedEvent`; éviction scope-aware; `handler_key="setting.cache-evict"`
- [ ] 9.5 Créer `SalesSessionAbortOnTerminalDisabledListener` — écoute `TerminalStatusChangedEvent`; si `newStatus != ACTIVE` → dispatch `AbortSalesSessionsForTerminalCommand` via `CommandBus`; `handler_key="session.abort-on-terminal-disabled"`; ignorer si `newStatus=ACTIVE`
- [ ] 9.6 Confirmer que `UserSuspendedEvent` existe dans `core.user` (OQ-3); créer `SalesSessionAbortOnUserSuspendedListener`; `handler_key="session.abort-on-user-suspended"`
- [ ] 9.7 Créer `SalesSessionAbortOnOutletOwnerChangedListener` — écoute `OutletKindOrOwnerChangedEvent`; abort sessions ouvertes sur cet outlet pour l'ancien owner; `handler_key="session.abort-on-outlet-owner-changed"`
- [ ] 9.8 Confirmer que `TicketPlacedEvent` porte un `sessionId` (OQ-4); créer `SalesSessionTicketCountersListener`; `handler_key="sales-session.ticket-counters"`; idempotent
- [ ] 9.9 Vérifier que tous les listeners utilisent `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@TchTx`

## 10. Gates de validation

- [ ] 10.1 `./mvnw compile` — exit 0
- [ ] 10.2 `docker compose down -v && docker compose up -d postgres` — DB recréée from scratch
- [ ] 10.3 `./mvnw flyway:migrate` — exit 0
- [ ] 10.4 `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` — exit 0
- [ ] 10.5 Application démarre sans `SettingRegistryValidator` failure
- [ ] 10.6 `grep -r "kind = 'PHYSICAL'\|kind = 'VIRTUAL'" src/main/java/` → zéro résultat (uniquement enum references)
- [ ] 10.7 Vérifier dans Postgres : `\d terminal_aud`, `\d outlet_aud`, `\d app_setting_aud`, `\d sales_session_aud` — toutes les nouvelles colonnes présentes
