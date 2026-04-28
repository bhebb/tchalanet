## Why

`core.outlet` présente 7 anomalies identifiées à l'audit `2026-04-28-core-outlet-audit.md` :
un controller de reporting mal placé dans le core (brise la règle architecturale feature/core),
un typed ID manquant (`addressId` en `UUID` brut), un handler retournant `UUID` au lieu d'`OutletId`,
trois handlers sans domain events after-commit, une vue read-model avec UUIDs bruts, et un query
handler qui ignore le paramètre `to()` dans la plage de dates.

Ces corrections sont nécessaires avant l'implémentation de `pos-v0-features` (le BFF POS consomme
`OutletId` typé) et avant d'activer les listeners stats.

## What Changes

### P0-1 — Migration du controller reporting hors de `core.outlet`

**Décision architecturale** :

- `OutletDailySummary` (stat live opérationnelle) → **`features/stats/outlet_daily`**
  - Nouveau slice `features/stats/outlet_daily/`
  - Path : `GET /tenant/stats/outlet/{id}/daily?date=YYYY-MM-DD`
  - Scope : TENANT_ADMIN + SUPER_ADMIN
- Génération et téléchargement de rapport CSV → **`features/reporting/outletreport`**

  - Nouveau slice `features/reporting/outletreport/`
  - Path : `GET /tenant/reports/outlet/{id}/export?from=X&to=Y`
  - Path : `GET /tenant/reports/outlet/{id}/download?date=X`
  - Scope : TENANT_ADMIN + SUPER_ADMIN

- **Supprimer** `core/outlet/infra/web/admin/OutletReportController.java`
- Corriger `DateTimeParseException` → `ProblemRest.badRequest("invalid date format: …")`
- Corriger fuite fichier `/tmp` : `Files.deleteIfExists(path)` dans un bloc `finally` ou via
  `@Async` post-response

### P1-1 — Typer `Outlet.addressId` en `AddressId`

- `Outlet.addressId : UUID` → `AddressId` (nullable)
- Tous les constructeurs `Outlet` : `UUID addressId` → `AddressId addressId`
- `Outlet.withAddressId(UUID)` → `withAddressId(AddressId)`
- `CreateOutletCommandHandler` : passer directement `AddressId` (plus de conversion intermédiaire `UUID`)
- `UpdateOutletConfigCommandHandler` : idem
- `OutletPersistenceAdapter` : UUID en DB, conversion `AddressId ↔ UUID` dans le mapper

### P1-2 — `CreateOutletCommand` retourne `OutletId`

- `CreateOutletCommandHandler` : `CommandHandler<CreateOutletCommand, UUID>` → `CommandHandler<CreateOutletCommand, OutletId>`
- Handler : `return OutletId.of(newId)` au lieu de `return newId`
- Controller appelant (à identifier et adapter) : `OutletId` retourné

### P1-3 — Domain events dans les handlers outlet

- **`CloseOutletDayCommandHandler`** : `AfterCommit → OutletDayClosedEvent(eventId, occurredAt, tenantId, outletId, closedDate)`
- **`ReopenOutletDayCommandHandler`** : `AfterCommit → OutletDayReopenedEvent(eventId, occurredAt, tenantId, outletId, reopenedDate)`
- **`UpdateOutletConfigCommandHandler`** : `AfterCommit → OutletConfigUpdatedEvent(eventId, occurredAt, tenantId, outletId)`
- Créer les 3 records dans `core/outlet/domain/event/`
- Javadoc : consommateurs attendus documentés (aucun listener créé dans ce change)
- Injecter `DomainEventPublisher`, `IdGenerator`, `Clock` dans les 3 handlers

### P1-4 — `ReopenOutletDayCommandHandler` — notice `salesBlocked`

- Après `reopenDay()`, si `outlet.salesBlocked() == true` : **ne pas lever automatiquement le blocage**
  (décision intentionnelle — `salesBlocked` est une décision indépendante de la réouverture)
- Ajouter `ReopenOutletDayResult` avec : `outletId`, `dayReopened: boolean`, `salesStillBlocked: boolean` + `salesBlockReason`
- Le handler retourne `ReopenOutletDayResult` (passer de `VoidCommandHandler` à `CommandHandler<…, ReopenOutletDayResult>`)
- Le controller expose `salesStillBlocked` dans la réponse `ApiResponse<ReopenOutletDayResult>`

### P1-5 — `OutletView` — typed IDs

- `OutletView.id : UUID` → `OutletId`
- `OutletView.tenantId : UUID` → `TenantId`
- Mettre à jour `OutletPersistenceAdapter` (ou le mapper) pour construire les typed IDs

### P1-6 — Corriger `GenerateOutletReportQueryHandler` — plage de dates

- Le handler appelle `reportPort.generateDailyReport(query.outletId(), query.from())` — `query.to()` ignoré
- Corriger : passer les deux dates `from` + `to`
- `OutletReportPort.generateDailyReport(outletId, from)` → `generateReport(outletId, from, to)`
- `FilesystemOutletReportAdapter` : si multi-jours, itérer (un fichier par jour) ou générer
  un fichier CSV consolidé selon la plage

## Capabilities

### New Capabilities

- `outlet-daily-stats`: Endpoint `GET /tenant/stats/outlet/{id}/daily` dans `features/stats/outlet_daily`
- `outlet-report-export`: Endpoints export/download CSV dans `features/reporting/outletreport`
- `outlet-domain-events`: 3 domain events outlet (`OutletDayClosedEvent`, `OutletDayReopenedEvent`, `OutletConfigUpdatedEvent`)
- `reopen-outlet-day-result`: `ReopenOutletDayResult` avec notice `salesStillBlocked`

### Modified Capabilities

_(aucune — corrections d'implémentation sans changement de contrat existant, sauf suppression
du controller temporaire et adaptation des retours de handlers)_

## Impact

**Java sources** :

- `core/outlet/infra/web/admin/OutletReportController.java` — **SUPPRIMER**
- `core/outlet/domain/model/Outlet.java` — `addressId` UUID → AddressId
- `core/outlet/application/query/model/OutletView.java` — id/tenantId UUID → typed IDs
- `core/outlet/application/command/handler/CreateOutletCommandHandler.java` — retour `OutletId`
- `core/outlet/application/command/handler/CloseOutletDayCommandHandler.java` — domain event
- `core/outlet/application/command/handler/ReopenOutletDayCommandHandler.java` — domain event + result
- `core/outlet/application/command/handler/UpdateOutletConfigCommandHandler.java` — domain event + AddressId
- `core/outlet/application/query/handler/GenerateOutletReportQueryHandler.java` — utiliser from+to
- `core/outlet/application/port/out/OutletReportPort.java` — signature `generateReport(id, from, to)`
- `core/outlet/infra/report/FilesystemOutletReportAdapter.java` — plage de dates
- `core/outlet/infra/persistence/OutletPersistenceAdapter.java` — mapper AddressId ↔ UUID
- `core/outlet/domain/event/OutletDayClosedEvent.java` — **NOUVEAU**
- `core/outlet/domain/event/OutletDayReopenedEvent.java` — **NOUVEAU**
- `core/outlet/domain/event/OutletConfigUpdatedEvent.java` — **NOUVEAU**
- `features/stats/outlet_daily/OutletDailyStatsController.java` — **NOUVEAU**
- `features/reporting/outletreport/OutletReportExportController.java` — **NOUVEAU**

**SQL / Flyway** : aucune migration requise

**API** :

- `GET /platform/outlets/{id}/daily-summary` → **SUPPRIMÉ**
- `GET /platform/outlets/{id}/report` → **SUPPRIMÉ**
- `GET /platform/outlets/{id}/report/download` → **SUPPRIMÉ**
- `GET /tenant/stats/outlet/{id}/daily` → **NOUVEAU**
- `GET /tenant/reports/outlet/{id}/export` → **NOUVEAU**
- `GET /tenant/reports/outlet/{id}/download` → **NOUVEAU**
