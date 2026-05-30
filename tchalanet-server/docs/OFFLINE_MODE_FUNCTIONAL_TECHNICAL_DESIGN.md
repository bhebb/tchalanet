# Tchalanet — Mode Offline Sales

> **Status**: SUPERSEDED — implémenté dans `core.offlinesync` (2026). Source canonique : `tchalanet-core/.../core/offlinesync/DOMAIN_OFFLINESYNC.md` et `openspec/changes/add-offlinesync-module/`.  
> **Conserver** : contient les décisions de conception et les risques historiques.  

> **Scope**: `tchalanet-server`, Flutter mobile app, POS app  
> **Domains**: `core.offlinesync`, `core.sales`, `core.session`, `core.terminal`, `core.outlet`, `core.limitpolicy`, `core.payout`, `features.stats`, `features.tenantadmin`  
> **Priority**: Security first, user experience second

---

## 1. Résumé exécutif

Tchalanet doit permettre aux vendeurs, notamment en Haïti, de continuer à vendre même avec une connexion faible, instable ou absente pendant plusieurs heures ou plusieurs jours.

Cependant, une vente offline ne peut pas être considérée automatiquement comme une vente officielle, car le vendeur peut :

- modifier l’heure du téléphone ;
- vendre après le cutoff ou après le tirage ;
- synchroniser seulement certains tickets ;
- dépasser ses limites offline ;
- utiliser un snapshot/grant expiré ;
- réinstaller l’application pour réinitialiser les compteurs locaux ;
- imprimer des reçus qui ressemblent à des tickets officiels.

La décision structurante est donc :

```text
Une vente offline est une soumission provisoire.
Elle devient un vrai Ticket Sales seulement si Sales l’accepte après synchronisation.
```

Cela implique deux domaines séparés :

```text
core.offlinesync
  = journal technique, preuve, synchronisation, signature, codes offline, séquences, risques techniques.

core.sales
  = vérité métier des tickets acceptés, argent, résultat, payout eligibility, stats sales.
```

Règle non négociable :

```text
Aucune soumission offline rejetée, suspecte ou en review ne doit entrer dans sales.ticket.
```

Les statistiques officielles, la caisse, les commissions, le ledger et le payout se basent uniquement sur les tickets acceptés par Sales.

---

## 2. Objectifs

### 2.1 Objectifs fonctionnels

Le mode offline doit permettre à un vendeur de :

- se préparer à vendre offline lorsqu’il est encore online ;
- recevoir un grant offline signé ;
- recevoir un lot de codes offline pré-réservés ;
- vendre localement dans l’application Flutter/POS ;
- imprimer un reçu clairement marqué `OFFLINE` ;
- synchroniser les ventes plus tard ;
- voir quelles ventes sont acceptées, rejetées ou en review ;
- permettre au client de vérifier son code offline.

### 2.2 Objectifs techniques

Le backend doit :

- séparer les soumissions offline des vrais tickets Sales ;
- stocker les preuves brutes pour audit ;
- détecter les fraudes et anomalies ;
- empêcher la pollution des stats Sales ;
- empêcher les payouts sur tickets non acceptés ;
- publier les events métier uniquement après acceptation Sales ;
- permettre au tenant de configurer des politiques offline ;
- fournir des dashboards de risque offline.

### 2.3 Objectifs sécurité

Le système doit privilégier la sécurité sur l’expérience utilisateur :

```text
Si le système ne peut pas prouver qu’une vente offline est légitime, il ne crée pas de ticket Sales automatiquement.
```

---

## 3. Concepts clés

### 3.1 Soumission offline

Une soumission offline est une vente locale créée sur le device, puis transmise au serveur plus tard.

Elle n’est pas encore un ticket officiel.

```text
OfflineSaleSubmission ≠ Ticket
```

### 3.2 Ticket Sales

Un Ticket Sales est une vente acceptée par le domaine Sales.

Il peut provenir :

- d’une vente online immédiate ;
- d’une soumission offline acceptée automatiquement ;
- d’une soumission offline acceptée par review admin.

```text
Ticket = vente officielle acceptée par Sales.
```

### 3.3 OfflineSalesGrant

Autorisation serveur préalable permettant à un vendeur/terminal de vendre offline dans une enveloppe limitée.

Le grant est signé par le serveur et contient :

- vendeur ;
- terminal ;
- outlet ;
- session capturée ;
- tirages autorisés ;
- limites offline ;
- fenêtre de validité ;
- snapshot pricing ;
- politique de risque ;
- signature.

### 3.4 OfflineCodeBatch

Lot de codes offline pré-réservés pour un vendeur/terminal/grant.

Ces codes permettent :

- d’imprimer un code client vérifiable ;
- de limiter le nombre de tickets offline ;
- de détecter les trous de séquence ;
- d’éviter les faux codes générés localement.

Règle :

```text
Code offline réservé ≠ ticket vendu.
Code offline accepté par Sales = publicCode officiel du Ticket.
```

### 3.5 TicketPlacedEvent

Event métier publié uniquement par Sales, uniquement pour les tickets acceptés.

Il ne doit jamais être publié pour une soumission offline brute, rejetée, suspecte ou en review.

---

## 4. Décision principale : séparation Offlinesync / Sales

### 4.1 `core.offlinesync`

`core.offlinesync` est responsable de la fiabilité de la synchronisation offline.

Il possède :

- `OfflineSalesGrant` ;
- `OfflineCodeBatch` ;
- `OfflineCodeReservation` ;
- `OfflineBatch` ;
- `OfflineSaleSubmission` ;
- `OfflineSyncAttempt` ;
- statuts techniques ;
- risques techniques ;
- payload brut ;
- signatures ;
- séquences ;
- décisions Sales enregistrées après traitement.

Il décide :

- signature valide ou invalide ;
- payload modifié ou non ;
- batch dupliqué ou non ;
- code offline réservé ou inconnu ;
- séquence cohérente ou suspecte ;
- grant connu, expiré, révoqué ou valide ;
- soumission prête ou non pour Sales.

Il ne décide pas :

- ticket vendu ;
- ticket payable ;
- ticket gagnant/perdant ;
- session cash ;
- cutoff métier ;
- pricing métier ;
- limites métier ;
- payout eligibility ;
- stats Sales.

### 4.2 `core.sales`

`core.sales` est la vérité métier des tickets.

Il possède :

- `Ticket` ;
- `TicketLine` ;
- `TicketMoneyBreakdown` ;
- `SaleOrigin` ;
- `TicketSyncStatus` ;
- `TicketSaleStatus` ;
- `TicketResultStatus` ;
- `TicketSettlementStatus` ;
- `OfflineSaleRef` ;
- `TicketPlacedEvent`.

Il décide :

- vente acceptée ou rejetée métier ;
- vrai ticket créé ou non ;
- cutoff respecté ;
- draw autorisé ;
- pricing/odds acceptés ;
- limites respectées ;
- session rattachable ;
- money breakdown valide ;
- payout eligibility.

Il ne contient pas :

- payload JSON brut offline ;
- signatures brutes ;
- journal technique de sync ;
- codes offline non utilisés ;
- soumissions rejetées ou suspectes.

---

## 5. Flow fonctionnel complet

### 5.1 Préparation online

```text
Flutter/POS online
  -> demande OfflineSalesGrant
  -> reçoit OfflineSalesGrant signé
  -> reçoit OfflineCodeBatch signé
  -> stocke localement grant + codes + snapshot
```

Le serveur vérifie avant émission du grant :

- tenant actif ;
- outlet autorisé à vendre ;
- terminal actif et autorisé offline ;
- vendeur actif et autorisé ;
- session de vente ouverte ;
- tirages disponibles ;
- pricing snapshot disponible ;
- limites offline tenant/vendeur/terminal.

### 5.2 Vente locale offline

```text
Flutter/POS offline
  -> sélectionne un code offline non utilisé
  -> crée une OfflineSaleSubmission locale
  -> signe/hash le payload
  -> imprime un reçu OFFLINE
  -> marque localStatus = LOCAL_PRINTED / SYNC_PENDING
```

Le reçu doit afficher clairement :

```text
TICKET OFFLINE
Validation finale après synchronisation
Non payable avant validation serveur
Code de vérification : O-HT-....
Heure device : ...
Dernière sync connue : ...
```

### 5.3 Synchronisation

```text
Flutter/POS online à nouveau
  -> POST /tenant/offline-sync/batches
  -> envoie batch + submissions + signatures + payload hashes
```

`offlinesync` :

```text
1. stocke OfflineBatch
2. stocke OfflineSaleSubmission
3. vérifie signature/hash/grant/code/séquence
4. marque READY_FOR_SALES si techniquement valide
5. rejette techniquement si invalide
6. déclenche Sales après commit
```

### 5.4 Traitement Sales

`sales` reçoit uniquement les submissions prêtes.

Pour chaque soumission :

```text
1. revalide draw/cutoff/result/pricing/odds/limits/session/money
2. décide ACCEPTED / REJECTED / REVIEW_REQUIRED / CONFLICT
3. crée un Ticket seulement si ACCEPTED
4. publie TicketPlacedEvent seulement si Ticket créé
5. retourne la décision à offlinesync
```

### 5.5 Retour au device

Le device reçoit :

```text
Acceptés : N
Rejetés : N
En review : N
Conflits : N
```

Chaque ticket local est mis à jour :

```text
SYNC_ACCEPTED
SYNC_REJECTED
SYNC_REVIEW_REQUIRED
SYNC_CONFLICT
```

---

## 6. Flow technique détaillé

### 6.1 IssueOfflineSalesGrantCommand

Commande côté `core.offlinesync` :

```java
public record IssueOfflineSalesGrantCommand(
    TerminalId terminalId
) implements Command<IssueOfflineSalesGrantResult> {}
```

Le tenant et le vendeur viennent du `TchRequestContext`, pas du payload client.

Le handler :

```text
1. lit le contexte tenant/user
2. vérifie terminal via core.terminal
3. vérifie outlet via core.outlet
4. vérifie session ouverte via core.session
5. demande à Sales/Draw/Pricing les données nécessaires au snapshot
6. applique la OfflineSellerPolicy
7. crée OfflineSalesGrant
8. crée OfflineCodeBatch
9. signe le grant et le code batch
10. retourne le package offline au device
```

### 6.2 ReceiveOfflineBatchCommand

```java
public record ReceiveOfflineBatchCommand(
    TerminalId terminalId,
    OfflineSalesGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String clientBatchId,
    List<OfflineSaleSubmissionInput> submissions
) implements Command<ReceiveOfflineBatchResult> {}
```

Le handler :

```text
1. crée OfflineBatch
2. stocke chaque OfflineSaleSubmission brute
3. vérifie les gates techniques
4. marque chaque submission :
   - TECHNICALLY_REJECTED
   - READY_FOR_SALES
   - DUPLICATE
5. publie OfflineBatchReadyForSalesEvent after commit si nécessaire
```

### 6.3 ProcessOfflineBatchWithSalesCommand

```java
public record ProcessOfflineBatchWithSalesCommand(
    OfflineBatchId batchId
) implements Command<ProcessOfflineBatchWithSalesResult> {}
```

Le handler :

```text
1. charge les submissions READY_FOR_SALES
2. construit SyncOfflineSalesCommand
3. appelle commandBus.execute(...)
4. enregistre les décisions retournées par Sales
```

### 6.4 SyncOfflineSalesCommand

Commande côté `core.sales` :

```java
public record SyncOfflineSalesCommand(
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSalesGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    List<OfflineTicketSaleInput> submissions
) implements Command<SyncOfflineSalesResult> {}
```

Le handler Sales :

```text
1. revalide les gates métier
2. crée Ticket pour ACCEPTED seulement
3. n’écrit rien dans sales.ticket pour REJECTED/REVIEW/CONFLICT
4. publie TicketPlacedEvent after commit pour ACCEPTED seulement
5. retourne une décision par submission
```

---

## 7. Les deux types de rejet

Il existe deux familles de rejet qui ne doivent pas être confondues.

---

### 7.1 Rejet par `limitpolicy` pendant la vente/sync

Ce rejet signifie :

```text
La soumission est techniquement valide, mais la règle de limite métier bloque la vente.
```

Exemples :

- montant par ticket trop élevé ;
- montant total offline trop élevé ;
- limite vendeur dépassée ;
- limite terminal dépassée ;
- limite outlet dépassée ;
- seuil de risque tenant dépassé.

Décision :

```text
Pas de Ticket Sales.
Pas de TicketPlacedEvent.
Submission reste dans offlinesync.
status = SALES_REJECTED.
reason = LIMIT_POLICY_BLOCKED.
```

Impact :

- ne compte pas dans stats Sales ;
- ne compte pas dans cash session ;
- peut compter dans offline risk stats ;
- peut réduire le score offline du vendeur.

---

### 7.2 Rejet après sync par gate offline Sales

Ce rejet signifie :

```text
La soumission ne peut pas devenir un ticket officiel parce qu’un gate offline a échoué.
```

Exemples :

- sync après résultat connu ;
- sync après tirage avec heure device non fiable ;
- cutoff dépassé ;
- draw absent du grant ;
- pricing mismatch ;
- duplicate code ;
- séquence locale déjà acceptée ;
- session non rattachable ;
- grant expiré/révoqué ;
- terminal bloqué ;
- code offline non réservé.

Décision :

```text
Pas de Ticket Sales.
Pas de TicketPlacedEvent.
Submission reste dans offlinesync.
status = SALES_REJECTED ou SALES_REVIEW_REQUIRED.
reason = DRAW_RESULTED / CUTOFF_PASSED / DEVICE_TIME_UNTRUSTED / etc.
```

Impact :

- ne compte pas dans stats Sales ;
- ne compte pas dans commission vendeur ;
- ne compte pas dans payout ;
- apparaît dans dashboard offline risk.

---

## 8. Gates techniques Offlinesync

Une submission doit passer ces gates avant d’être envoyée à Sales.

| Gate                    | Décision si échec                                                       |
| ----------------------- | ----------------------------------------------------------------------- |
| Grant connu             | `TECHNICALLY_REJECTED: UNKNOWN_GRANT`                                   |
| Grant non révoqué       | `TECHNICALLY_REJECTED: GRANT_REVOKED`                                   |
| Code batch connu        | `TECHNICALLY_REJECTED: UNKNOWN_CODE_BATCH`                              |
| Code offline réservé    | `TECHNICALLY_REJECTED: UNKNOWN_OFFLINE_CODE`                            |
| Signature batch valide  | `TECHNICALLY_REJECTED: INVALID_BATCH_SIGNATURE`                         |
| Signature ticket valide | `TECHNICALLY_REJECTED: INVALID_TICKET_SIGNATURE`                        |
| Payload hash valide     | `TECHNICALLY_REJECTED: PAYLOAD_HASH_MISMATCH`                           |
| Terminal cohérent       | `TECHNICALLY_REJECTED: TERMINAL_MISMATCH`                               |
| Seller cohérent         | `TECHNICALLY_REJECTED: SELLER_MISMATCH`                                 |
| Séquence non dupliquée  | `TECHNICALLY_REJECTED: DUPLICATE_SEQUENCE`                              |
| Code non déjà soumis    | `DUPLICATE` ou `TECHNICALLY_REJECTED`                                   |
| Local sequence monotone | `READY_FOR_SALES` avec risk flag ou `TECHNICALLY_REJECTED` selon policy |

---

## 9. Gates métier Sales

Une submission techniquement valide doit passer les gates métier.

| Gate                                    | Décision si échec                                             |
| --------------------------------------- | ------------------------------------------------------------- |
| Money breakdown valide                  | `SALES_REJECTED: MONEY_BREAKDOWN_INVALID`                     |
| `stakeAmount = sum(lines)`              | `SALES_REJECTED: STAKE_MISMATCH`                              |
| `totalAmount = stakeAmount + feeAmount` | `SALES_REJECTED: TOTAL_MISMATCH`                              |
| Draw existe                             | `SALES_REJECTED: DRAW_NOT_FOUND`                              |
| Draw dans grant                         | `SALES_REJECTED: DRAW_NOT_ALLOWED`                            |
| `createdAtDevice <= cutoffAt`           | `SALES_REJECTED: CUTOFF_PASSED`                               |
| Draw pas resulted                       | `SALES_REJECTED: DRAW_ALREADY_RESULTED`                       |
| Sync pas trop tardive                   | `SALES_REJECTED` ou `REVIEW_REQUIRED: SYNC_TOO_LATE`          |
| Pricing snapshot cohérent               | `SALES_REJECTED: PRICING_MISMATCH`                            |
| Odds snapshot cohérent                  | `SALES_REJECTED: ODDS_MISMATCH`                               |
| Offline limits respectées               | `SALES_REJECTED: LIMIT_POLICY_BLOCKED`                        |
| Session rattachable                     | `SALES_REJECTED` ou `REVIEW_REQUIRED: SESSION_NOT_ATTACHABLE` |
| Duplicate publicCode absent             | `SALES_REJECTED: DUPLICATE_PUBLIC_CODE`                       |

---

## 10. Règles temporelles anti-fraude

### 10.1 Device time

```text
createdAtDevice est une déclaration du device, pas une preuve.
```

### 10.2 Politique v0 recommandée

| Situation                                        | Décision                                       |
| ------------------------------------------------ | ---------------------------------------------- |
| Sync avant cutoff                                | Auto-accept possible si tous les gates passent |
| Sync après cutoff mais avant draw                | Review ou reject selon policy tenant           |
| Sync après scheduledAt mais avant result         | `REVIEW_REQUIRED`                              |
| Sync après result connu                          | `SALES_REJECTED: DRAW_ALREADY_RESULTED`        |
| createdAtDevice après cutoff                     | `SALES_REJECTED: CUTOFF_PASSED`                |
| createdAtDevice avant grant.issuedAt - tolerance | `REVIEW_REQUIRED: DEVICE_TIME_UNTRUSTED`       |
| createdAtDevice après grant.validUntil           | `SALES_REJECTED: GRANT_EXPIRED_AT_SALE_TIME`   |

### 10.3 Faille mobile/POS

Flutter mobile standard n’a pas d’horloge inviolable.

Correction :

- limite offline basse sur mobile ;
- grant court ;
- secure storage ;
- PIN local ;
- attestation si possible ;
- pas d’auto-accept après résultat connu.

POS dédié peut recevoir une politique plus souple seulement s’il dispose :

- d’une clé device protégée ;
- d’un stockage sécurisé ;
- d’un journal append-only ;
- d’une horloge ou monotonic counter plus fiable.

---

## 11. Gestion de session

### 11.1 Principe

La session capturée dans le grant sert de rattachement.

```text
OfflineSalesGrant.salesSessionId = session ouverte au moment de l’émission.
```

### 11.2 Session encore ouverte à la sync

```text
Ticket accepté
TicketPlacedEvent publié
core.session ajoute totalAmount au salesCashIn normal
```

### 11.3 Session fermée à la sync

La sync après fermeture n’est pas automatiquement rejetée.

Mais il ne faut jamais modifier silencieusement une caisse fermée.

Décision : créer un ajustement post-close.

```text
SalesSessionOfflineAdjustment
  sessionId
  ticketId
  amountCents
  createdAtDevice
  syncedAt
  reason = OFFLINE_ACCEPTED_AFTER_CLOSE
```

Les rapports doivent afficher :

```text
cash attendu à fermeture
+ ventes offline acceptées après fermeture
= cash attendu ajusté
```

### 11.4 Session finalisée/comptabilisée

Si la session est déjà finalisée comptablement :

```text
REVIEW_REQUIRED ou REJECTED selon policy.
Pas d’auto-adjust silencieux.
```

---

## 12. Money model Sales

Sales doit utiliser :

```text
stakeAmount = montant joué
feeAmount   = frais SMS/service/taxe
totalAmount = stakeAmount + feeAmount
```

TicketLine ne contient que la mise et les odds.

```text
potentialPayout = line.stake * oddsSnapshot
```

Impacts :

- payout basé sur stake/winning amount ;
- session cash basé sur totalAmount ;
- stats Sales peuvent exposer stake/fees/total séparément ;
- TicketPlacedEvent doit contenir `stakeAmountCents`, `feeAmountCents`, `totalAmountCents`.

---

## 13. TicketPlacedEvent

Event cible :

```java
public record TicketPlacedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TicketId ticketId,
    OutletId outletId,
    UserId sellerUserId,
    TerminalId terminalId,
    SalesSessionId sessionId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    long stakeAmountCents,
    long feeAmountCents,
    long totalAmountCents,
    CurrencyCode currency,
    SaleOrigin saleOrigin,
    TicketSyncStatus syncStatus,
    SalesSessionPostingMode sessionPostingMode,
    OfflineSaleRef offlineSaleRef,
    List<TicketPlacedLineEvent> lines
) implements DomainEvent {}
```

Règles :

```text
saleOrigin = ONLINE  -> syncStatus = NONE, offlineSaleRef = null
saleOrigin = OFFLINE -> syncStatus = ACCEPTED, offlineSaleRef required
```

`TicketPlacedEvent` est publié after commit uniquement si le ticket est créé.

---

## 14. Public verification

Le client doit pouvoir vérifier son code.

### 14.1 Avant sync

Si le code est réservé mais pas soumis :

```text
OFFLINE_CODE_RESERVED
Message : Code offline reconnu, en attente de synchronisation.
Non payable.
```

### 14.2 Après sync, accepté

```text
OFFLINE_ACCEPTED / OFFICIAL_TICKET_ACCEPTED
Message : Ticket validé par le serveur.
Payable seulement si gagnant et non payé.
```

### 14.3 Après rejet

```text
OFFLINE_REJECTED
Message : Ticket non validé par le serveur.
Non payable.
```

### 14.4 En review

```text
OFFLINE_REVIEW_REQUIRED
Message : Ticket en vérification.
Non payable pour le moment.
```

---

## 15. UX Flutter/POS

### 15.1 Écran offline

Afficher :

```text
Mode offline actif
Tickets créés hors ligne : N
Tickets non synchronisés : N
Limite restante : X
Dernière synchronisation : ...
Les tickets seront validés au retour réseau.
```

### 15.2 Après sync

Afficher :

```text
Acceptés : N
Rejetés : N
En review : N
Conflits : N
```

### 15.3 Message vendeur

```text
Les tickets offline imprimés ne comptent dans vos ventes officielles qu’après validation serveur.
Trop de ventes offline rejetées ou synchronisées tardivement peuvent réduire votre capacité offline.
```

---

## 16. Politique tenant : pénalité offline

Le tenant peut pénaliser un vendeur qui abuse du mode offline.

### 16.1 OfflineSellerRiskStatus

```java
public enum OfflineSellerRiskStatus {
  NORMAL,
  WATCHLIST,
  RESTRICTED_OFFLINE,
  OFFLINE_DISABLED,
  SUSPENDED
}
```

### 16.2 Métriques

```text
offlineSubmissionCount
offlineAcceptedCount
offlineRejectedCount
offlineReviewRequiredCount
syncAfterDrawCount
syncAfterResultCount
sequenceGapCount
invalidSignatureCount
limitPolicyBlockedCount
averageSyncDelay
```

### 16.3 Actions possibles

```text
warning
réduction des limites offline
réduction de durée grant
review admin obligatoire
blocage du mode offline
suspension terminal
suspension vendeur
```

### 16.4 Commission vendeur

```text
Commission = tickets acceptés par Sales uniquement.
Soumissions rejetées/review ne donnent aucune commission.
```

---

## 17. Implications inter-domaines

### 17.1 `core.terminal`

Responsable de :

- terminal actif ;
- terminal locké ou non ;
- terminal autorisé offline ;
- terminal key/device binding ;
- sync state ;
- lastSeen.

Ne décide pas si un ticket est vendu.

### 17.2 `core.outlet`

Responsable de :

- salesBlocked ;
- salesBlockReason ;
- timezone ;
- receipt config ;
- sales capability.

### 17.3 `core.session`

Responsable de :

- session ouverte/fermée ;
- cash summary ;
- post-close offline adjustments ;
- variance ajustée.

### 17.4 `core.limitpolicy`

Responsable de :

- limites online ;
- limites offline ;
- blocage par montant ;
- risk thresholds ;
- décision `LIMIT_POLICY_BLOCKED`.

### 17.5 `core.draw` / `core.drawresult`

Responsable de :

- draw schedule ;
- cutoffAt ;
- scheduledAt ;
- resultedAt ;
- statut du draw.

Sales demande ces faits pour décider.

### 17.6 `core.payout`

Règle :

```text
Payout interdit si ticket non accepté par Sales.
Payout interdit si syncStatus != ACCEPTED/NONE.
Payout interdit si ticket non resulted/winning/not paid.
```

### 17.7 `features.stats`

Deux familles de stats :

```text
Sales stats       -> sales.ticket uniquement
Offline risk stats -> offlinesync uniquement
```

### 17.8 `features.tenantadmin`

Doit exposer :

- dashboard offline risk ;
- vendeur watchlist ;
- taux de rejet ;
- sync tardive ;
- décisions admin review ;
- actions de pénalité.

---

## 18. API proposée

### 18.1 Offlinesync tenant APIs

```http
POST /tenant/offline-sync/grants
POST /tenant/offline-sync/batches
GET  /tenant/offline-sync/batches/{batchId}
GET  /tenant/offline-sync/submissions/{submissionId}
GET  /tenant/offline-sync/status
```

### 18.2 Sales APIs existantes

```http
POST  /tenant/tickets
GET   /tenant/tickets
GET   /tenant/tickets/{ticketId}
PATCH /tenant/tickets/{ticketId}/cancel
POST  /tenant/tickets/{ticketId}/approve
POST  /tenant/tickets/{ticketId}/reject
PATCH /tenant/tickets/{ticketId}/result/override
```

### 18.3 Review admin offline

```http
GET  /admin/offline-submissions/review
POST /admin/offline-submissions/{submissionId}/approve
POST /admin/offline-submissions/{submissionId}/reject
```

Admin approve :

```text
Sales crée Ticket
TicketPlacedEvent publié
Offlinesync submission -> SALES_ACCEPTED
```

Admin reject :

```text
Pas de Ticket
Offlinesync submission -> SALES_REJECTED
```

### 18.4 Public verification

```http
GET /public/tickets/verify?code=...
```

Peut résoudre :

- official ticket code ;
- offline reserved code ;
- offline submitted code ;
- rejected/review status.

---

## 19. Modèle de données proposé

### 19.1 `offlinesync.offline_sales_grant`

```text
id
tenant_id
outlet_id
terminal_id
seller_user_id
sales_session_id
issued_at
valid_until
max_ticket_count
max_ticket_amount
max_total_amount
allowed_draws_json
pricing_snapshot_hash
risk_policy_json
signature
status
revoked_at
created_at
updated_at
version
deleted_at
```

### 19.2 `offlinesync.offline_code_batch`

```text
id
tenant_id
outlet_id
terminal_id
seller_user_id
sales_session_id
grant_id
prefix
sequence_from
sequence_to
issued_at
valid_until
status
signature
created_at
updated_at
version
deleted_at
```

### 19.3 `offlinesync.offline_code_reservation`

```text
id
batch_id
code
local_sequence
status
submitted_at
sales_ticket_id null
created_at
updated_at
version
```

### 19.4 `offlinesync.offline_batch`

```text
id
tenant_id
terminal_id
grant_id
code_batch_id
client_batch_id
received_at
status
ticket_count
technical_reject_count
sales_accept_count
sales_reject_count
review_count
risk_flags_json
created_at
updated_at
version
deleted_at
```

### 19.5 `offlinesync.offline_sale_submission`

```text
id
tenant_id
batch_id
grant_id
code_batch_id
offline_code
terminal_id
outlet_id
seller_user_id
sales_session_id
client_ticket_id
local_sequence
created_at_device
received_at
payload_json
payload_hash
signature
status
technical_reject_reason
sales_decision
sales_reject_reason
sales_ticket_id null
processed_at
created_at
updated_at
version
deleted_at
```

### 19.6 `sales.ticket`

Uniquement pour tickets acceptés.

```text
id
tenant_id
outlet_id
terminal_id
seller_user_id
sales_session_id
draw_id
draw_channel_id
public_code
verification_code
stake_amount
fee_amount
total_amount
potential_payout_amount
winning_amount
sale_status
result_status
settlement_status
sale_origin
sync_status
offline_submission_id null
offline_batch_id null
offline_code_batch_id null
client_ticket_id null
local_sequence null
created_at_device null
synced_at null
created_at
updated_at
version
deleted_at
```

### 19.7 `session.sales_session_offline_adjustment`

```text
id
tenant_id
session_id
ticket_id
amount_cents
currency
created_at_device
synced_at
reason
created_at
updated_at
version
```

---

## 20. Statuts recommandés

### 20.1 Offlinesync submission status

```java
public enum OfflineSubmissionStatus {
  RECEIVED,
  DUPLICATE,
  TECHNICALLY_REJECTED,
  READY_FOR_SALES,
  SENT_TO_SALES,
  SALES_ACCEPTED,
  SALES_REJECTED,
  SALES_CONFLICT,
  SALES_REVIEW_REQUIRED
}
```

### 20.2 Sales decision

```java
public enum SalesOfflineDecision {
  ACCEPTED,
  ACCEPTED_POST_CLOSE_ADJUSTMENT,
  REJECTED,
  CONFLICT,
  REVIEW_REQUIRED
}
```

### 20.3 Sale origin

```java
public enum SaleOrigin {
  ONLINE,
  OFFLINE
}
```

### 20.4 Ticket sync status

```java
public enum TicketSyncStatus {
  NONE,
  ACCEPTED
}
```

Note : Sales ne stocke pas les rejetés/review dans `ticket`, donc `TicketSyncStatus` n’a pas besoin de porter `REJECTED` ou `REVIEW_REQUIRED` sur les tickets officiels v0.

---

## 21. Sécurité mobile/POS

### 21.1 Mobile Flutter standard

Risques :

- clock spoofing ;
- app reinstall ;
- local DB wipe ;
- root/jailbreak ;
- PIN partagé ;
- stockage compromis.

Corrections :

- secure storage pour clés ;
- base locale chiffrée ;
- signature payload ;
- OfflineCodeBatch borné ;
- localSequence append-only ;
- limites basses ;
- grant court ;
- sync après résultat rejetée ;
- sync après tirage review.

### 21.2 POS dédié

Risque plus faible si :

- device key protégée ;
- stockage sécurisé ;
- imprimante intégrée ;
- app verrouillée ;
- journal local append-only ;
- séquence monotone persistante.

Corrections :

- politiques offline plus élevées possibles ;
- mais toujours pas d’auto-accept après résultat connu sans preuve forte.

---

## 22. Règles UX prioritaires

L’expérience utilisateur doit rester claire :

```text
Le vendeur peut vendre offline.
Le client reçoit une preuve offline avec un code.
Le code peut être vérifié.
Mais le ticket n’est officiel qu’après validation serveur.
```

Messages obligatoires :

- côté vendeur : “Ces ventes ne comptent dans vos statistiques qu’après validation serveur.”
- côté client : “Ce ticket offline n’est pas payable avant validation.”
- côté tenant admin : “Les ventes offline rejetées peuvent réduire la capacité offline du vendeur.”

---

## 23. Règles d’architecture

### 23.1 Pas de ticket Sales avant acceptation

```text
OfflineSaleSubmission n’est pas Ticket.
Ticket existe seulement après ACCEPTED.
```

### 23.2 Pas de stats Sales depuis offlinesync

```text
Sales stats = sales.ticket uniquement.
Offline risk stats = offlinesync uniquement.
```

### 23.3 Pas de payout offline

```text
Payout nécessite un ticket Sales accepté, resulted, winning, not paid.
```

### 23.4 Events after commit

```text
TicketPlacedEvent publié uniquement after commit.
Offlinesync events ne déclenchent pas session cash/ledger/stats Sales.
```

### 23.5 Typed IDs

Tous les IDs domain/application doivent être typés :

```text
OfflineSalesGrantId
OfflineBatchId
OfflineSaleSubmissionId
OfflineCodeBatchId
OfflineCodeReservationId
TicketId
TerminalId
SalesSessionId
```

Pas de `UUID` brut hors persistence.

---

## 24. Décisions finales par challenge

| Challenge                    | Décision                              | Faille possible                     | Correction                                       |
| ---------------------------- | ------------------------------------- | ----------------------------------- | ------------------------------------------------ |
| Ventes offline non acceptées | Restent dans offlinesync              | Polluent stats si créées dans Sales | Pas de `sales.ticket` avant acceptation          |
| Heure device trafiquée       | `createdAtDevice` non fiable seul     | Vente après résultat antidatée      | Reject/review selon sync time et result          |
| Offline plusieurs jours      | Grant signé limité                    | Résultats connus avant sync         | Pas d’auto-accept après résultat                 |
| Code client offline          | OfflineCodeBatch pré-réservé          | Faux codes locaux                   | Codes signés et séquences serveur                |
| Session fermée               | Post-close adjustment                 | Caisse modifiée silencieusement     | Ajustement visible séparé                        |
| Payout offline               | Interdit                              | Double payout/faux ticket           | Payout seulement sur ticket accepté              |
| Mobile Flutter               | Trust faible                          | Clock/root/storage wipe             | Limites basses, secure storage, gates stricts    |
| POS                          | Trust moyen                           | Journal modifiable                  | Device key, append-only, attestation si possible |
| Reset app                    | Séquence serveur                      | Compteurs remis à zéro              | CodeBatch + last sequence serveur                |
| Sync partielle               | Séquence obligatoire                  | Cacher tickets gagnants             | Détecter gaps, review/reject                     |
| Limites offline              | Plus strictes que online              | Vente excessive                     | LimitPolicy block + risk score                   |
| Review admin                 | Pas de ticket Sales avant acceptation | Stats polluées                      | Review reste dans offlinesync                    |
| Reçu client                  | Marqué OFFLINE                        | Client croit ticket garanti         | Mention non payable avant validation             |

---

## 25. Décision officielle à retenir

```text
Le mode offline est autorisé comme mécanisme de continuité de vente, mais chaque vente offline reste provisoire jusqu’à validation serveur.

Offlinesync conserve toutes les preuves, y compris les soumissions rejetées, suspectes ou en review.
Sales ne crée un vrai ticket que pour les soumissions acceptées.

Les stats Sales, la session cash, le ledger, les commissions et le payout ne tiennent compte que des tickets acceptés par Sales.

Les soumissions rejetées après sync, y compris les rejets par limitpolicy ou par gate offline après tirage/cutoff, restent dans offlinesync et alimentent uniquement les dashboards de risque offline.
```

---

## 26. Prochaines étapes d’implémentation

1. Créer les typed IDs offlinesync.
2. Créer les enums offlinesync.
3. Créer `OfflineSalesGrant`, `OfflineCodeBatch`, `OfflineSaleSubmission`.
4. Créer tables offlinesync + `_AUD` si audit Envers.
5. Modifier `sales.ticket` pour `stakeAmount / feeAmount / totalAmount` et offline refs.
6. Modifier `TicketPlacedEvent` avec money breakdown complet.
7. Implémenter IssueOfflineSalesGrant.
8. Implémenter ReceiveOfflineBatch.
9. Implémenter SyncOfflineSalesCommand dans Sales.
10. Implémenter review admin approve/reject submission.
11. Adapter session avec `SalesSessionOfflineAdjustment`.
12. Adapter public verify pour codes offline.
13. Adapter Flutter/POS local DB + sync queue.
14. Créer dashboards offline risk dans tenant admin.
