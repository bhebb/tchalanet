# DOMAIN_OFFLINESYNC — conception corrigée v2.1

## 1. Vision

`core.offlinesync` permet à un terminal POS de continuer à vendre temporairement sans réseau, puis de réconcilier ces ventes avec le backend central lorsque la connectivité revient.

Le module est un sas de sécurité. Il ne remplace pas `core.sales`; il prépare, valide techniquement et trace des submissions qui seront ensuite promues par `sales` en tickets réels.

## 2. Package cible

```text
com.tchalanet.server.core.offlinesync
  api/
    command/
    query/
    event/
    model/
  internal/
    domain/
      model/
      service/
      exception/
    application/
      command/handler/
      query/handler/
      port/out/
      service/
    infra/
      web/
      persistence/
      event/
      batch/
      scheduler/
      config/
```

## 3. Dépendances autorisées

`core.offlinesync` peut consommer :

```text
common
catalog.api
platform.api
core.session.api.query
core.terminal.api.query
core.outlet.api.query
core.limitpolicy.api.query
```

`core.sales` consomme uniquement :

```text
core.offlinesync.api.event.*
```

Interdit :

```text
core.sales -> core.offlinesync.internal.*
core.sales -> offlinesync repository/query pour promouvoir une submission
core.offlinesync -> core.sales.internal.*
```

## 4. Entités

### 4.1 OfflineGrant

Autorisation temporaire donnée à un triplet opérationnel et device.

Champs principaux :

| Champ | Type | Note |
|---|---|---|
| id | OfflineGrantId | typed id |
| tenantId | TenantId | RLS / audit |
| sellerUserId | UserId | vendeur |
| terminalId | TerminalId | terminal |
| outletId | OutletId | point de vente |
| salesSessionId | SalesSessionId | session POS |
| deviceId | String | identifiant device |
| devicePublicKey | String | clé Ed25519 du device |
| keyId | String | version de clé |
| validFrom | Instant | début |
| validUntil | Instant | limite création locale |
| syncAcceptedUntil | Instant | limite réception serveur |
| maxTicketCount | Integer | quota |
| maxTotalAmount | Money | quota |
| consumedTicketCount | Integer | consommation TECH_VALIDATED |
| consumedTotalAmount | Money | consommation TECH_VALIDATED |
| status | OfflineGrantStatus | état |

États :

```text
ACTIVE
EXPIRED
REVOKED
SUPERSEDED
QUOTA_EXHAUSTED
```

Règles :

- `validFrom < validUntil < syncAcceptedUntil`.
- Un grant `SUPERSEDED` peut encore recevoir des submissions faites avant son `validUntil`, jusqu’à `syncAcceptedUntil`.
- Aucun nouveau grant n’est émis si le backend ne peut pas valider le contexte opérationnel.

### 4.2 OfflineCodeBatch

Lot de codes pré-alloués au device.

| Champ | Type | Note |
|---|---|---|
| id | OfflineCodeBatchId | typed id |
| grantId | OfflineGrantId | parent |
| batchNo | String | référence humaine |
| codeCount | Integer | nombre de codes |
| issuedAt | Instant | émission |
| expiresAt | Instant | aligné sur `grant.syncAcceptedUntil` |
| status | OfflineCodeBatchStatus | état |

États :

```text
ACTIVE
EXHAUSTED
EXPIRED
VOIDED
```

### 4.3 OfflineCode

Code court imprimé sur le ticket papier offline.

États :

```text
AVAILABLE
RESERVED
CONSUMED_PROMOTED
CONSUMED_REJECTED
VOIDED
EXPIRED
```

Transitions autorisées :

```text
AVAILABLE -> RESERVED
AVAILABLE -> EXPIRED
AVAILABLE -> VOIDED
RESERVED -> CONSUMED_PROMOTED
RESERVED -> CONSUMED_REJECTED
RESERVED -> VOIDED
```

Transition interdite :

```text
RESERVED -> AVAILABLE
```

### 4.4 OfflineSyncBatch

Envelope reçue lors de `POST /tenant/offline/sync`.

| Champ | Type | Note |
|---|---|---|
| id | OfflineSyncBatchId | typed id |
| grantId | OfflineGrantId | grant utilisé |
| clientBatchId | String | id local Flutter |
| batchPayloadHash | String | hash du batch normalisé |
| receivedAt | Instant | serveur |
| totalSubmissionCount | Integer | déclaré |
| acceptedCount | Integer | accepté technique |
| rejectedCount | Integer | rejet technique |
| duplicateCount | Integer | duplicats API |
| status | OfflineSyncBatchStatus | état |

Statuts :

```text
RECEIVED
PARTIALLY_ACCEPTED
ACCEPTED
REJECTED
COMPLETED
```

### 4.5 OfflineSubmission

Représentation serveur d’une vente offline avant promotion.

| Champ | Type | Note |
|---|---|---|
| id | OfflineSubmissionId | typed id |
| syncBatchId | OfflineSyncBatchId | parent |
| grantId | OfflineGrantId | grant |
| codeId | OfflineCodeId | code réservé |
| offlineCode | String | code humain |
| clientSubmissionId | String | id local Flutter |
| payloadHash | String | hash payload normalisé |
| clientSoldAt | Instant | heure déclarée/signée par device |
| receivedAt | Instant | serveur |
| totalStakeAmount | Money | total déclaré |
| lineCount | Integer | nombre de lignes |
| status | OfflineSubmissionStatus | état |
| rejectionCode | String | nullable |
| rejectionReason | String | nullable |
| promotionAttemptId | PromotionAttemptId | tentative courante |
| promotionRequestedAt | Instant | nullable |
| lastPromotionEventId | EventId | dernier retour traité |
| createdTicketId | TicketId | dénormalisation nullable |

Statuts persistants :

```text
RECEIVED
TECH_VALIDATED
TECH_REJECTED
PROMOTION_REQUESTED
PROMOTED
BUSINESS_REJECTED
NEEDS_ADMIN_REVIEW
ADMIN_APPROVED
ADMIN_REJECTED
SYNC_FAILED
```

`DUPLICATE` n’est pas un statut persistant.

### 4.6 OfflineSubmissionLine

Ligne de vente offline.

Règle : le payout potentiel côté offline est indicatif seulement. `sales` recalcule ce qui est nécessaire.

### 4.7 OfflineSubmissionTicketLink

Lien de traçabilité vers `sales.ticket`.

Types :

```text
CREATED
DUPLICATE_OF
REPLACED_BY
VOIDED_BY_SYNC
```

Une seule ligne `CREATED` par submission.

### 4.8 OfflineSubmissionDecision

Trace les décisions admin : approve, reject, replay dry-run.

## 5. Validation technique ordonnée

`OfflineSubmissionTechnicalPolicy` applique les contrôles dans cet ordre :

1. feature flag tenant `offlinesync.enabled` ;
2. plan tenant autorise offline ;
3. contexte opérationnel trusted présent ;
4. contexte POS validé par query ;
5. grant existe ;
6. grant non révoqué ;
7. `clientSoldAt` entre `validFrom` et `validUntil` ;
8. `receivedAt <= syncAcceptedUntil` ;
9. deviceId et public key correspondent au grant ;
10. signature Ed25519 valide ;
11. code existe dans le batch ;
12. code `AVAILABLE` puis lock pessimiste vers `RESERVED` ;
13. batch non expiré ;
14. payloadHash cohérent ;
15. quotas offline non dépassés.

Après succès :

- submission `TECH_VALIDATED` ;
- code reste `RESERVED` jusqu’au retour `sales` ;
- quotas grant incrémentés ;
- event `OfflineSubmissionTechValidatedEvent` publié after-commit.

Après rejet technique :

- submission `TECH_REJECTED` ;
- code `CONSUMED_REJECTED` si le code avait été réservé ;
- aucune promotion vers `sales`.

## 6. Promotion vers sales

`offlinesync` publie :

```text
OfflineSubmissionTechValidatedEvent
OfflineSubmissionAdminApprovedEvent
```

Ces events sont self-contained.

`core.sales` :

1. reçoit l’event ;
2. vérifie l’idempotence event ;
3. tente de créer le ticket ;
4. protège physiquement via unique `(tenant_id, offline_submission_id)` ;
5. publie `OfflineSubmissionProcessedEvent`.

`offlinesync` reçoit le retour et applique :

```text
PROMOTED          si ticket créé
BUSINESS_REJECTED si sales rejette métier
NEEDS_ADMIN_REVIEW si erreur récupérable ou conflit à traiter
```

## 7. Jobs

Les jobs sont thin :

```text
StuckSubmissionRecoveryJob
OrphanedCodeReservationJob
GrantExpirationJob
SyncAcceptedWindowCloseJob
```

Ils ne contiennent pas de logique métier. Ils sélectionnent des candidats et dispatchent des commands.
