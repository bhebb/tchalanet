# OFFLINESYNC — guide d’implémentation

## 1. Application commands

```text
RequestOfflineGrantCommand
RenewOfflineGrantCommand
RevokeOfflineGrantCommand
SyncOfflineSalesCommand
ApproveOfflineSubmissionCommand
RejectOfflineSubmissionCommand
ReplayOfflineSubmissionCommand
ExpireOfflineGrantCommand
RecoverStuckOfflineSubmissionCommand
```

Tous les handlers de write utilisent `@UseCase` + `@TchTx`.

## 2. Queries

```text
GetOfflineGrantQuery
GetCurrentOfflineGrantQuery
GetOfflineSubmissionQuery
ListOfflineSubmissionsQuery
GetOfflineSyncBatchQuery
GetOfflineDashboardQuery
```

Les queries retournent des views, pas des aggregates.

## 3. Ports out

```text
OfflineGrantWriterPort
OfflineGrantReaderPort
OfflineCodeWriterPort
OfflineCodeReaderPort
OfflineSubmissionWriterPort
OfflineSubmissionReaderPort
OfflineSyncBatchWriterPort
OfflineSyncBatchReaderPort
OfflineCryptoPort
```

Les ports utilisent des typed IDs et des modèles domain/application.

## 4. Policies domain pures

```text
OfflineGrantPolicy
OfflineSubmissionTechnicalPolicy
OfflineCodeTransitionPolicy
OfflineSyncPromotionPolicy
```

Aucune injection Spring, aucun repository, aucun bus dans ces policies.

## 5. Handler SyncOfflineSalesCommand — flow

```text
1. Lire TchRequestContext.
2. Exiger trustedOperationalContextRequired().
3. Valider POS via ResolvePosOperationContextQuery.
4. Vérifier idempotence du batch clientBatchId + batchPayloadHash.
5. Créer OfflineSyncBatch RECEIVED.
6. Pour chaque submission :
   a. détecter duplicate clientSubmissionId + payloadHash ;
   b. détecter payload mismatch ;
   c. créer OfflineSubmission RECEIVED ;
   d. lock code AVAILABLE -> RESERVED ;
   e. appliquer OfflineSubmissionTechnicalPolicy ;
   f. si validée : TECH_VALIDATED + promotionAttemptId ;
   g. publier OfflineSubmissionTechValidatedEvent after-commit.
7. Retourner un résultat détaillé par submission.
```

## 6. Listener sales

Le listener sales ne doit pas appeler `offlinesync`.

```text
OfflineSubmissionTechValidatedEvent
  -> CreateTicketFromOfflineSubmissionCommand
  -> Ticket créé ou rejet métier
  -> OfflineSubmissionProcessedEvent
```

Protection obligatoire :

```sql
UNIQUE (tenant_id, offline_submission_id)
```

## 7. Jobs

Les jobs appellent des commands :

```text
RecoverStuckOfflineSubmissionCommand
ExpireOfflineGrantCommand
ReleaseOrMarkOrphanedReservedCodeCommand
CloseSyncAcceptedWindowCommand
```

Ils ne font pas de transition directement dans le scheduler.

## 8. Tests P0

- code soumis ne revient jamais à `AVAILABLE` ;
- duplicate même hash ne crée pas de ligne ;
- duplicate hash différent retourne conflit ;
- vente faite avant `validUntil` mais sync avant `syncAcceptedUntil` acceptée ;
- sync après `syncAcceptedUntil` rejetée ;
- event retour avec promotionAttemptId obsolète ignoré ;
- double event sales ne crée pas deux tickets ;
- DB unique empêche deux tickets même si listener rejoué.
