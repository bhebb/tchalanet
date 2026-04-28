## 1. Suppression OutletReportController (nettoyage core)

- [ ] 1.1 Supprimer `core/outlet/infra/web/admin/OutletReportController.java`
- [ ] 1.2 Vérifier qu'aucun import ne référence ce controller

## 2. Typed IDs — Outlet.addressId

- [ ] 2.1 Modifier `Outlet` : champ `addressId` de `UUID` → `AddressId`
- [ ] 2.2 Mettre à jour tous les constructeurs de `Outlet` : paramètre `UUID addressId` → `AddressId addressId`
- [ ] 2.3 Remplacer `Outlet.withAddressId(UUID)` par `withAddressId(AddressId)`
- [ ] 2.4 Corriger `CreateOutletCommandHandler` : passer directement `AddressId` (supprimer conversion `UUID`)
- [ ] 2.5 Corriger `UpdateOutletConfigCommandHandler` : idem
- [ ] 2.6 Corriger `OutletPersistenceAdapter` : mapper `AddressId ↔ UUID` dans la couche infra
- [ ] 2.7 `./mvnw compile` → 0 erreur

## 3. Typed IDs — OutletView

- [ ] 3.1 Modifier `OutletView.id : UUID` → `OutletId`
- [ ] 3.2 Modifier `OutletView.tenantId : UUID` → `TenantId`
- [ ] 3.3 Mettre à jour le mapper / constructeur dans `OutletPersistenceAdapter` ou le query handler
- [ ] 3.4 Vérifier les tests et controllers qui consomment `OutletView`

## 4. CreateOutletCommand retourne OutletId

- [ ] 4.1 Modifier `CreateOutletCommandHandler` : `CommandHandler<CreateOutletCommand, UUID>` → `CommandHandler<CreateOutletCommand, OutletId>`
- [ ] 4.2 Modifier le corps du handler : `return OutletId.of(newId)` au lieu de `return newId`
- [ ] 4.3 Adapter le controller qui dispatch `CreateOutletCommand` (identifier le controller appelant, adapter le retour)

## 5. Domain events outlet

- [ ] 5.1 Créer `core/outlet/domain/event/OutletDayClosedEvent.java`
  - record : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`, `LocalDate closedDate`
  - implements `DomainEvent`
  - Javadoc : consommateurs attendus (stats/aggregates, pos)
- [ ] 5.2 Créer `core/outlet/domain/event/OutletDayReopenedEvent.java`
  - record : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`, `LocalDate reopenedDate`
  - implements `DomainEvent`
- [ ] 5.3 Créer `core/outlet/domain/event/OutletConfigUpdatedEvent.java`
  - record : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `OutletId outletId`
  - implements `DomainEvent`
- [ ] 5.4 `CloseOutletDayCommandHandler` : injecter `DomainEventPublisher`, `IdGenerator`, `Clock` + publier `OutletDayClosedEvent` after-commit
- [ ] 5.5 `ReopenOutletDayCommandHandler` : injecter `DomainEventPublisher`, `IdGenerator`, `Clock` + publier `OutletDayReopenedEvent` after-commit
- [ ] 5.6 `UpdateOutletConfigCommandHandler` : injecter `DomainEventPublisher`, `IdGenerator`, `Clock` + publier `OutletConfigUpdatedEvent` after-commit

## 6. ReopenOutletDayResult

- [ ] 6.1 Créer `core/outlet/application/command/model/ReopenOutletDayResult.java`
  - record : `OutletId outletId`, `boolean dayReopened`, `boolean salesStillBlocked`, `String salesBlockReason`
- [ ] 6.2 Modifier `ReopenOutletDayCommandHandler` : `VoidCommandHandler<ReopenOutletDayCommand>` → `CommandHandler<ReopenOutletDayCommand, ReopenOutletDayResult>`
- [ ] 6.3 Logique : si `outlet.salesBlocked() == true` après `reopenDay()` → `salesStillBlocked=true` dans le résultat (PAS de levée automatique)
- [ ] 6.4 Adapter le controller appelant pour retourner `ApiResponse<ReopenOutletDayResult>`

## 7. GenerateOutletReportQueryHandler — plage de dates

- [ ] 7.1 Modifier `OutletReportPort` : `generateDailyReport(outletId, from)` → `generateReport(outletId, from, to)`
- [ ] 7.2 Modifier `GenerateOutletReportQueryHandler` : appeler `reportPort.generateReport(query.outletId(), query.from(), query.to())`
- [ ] 7.3 Modifier `FilesystemOutletReportAdapter` : implémenter la plage multi-jours (itérer ou CSV consolidé avec colonne `date`)

## 8. Feature stats — OutletDailyStatsController

- [ ] 8.1 Créer `features/stats/outlet_daily/OutletDailyStatsController.java`
  - `@RestController`, `@RequestMapping("/tenant/stats/outlet")`, `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - `GET /{id}/daily?date=YYYY-MM-DD` → `ApiResponse<OutletDailySummary>`
  - Dispatcher `GetOutletDailySummaryQuery` via `QueryBus`
  - `DateTimeParseException` → `ProblemRest.badRequest("invalid date format: " + date)`

## 9. Feature reporting — OutletReportExportController

- [ ] 9.1 Créer `features/reporting/outletreport/OutletReportExportController.java`
  - `@RestController`, `@RequestMapping("/tenant/reports/outlet")`, `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")`
  - `GET /{id}/export?from=X&to=Y` → `ApiResponse<String>`
  - `GET /{id}/download?date=X` → `ResponseEntity<Resource>` avec `Content-Disposition: attachment`
  - `DateTimeParseException` → `ProblemRest.badRequest("invalid date format: ...")`
  - Bloc `finally` pour supprimer le fichier `/tmp` après streaming

## 10. Vérification finale

- [ ] 10.1 `./mvnw compile` → 0 erreur
- [ ] 10.2 `./mvnw test -pl tchalanet-server` → vert
- [ ] 10.3 `GET /platform/outlets/{id}/daily-summary` → HTTP 404 (supprimé)
- [ ] 10.4 `GET /tenant/stats/outlet/{id}/daily?date=2026-04-28` → HTTP 200
- [ ] 10.5 `GET /tenant/reports/outlet/{id}/export?from=2026-04-01&to=2026-04-28` → rapport multi-jours
- [ ] 10.6 Build vert — checklist DoD complète
