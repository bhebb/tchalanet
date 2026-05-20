# Offline Sales Risk Policy — Tchalanet

> **Status**: Draft / Design decision  
> **Scope**: `tchalanet-server`, mobile cashier app, terminal sync  
> **Applies to**: `core.sales`, `core.terminal`, `core.session`, `core.outlet`, future mobile offline storage  
> **Goal**: permettre la vente hors ligne en Haïti sans transformer le terminal en source de vérité métier.

---

## 1. Contexte

Beaucoup de vendeurs haïtiens peuvent avoir une connexion absente, instable ou insuffisante. Le système doit donc permettre une vente offline contrôlée.

La règle structurante est :

```text
offline normal
online optimal
server remains source of truth
```

La vente offline est autorisée uniquement dans une enveloppe pré-autorisée, signée, limitée et synchronisée ensuite.

---

## 2. Principes non négociables

1. Le serveur reste la source de vérité.
2. Un ticket offline est provisoire jusqu’à synchronisation.
3. Le payout offline est interdit.
4. La session de vente reste obligatoire, même offline.
5. Le terminal doit posséder un snapshot offline valide.
6. La synchronisation est idempotente et faite par batch.
7. Les limites offline sont inférieures ou égales aux limites online.
8. Les ventes offline sont revalidées côté serveur.
9. Les reçus offline doivent indiquer clairement que la validation finale dépend de la sync.
10. Les risques doivent être visibles dans le reporting par terminal, outlet, vendeur et session.

---

## 3. Modèle conceptuel

### 3.1 OfflineSalesCapability

Le terminal télécharge une capacité offline quand il est online.

Contenu minimal :

```text
tenantId
outletId
terminalId
sellerUserId
salesSessionId
offlineWindowStart
offlineWindowEnd
allowedDraws
allowedGames
pricingSnapshot
limitsSnapshot
receiptConfigSnapshot
maxOfflineTicketAmount
maxOfflineTotalAmount
maxOfflineTicketCount
lastKnownServerTime
snapshotId
snapshotSignature
```

Sans snapshot valide, aucune vente offline n’est autorisée.

### 3.2 Ticket offline

Chaque ticket offline doit contenir :

```text
clientTicketId
terminalId
localSequence
idempotencyKey
offlineBatchId
createdAtDevice
snapshotId
lines
stakeAmount
feeAmount
totalAmount
ticketHash
ticketSignature
```

Le ticket devient immuable après impression.

### 3.3 Statuts recommandés

Garder le statut métier du ticket simple et ajouter des champs dédiés :

```java
public enum SaleOrigin {
  ONLINE,
  OFFLINE
}
```

```java
public enum TicketSyncStatus {
  NONE,
  PENDING,
  ACCEPTED,
  REJECTED,
  CONFLICT,
  REVIEW_REQUIRED
}
```

```java
public enum OfflineRiskFlag {
  CLOCK_DRIFT,
  LIMIT_EXCEEDED,
  SEQUENCE_GAP,
  SNAPSHOT_EXPIRED,
  CUTOFF_RISK,
  USER_REVOKED,
  TERMINAL_LOCKED,
  SESSION_CLOSED,
  DUPLICATE_SYNC,
  INVALID_SIGNATURE,
  PRICING_MISMATCH,
  DRAW_NOT_ALLOWED
}
```

---

## 4. Actions offline permises

Les actions suivantes peuvent être autorisées offline si le snapshot est valide :

```text
login offline avec token/snapshot valide
consulter snapshot
vendre ticket dans enveloppe autorisée
imprimer ticket offline
réimprimer ticket local
annuler localement un ticket non synchronisé selon policy
lister tickets locaux
préparer sync
```

---

## 5. Actions offline interdites

Les actions suivantes doivent rester online uniquement :

```text
payout
vente hors snapshot
vente sans session snapshot
vente après validUntil
vente sur draw absent du snapshot
vente avec pricing absent du snapshot
override admin
fermeture journée outlet
fermeture session finale
modification odds/pricing
modification limites
changement utilisateur/terminal/outlet
correction résultat
validation définitive ticket
```

---

## 6. Matrice des risques et mitigations

| Risque                          | Criticité | Détection                                               | Mitigations                                                                | Décision recommandée                        |
| ------------------------------- | --------: | ------------------------------------------------------- | -------------------------------------------------------------------------- | ------------------------------------------- |
| Vente après cutoff              |  Critique | `createdAtDevice` hors fenêtre, draw fermé, drift élevé | `cutoffAt` signé, `validUntil` court, revalidation serveur                 | Reject ou `REVIEW_REQUIRED` selon tolérance |
| Dépassement limites offline     |     Élevé | Montant/ticket/batch/session dépasse plafond            | `maxOfflineTicketAmount`, `maxOfflineTotalAmount`, `maxOfflineTicketCount` | Reject excédent + alerte admin              |
| Login offline frauduleux        |     Élevé | Token expiré, user révoqué, terminal verrouillé         | Token signé court, PIN/biométrie, device binding                           | Pas de snapshot valide = pas de vente       |
| Terminal volé                   |     Élevé | Terminal locked, activité anormale, clé révoquée        | validité courte, limites basses, PIN, key rotation                         | Reject sync selon policy de verrouillage    |
| Faux ticket                     |  Critique | Signature invalide, hash mismatch, QR incohérent        | Ticket signature, hash canonique, public verify                            | Reject + flag fraude                        |
| Ticket modifié avant sync       |  Critique | Hash imprimé différent du payload                       | Immutabilité après impression, ticketHash, signature                       | Reject                                      |
| Sync sélective                  |     Élevé | Gaps de `localSequence`, tickets manquants              | Séquence monotone, append-only sync, batch complet                         | `CONFLICT` ou `REVIEW_REQUIRED`             |
| Double sync / replay            |     Moyen | `offlineBatchId` ou `idempotencyKey` déjà connu         | Unique constraints, réponse idempotente                                    | Retourner résultat précédent                |
| Session fermée pendant offline  |     Moyen | `createdAtDevice > session.closedAt`                    | sessionId dans snapshot, fenêtre courte, tolerance policy                  | Reject après `closedAt + tolerance`         |
| Outlet bloqué pendant offline   |     Élevé | `salesBlocked` changé avant sync                        | validUntil court, revalidation outlet à sync                               | Review ou reject selon `blockedAt`          |
| Ancien pricing / odds           |     Moyen | `pricingSnapshotId` inconnu/expiré ou mismatch          | Snapshot signé, oddsSnapshot par ligne                                     | Reject si mismatch                          |
| Payout d’un ticket pending      |  Critique | `syncStatus != ACCEPTED`                                | Payout online uniquement, lock transactionnel payout                       | Interdit non négociable                     |
| Manipulation heure device       |     Élevé | Drift vs `serverTimeAtSnapshot`, timestamps incohérents | Monotonic clock, drift limit, validUntil serveur                           | Review/reject                               |
| Reset app/device                |     Élevé | Séquence recule, batchId inconnu, state local perdu     | secure storage, dernier compteur serveur, sequence check                   | Conflict/review                             |
| Connexion volontairement coupée |     Élevé | vendeur souvent offline, offline ratio élevé            | limites offline basses, reporting risque                                   | Alerte admin / réduire capacité offline     |
| Litige client                   |     Moyen | Ticket pending présenté comme valide                    | reçu explicite, public verify clair                                        | Formation + texte reçu obligatoire          |
| Perte device avant sync         |     Moyen | Tickets imprimés mais jamais synchronisés               | backup chiffré, QR signé, alerte pending                                   | Review opérationnelle                       |

---

## 7. Login offline

Le login offline est autorisé seulement si :

```text
un login online a déjà été validé
un offline token signé existe
le token n’est pas expiré
le terminal est lié au sellerUserId
le snapshot contient le sellerUserId
le PIN local est valide
```

Mitigations recommandées :

```text
offline token signed
short expiration
PIN required
biometric optional
device binding
sellerUserId fixed in snapshot
revocation checked at sync
```

Décision métier à documenter : si un utilisateur est révoqué après émission du snapshot mais avant synchronisation, les tickets peuvent être rejetés, acceptés dans la fenêtre signée, ou mis en review. La recommandation est de mettre en review les cas ambigus et de bloquer tout futur snapshot.

---

## 8. Politique de synchronisation

### 8.1 Endpoint cible

```http
POST /tenant/offline-sales/sync
```

La sync doit être batchée et idempotente.

### 8.2 Validation serveur par ticket

Ordre recommandé :

```text
1. idempotency / duplicate check
2. terminal exists + terminal key valid
3. snapshot exists + signature valid
4. seller user valid
5. session valid
6. outlet not blocked according to policy
7. localSequence valid
8. createdAtDevice within allowed offline window
9. draw allowed
10. cutoff valid
11. pricing/odds match snapshot
12. ticket money breakdown valid
13. offline limits not exceeded
14. persist accepted/rejected/conflict result
15. update session cash summary for accepted tickets only
16. publish TicketPlacedEvent / OfflineTicketRejectedEvent after commit
```

### 8.3 Résultat individuel par ticket

Chaque ticket doit recevoir un résultat indépendant :

```text
ACCEPTED
REJECTED
CONFLICT
REVIEW_REQUIRED
DUPLICATE_ALREADY_ACCEPTED
```

---

## 9. Money model offline

La séparation suivante est obligatoire :

```text
stakeAmount = montant joué
feeAmount   = frais SMS/service/taxe
totalAmount = stakeAmount + feeAmount
```

Règles :

```text
payout potentiel basé sur stakeAmount
cash session basé sur totalAmount
session.salesCashIn augmente seulement pour tickets acceptés
```

`TicketPlacedEvent` doit distinguer :

```text
stakeAmountCents
feeAmountCents
totalAmountCents
```

---

## 10. Reçu offline

Le reçu doit afficher explicitement :

```text
MODE HORS LIGNE
VALIDATION FINALE APRÈS SYNCHRONISATION
NON PAYABLE AVANT VALIDATION
syncStatus = PENDING
localSequence
publicCode
verificationCode
createdAtDevice
dernière synchronisation connue
ticketHash court
QR code signé
```

Le client ne doit jamais croire qu’un ticket offline est définitivement validé.

---

## 11. Architecture backend proposée

```text
core.sales
  domain
    model
      Ticket
      TicketLine
      OfflineSaleBatch
      OfflineSalePolicy
      SaleOrigin
      TicketSyncStatus
      OfflineRiskFlag
  application
    command.model
      SellTicketCommand
      SyncOfflineSalesCommand
      CancelOfflineTicketCommand
    command.handler
      SellTicketCommandHandler
      SyncOfflineSalesCommandHandler
    query.model
      GetOfflineSalesCapabilityQuery
      ListOfflineTicketsQuery
    query.handler
      GetOfflineSalesCapabilityQueryHandler
  infra.web
    TenantOfflineSalesController
```

`core.terminal` reste responsable de l’état terminal :

```text
autoSessionEnabled
syncState
lastSeen
terminal key
terminal lock
```

`core.sales` reste responsable de décider si la vente offline est acceptée.

---

## 12. Reporting et audit

Les rapports doivent distinguer :

```text
online sales amount
offline accepted amount
offline rejected amount
offline pending amount
offline review amount
risk flags by seller
risk flags by terminal
risk flags by outlet
sync delay average
sequence gaps
clock drift incidents
```

Événements recommandés :

```text
OfflineSaleBatchSyncedEvent
OfflineTicketAcceptedEvent
OfflineTicketRejectedEvent
OfflineTicketReviewRequiredEvent
OfflineRiskDetectedEvent
```

Les événements doivent être publiés after-commit.

---

## 13. Règle finale

La vente offline est permise uniquement comme capacité limitée :

```text
vente offline courte
montant plafonné
nombre de tickets plafonné
tirages préchargés seulement
payout interdit
sync obligatoire avant settlement/payout
audit complet
```

Toute violation doit produire un statut explicite :

```text
REJECTED
CONFLICT
REVIEW_REQUIRED
```

et jamais une acceptation silencieuse.
