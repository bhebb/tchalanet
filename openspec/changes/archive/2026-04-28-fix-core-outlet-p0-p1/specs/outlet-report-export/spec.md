## ADDED Requirements

### Requirement: Export rapport CSV outlet

Le systeme SHALL exposer deux endpoints dans `features/reporting/outletreport/OutletReportExportController` :

- `GET /tenant/reports/outlet/{id}/export?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /tenant/reports/outlet/{id}/download?date=YYYY-MM-DD`

Le controller SHALL etre annote `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")` au niveau classe.

- Dispatcher `GenerateOutletReportQuery(tenantId, outletId, from, to)` via `QueryBus`
- `/export` : retourner `ApiResponse<String>` (chemin ou statut)
- `/download` : retourner `ResponseEntity<Resource>` avec `Content-Disposition: attachment`, `Content-Type: text/csv`

#### Scenario: Export avec plage de dates

- **WHEN** `GET /tenant/reports/outlet/{id}/export?from=2026-04-01&to=2026-04-28` est appele
- **THEN** le rapport CSV est genere pour toute la plage
- **AND** `query.to()` est utilise par `GenerateOutletReportQueryHandler`

#### Scenario: Download CSV succes

- **WHEN** `GET /tenant/reports/outlet/{id}/download?date=2026-04-28` est appele
- **THEN** HTTP 200 avec `Content-Disposition: attachment; filename="outlet-report-2026-04-28.csv"`
- **AND** le fichier temporaire `/tmp/` est supprime apres streaming

#### Scenario: Date invalide retourne 400

- **WHEN** un parametre de date non parseable est fourni
- **THEN** HTTP 400 via `ProblemRest.badRequest("invalid date format: ...")`

#### Scenario: Anciens endpoints supprimes

- **WHEN** `GET /platform/outlets/{id}/report` est appele
- **THEN** HTTP 404

### Requirement: GenerateOutletReportQueryHandler utilise from ET to

`GenerateOutletReportQueryHandler` SHALL appeler `reportPort.generateReport(outletId, from, to)`.
`OutletReportPort.generateDailyReport(outletId, from)` SHALL etre remplace par `generateReport(outletId, from, to)`.
`FilesystemOutletReportAdapter` SHALL gerer la plage multi-jours.

#### Scenario: Rapport mono-jour

- **WHEN** `from == to`
- **THEN** le rapport couvre exactement ce jour

#### Scenario: Rapport multi-jours

- **WHEN** `from < to`
- **THEN** le CSV consolide tous les jours de la plage avec une colonne `date`

### Requirement: Pas de fuite fichier temporaire

Le fichier `/tmp` SHALL etre supprime apres streaming, meme en cas d'erreur (bloc `finally`).

#### Scenario: Fichier supprime apres streaming reussi

- **WHEN** le client telecharge le fichier
- **THEN** le fichier temporaire n'existe plus sur le serveur

#### Scenario: Fichier supprime en cas d'erreur

- **WHEN** une exception survient pendant le streaming
- **THEN** le fichier temporaire est quand meme supprime
