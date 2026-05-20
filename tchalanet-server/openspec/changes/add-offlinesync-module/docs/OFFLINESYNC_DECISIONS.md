# OFFLINESYNC — décisions finales v2.1

## 1. Positionnement

`offlinesync` est un domaine métier critique sous :

```text
core.offlinesync
```

Raison : il protège des ventes, de l’argent, de la fraude, des quotas et de la promotion vers les tickets réels. Ce n’est pas un service technique transversal.

## 2. Frontière du domaine

### `core.offlinesync` fait

- Émettre, renouveler et révoquer des `OfflineGrant`.
- Allouer des batches de codes offline signés.
- Recevoir les batches de sync venant du POS.
- Valider techniquement les submissions offline.
- Tracer l’état, les décisions admin et les incidents.
- Publier des events self-contained vers `sales`.
- Recevoir les events de retour de `sales` et fermer la boucle.

### `core.offlinesync` ne fait pas

- Créer directement des tickets.
- Calculer le prix final d’un ticket.
- Décider les gagnants.
- Appliquer les limites métier finales de vente.
- Valider seul tout le contexte POS.
- Écrire dans les tables `sales`.

## 3. Invariants non négociables

### I1 — Un code soumis est brûlé

Un `OfflineCode` qui a été réservé pour une submission serveur ne revient jamais à `AVAILABLE`.

États finaux possibles après soumission :

```text
CONSUMED_PROMOTED
CONSUMED_REJECTED
VOIDED
EXPIRED
```

La transition suivante est interdite :

```text
RESERVED -> AVAILABLE
```

### I2 — Une submission ne crée jamais deux tickets

Protection en deux couches :

1. idempotence applicative avec `promotionAttemptId` et `clientSubmissionId + payloadHash` ;
2. contrainte physique dans `sales.ticket` :

```sql
UNIQUE (tenant_id, offline_submission_id)
```

### I3 — `DUPLICATE` n’est pas un état persistant

Si le serveur reçoit une submission déjà connue :

- même `clientSubmissionId` ;
- même `payloadHash` ;

alors il retourne un résultat API `DUPLICATE` qui pointe vers la submission originale. Il ne crée pas une nouvelle ligne `OfflineSubmission`.

Si le même `clientSubmissionId` arrive avec un `payloadHash` différent, le serveur retourne un conflit :

```text
offlinesync.submission.payload_mismatch
```

### I4 — Deux fenêtres temporelles distinctes

```text
validUntil          = limite de création locale d’une vente offline
syncAcceptedUntil   = limite de réception serveur d’une submission déjà créée
```

Une vente faite avant `validUntil` peut être synchronisée après `validUntil`, tant que `receivedAt <= syncAcceptedUntil`.

### I5 — Events self-contained

`core.sales` consomme les events d’`offlinesync` sans refaire de query retour vers `offlinesync`.

Les events de promotion doivent contenir :

- `eventId`
- `occurredAt`
- `tenantId`
- `offlineSubmissionId`
- `promotionAttemptId`
- contexte POS validé ou snapshot utile
- seller, terminal, outlet, session
- code offline
- `clientSoldAt`
- lignes de vente
- totaux déclarés
- hash payload

### I6 — Retour obsolète ignoré

Quand `offlinesync` reçoit `OfflineSubmissionProcessedEvent`, il applique le retour seulement si :

```text
event.promotionAttemptId == submission.currentPromotionAttemptId
```

Sinon l’event est ignoré et logué comme obsolète.

### I7 — Contexte opérationnel trusted obligatoire

Les use cases suivants doivent commencer par :

```java
ctx.trustedOperationalContextRequired();
```

Puis valider le contexte via query dédiée, par exemple :

```java
ResolvePosOperationContextQuery
```

Use cases concernés :

```text
offline grant
offline sync
sell
payout
```

### I8 — Les quotas offline consomment les tentatives TECH_VALIDATED

`consumedTicketCount` et `consumedTotalAmount` sont incrémentés quand une submission passe la validation technique (`TECH_VALIDATED`).

Raison : une vente offline techniquement valide, même rejetée ensuite par `sales`, a consommé du risque terrain.

## 4. Ordre recommandé de build

```text
1. domain model + state machines offlinesync
2. migrations SQL offlinesync + sales.offline_submission_id
3. grant issuance + code batch allocation
4. sync technique sans promotion
5. events offlinesync -> sales -> offlinesync
6. dashboard admin minimal
7. jobs de recovery
8. Flutter offline local flow
```
