## ADDED Requirements

### Requirement: ReopenOutletDayResult avec notice salesBlocked

`ReopenOutletDayCommandHandler` SHALL retourner `ReopenOutletDayResult` :

- `outletId: OutletId`
- `dayReopened: boolean`
- `salesStillBlocked: boolean`
- `salesBlockReason: String` (nullable)

**DECISION** : la reouverture du jour ne leve PAS automatiquement `salesBlocked`.
`salesBlocked` est une decision operationnelle independante.
Si `salesBlocked == true` apres reouverture, le resultat l'indique via `salesStillBlocked=true`.

Le controller SHALL exposer `ReopenOutletDayResult` dans `ApiResponse<ReopenOutletDayResult>`.

#### Scenario: Reouverture sans salesBlocked

- **WHEN** `ReopenOutletDayCommand` est traite sur un outlet non bloque
- **THEN** le handler retourne `ReopenOutletDayResult{dayReopened=true, salesStillBlocked=false}`
- **AND** aucun auto-unblock n'est effectue

#### Scenario: Reouverture avec salesBlocked actif

- **WHEN** `ReopenOutletDayCommand` est traite sur un outlet avec `salesBlocked=true`
- **THEN** le handler retourne `ReopenOutletDayResult{dayReopened=true, salesStillBlocked=true, salesBlockReason="..."}`
- **AND** `salesBlocked` reste `true` (pas de levee automatique)
- **AND** le controller retourne un `ApiResponse` avec `salesStillBlocked=true` pour que le frontend affiche un warning

#### Scenario: Reouverture d'un jour deja ouvert (idempotent)

- **WHEN** `ReopenOutletDayCommand` est traite sur un outlet dont `dayClosed=false`
- **THEN** le handler retourne `ReopenOutletDayResult{dayReopened=false, salesStillBlocked=outlet.salesBlocked()}`
