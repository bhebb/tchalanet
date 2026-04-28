## ADDED Requirements

### Requirement: Endpoint stats journalier outlet

Le système SHALL exposer un endpoint `GET /tenant/stats/outlet/{id}/daily?date=YYYY-MM-DD`
dans `features/stats/outlet_daily/OutletDailyStatsController`.

Le controller SHALL :

- Être annoté `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")` au niveau classe
- Récupérer le `tenantId` via `@CurrentContext TchRequestContext ctx`
- Dispatcher `GetOutletDailySummaryQuery(tenantId, outletId, localDate)` via `QueryBus`
- Retourner `ApiResponse<OutletDailySummary>`
- En cas de `DateTimeParseException` sur le paramètre `date` → lever `ProblemRest.badRequest("invalid date format: " + date)`

#### Scenario: GET daily stats — succès

- **WHEN** `GET /tenant/stats/outlet/{id}/daily?date=2026-04-28` est appelé avec un utilisateur TENANT_ADMIN
- **THEN** la réponse est HTTP 200 avec `ApiResponse<OutletDailySummary>`
- **AND** le controller ne contient aucune logique métier (thin controller)

#### Scenario: Date invalide → 400

- **WHEN** `GET /tenant/stats/outlet/{id}/daily?date=NOT-A-DATE` est appelé
- **THEN** la réponse est HTTP 400 avec un message d'erreur lisible

#### Scenario: Ancien endpoint `/platform/outlets/{id}/daily-summary` supprimé

- **WHEN** `GET /platform/outlets/{id}/daily-summary` est appelé
- **THEN** la réponse est HTTP 404 (endpoint absent)
