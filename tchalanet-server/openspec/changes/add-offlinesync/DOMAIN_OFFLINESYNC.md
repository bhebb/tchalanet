# DOMAIN_OFFLINESYNC

> Document de conception du module `core.offlinesync`.
> **Version 2.0** — intègre les retours de revue post-v1.
> Format bilingue : prose en français, identifiants/code/API en anglais.

> **Changements clés depuis v1 :**
> - Crypto : passage à Ed25519 côté device (clé privée dans Android Keystore), serveur ne stocke plus de secret signant
> - Cycle de vie des codes : 6 états explicites, plus de retour à `AVAILABLE` après soumission
> - Statuts Grant : ajout `SUPERSEDED` pour le renouvellement (séparé de `CONSUMED` pour quotas atteints)
> - Fenêtres de validité : séparation `validUntil` (création vente) vs `syncAcceptedUntil` (réception serveur, 7 jours par défaut)
> - Idempotence stricte : `batchPayloadHash` et `payloadHash` côté serveur, conflits détectés
> - Promotion idempotente : `promotionAttemptId`, contraintes UNIQUE côté sales, listeners strictement idempotents
> - `DUPLICATE` est un résultat API, plus un état persistant
> - Submission : ajout `totalStakeAmount`, `lineCount` dénormalisés
> - Invariants de cohérence multi-status formalisés
> - Statuts locaux Flutter enrichis (9 états)
> - Events self-contained (sales ne fait pas de query retour)

---

## 1. Vision et frontières

### 1.1 Raison d'être

Le module `core.offlinesync` permet à un terminal POS de continuer à vendre lorsqu'il est temporairement coupé du backend, puis de réconcilier ces ventes avec le système central dès le retour de la connectivité.

Il n'est **pas** une copie hors-ligne du module `core.sales`. C'est un **sas de sécurité** qui :

- accorde et révoque des autorisations temporaires de vente offline (`Grant`)
- distribue des codes uniques pré-alloués pour matérialiser les ventes offline (`OfflineCode`)
- reçoit, valide techniquement et trace les soumissions de vente (`OfflineSubmission`)
- orchestre leur promotion vers le module `core.sales` via des events idempotents
- fournit aux administrateurs les outils de visibilité et de résolution

### 1.2 Frontières — ce que le module fait

- Émettre, renouveler, révoquer des `OfflineGrant`
- Allouer des batches de codes offline signés et uniques
- Recevoir des batches de submissions provenant des devices
- Valider techniquement (signature Ed25519, intégrité, idempotence, code valide, grant actif, fenêtres temporelles)
- Publier des events de demande de promotion vers `sales` (self-contained)
- Recevoir les events de retour de `sales` et mettre à jour l'état (strictement idempotent)
- Exposer les submissions et leurs lots aux administrateurs
- Permettre l'approbation/rejet manuel des cas en `NEEDS_REVIEW`

### 1.3 Frontières — ce que le module ne fait PAS

- Ne crée pas directement de ticket dans les tables de `core.sales`
- Ne valide pas le pricing métier (lignes, jeux, montants finaux)
- Ne gère pas le cutoff des tirages (responsabilité `core.draw`)
- Ne calcule pas les limites métier (responsabilité `core.limitpolicy`)
- Ne fait pas d'analyse de risque/fraude en v1 (futur `core.risk`)
- Ne gère pas les remboursements (manuel hors système en v1)
- Ne valide pas le contexte POS lui-même (responsabilité `core.session`)

### 1.4 Frontières modulith — règle reformulée

**Règle d'isolation :**
- Aucun accès cross-module aux packages `internal` ni aux tables
- Appels synchrones autorisés via `api.command` / `api.query` publiques
- Workflows asynchrones via `api.event`

Le module `offlinesync` consomme :
- `core.session.ResolvePosOperationContextQuery` (synchrone, API publique)
- `core.limitpolicy.GetOfflineLimitPolicyQuery` (synchrone, API publique)

Le module `core.sales` consomme les events d'`offlinesync` et **ne fait aucune query retour** vers `offlinesync` — les events sont self-contained.

### 1.5 Modules dépendants

| Module | Direction | Type | Raison |
|---|---|---|---|
| `core.session` | offlinesync → session | Query sync | Valider le contexte POS lors de la demande de grant |
| `core.sales` | offlinesync ↔ sales | Events | Promotion des submissions en tickets réels |
| `core.limitpolicy` | offlinesync → limitpolicy | Query sync | Lire les limites offline applicables |
| `core.draw` | sales → draw | (transitif) | Indirect, sales valide les cutoffs |
| `platform.audit` | offlinesync → audit | Envers | Audit transverse |
| `platform.communication` | offlinesync → communication | Event/RPC | Alertes admin sur incidents |

---

## 2. Glossaire métier

| Terme | Définition |
|---|---|
| **Grant** | Autorisation temporaire signée donnée à un quadruplet `(seller, terminal, device)` pour vendre offline pendant une période, avec des limites quantitatives. |
| **OfflineCodeBatch** | Lot de codes offline pré-alloués par le serveur, signés, distribués au device en même temps qu'un Grant. |
| **OfflineCode** | Code court, unique, humain-lisible, qui apparaît sur le ticket papier offline et permet l'identification ultérieure de la vente. |
| **OfflineSyncBatch** | Lot de submissions envoyé par un device lors d'un appel de synchronisation. Toutes ses submissions partagent le même contexte (grant, code batch, seller, terminal, outlet, device). |
| **OfflineSubmission** | Représentation côté serveur d'une vente offline soumise par un device. Avant d'être promue en ticket. |
| **OfflineSubmissionLine** | Détail d'une ligne de jeu d'une submission (mise, type de pari, sélection, payout indicatif). |
| **OfflineSubmissionTicketLink** | Lien entre une submission promue et le ticket réel créé dans `core.sales`. |
| **OfflineSubmissionDecision** | Trace d'une décision manuelle d'admin (approve/reject/replay) sur une submission. |
| **Promotion** | Action de transformer une submission validée en un vrai ticket dans `core.sales`. |
| **PromotionAttempt** | Une tentative de promotion identifiée par `promotionAttemptId` (UUID). Permet d'identifier et de corréler les events aller/retour. |
| **TechnicalStatus** | Résultat de la validation purement technique côté offlinesync. |
| **BusinessStatus** | Résultat de la validation métier côté sales. |
| **clientSubmissionId** | UUID généré par le device, sert de clé d'idempotence niveau submission. |
| **clientBatchId** | UUID généré par le device, identifie un batch de sync, sert d'idempotence batch. |
| **payloadHash** | SHA-256 du payload sérialisé en JSON canonique. Sert à détecter altération et conflits d'idempotence. |
| **batchPayloadHash** | SHA-256 du payload de batch entier (clientBatchId + submissions ordonnées). |
| **validUntil** | Limite de **création** d'une vente offline. Au-delà, le device ne peut plus produire de nouvelles submissions. |
| **syncAcceptedUntil** | Limite de **réception serveur** d'une submission. Au-delà, même une vente faite pendant la validité du grant est refusée à la sync. |
| **TrustLevel** | (Roadmap, post-v1) Niveau de confiance d'un seller/terminal, influence les paramètres du Grant. |

---

## 3. Modèle de domaine

### 3.1 Vue d'ensemble

```
Tenant
  └─ OfflineGrant (1..N)
       ├─ OfflineCodeBatch (1..1)
       │    └─ OfflineCode (N) — pré-alloués
       └─ OfflineSubmission (0..N)
            ├─ OfflineSubmissionLine (1..N)
            ├─ OfflineSubmissionTicketLink (0..1) — vers Ticket
            └─ OfflineSubmissionDecision (0..N) — actions admin

OfflineSyncBatch
  └─ OfflineSubmission (1..N)
     (toutes les submissions d'un batch partagent grantId, codeBatchId,
      sellerUserId, terminalId, outletId, deviceId)
```

### 3.2 Entités

#### `OfflineGrant`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant du grant |
| `tenantId` | UUID | Tenant propriétaire |
| `sellerUserId` | UUID | Utilisateur vendeur |
| `terminalId` | UUID | Terminal POS |
| `outletId` | UUID | Point de vente |
| `deviceId` | String | Identifiant unique du device physique |
| `codeBatchId` | UUID | Batch de codes associé |
| `devicePublicKey` | String (Ed25519) | **Clé publique** du device, fournie par le device à la demande de grant. Le serveur ne stocke jamais de secret signant. |
| `keyId` | String | Identifiant de la clé device (pour rotation future) |
| `grantSignature` | String | Signature serveur (Ed25519) du payload de grant, vérifiable par le device |
| `serverKeyId` | String | Identifiant de la clé serveur utilisée pour signer le grant |
| `schemaVersion` | Integer | Version du schéma de payload (anti-divergence client/serveur) |
| `status` | Enum | ACTIVE, EXPIRED, REVOKED, CONSUMED, SUPERSEDED |
| `validFrom` | Instant | Début de validité (création de ventes) |
| `validUntil` | Instant | Fin de validité (création de ventes) |
| `syncAcceptedUntil` | Instant | Limite de réception serveur (typiquement validUntil + 7 jours) |
| `maxTicketCount` | Integer | Quota de tickets autorisés |
| `maxTotalAmount` | BigDecimal | Quota de montant cumulé |
| `consumedTicketCount` | Integer | Compteur tickets (mis à jour à chaque submission TECH_VALIDATED) |
| `consumedTotalAmount` | BigDecimal | Compteur montant |
| `issuedAt` | Instant | Date d'émission |
| `revokedAt` | Instant (nullable) | Date de révocation |
| `revokedReason` | String (nullable) | Motif |
| `revokedBy` | UUID (nullable) | Auteur |
| `supersededAt` | Instant (nullable) | Date de remplacement par renouvellement |
| `supersededByGrantId` | UUID (nullable) | Nouveau grant qui remplace |

**Invariants :**
- `validFrom < validUntil < syncAcceptedUntil`
- `maxTicketCount > 0`, `maxTotalAmount > 0`
- `0 <= consumedTicketCount <= maxTicketCount`
- `0 <= consumedTotalAmount <= maxTotalAmount`
- Si `status == REVOKED` alors `revokedAt != null` et `revokedReason != null`
- Si `status == SUPERSEDED` alors `supersededAt != null` et `supersededByGrantId != null`
- Un seul Grant `ACTIVE` par `(tenantId, sellerUserId, terminalId, deviceId)` à un instant donné (contrainte UNIQUE PARTIAL)
- `devicePublicKey` non nul à la création

**Sémantique des statuts terminaux :**
- `EXPIRED` : `now > validUntil`, pas de quotas atteints, pas de remplacement
- `REVOKED` : action admin ou policy de sécurité
- `CONSUMED` : quotas atteints (`consumedTicketCount == maxTicketCount` OU `consumedTotalAmount == maxTotalAmount`)
- `SUPERSEDED` : remplacé par un nouveau grant lors d'un renouvellement avant expiration naturelle

#### `OfflineCodeBatch`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant du batch |
| `tenantId` | UUID | Tenant |
| `grantId` | UUID (FK) | Grant qui possède ce batch |
| `terminalId` | UUID | Terminal cible |
| `outletId` | UUID | Outlet |
| `sellerUserId` | UUID | Seller |
| `allocatedCount` | Integer | Nombre de codes alloués |
| `consumedCount` | Integer | Codes consommés (CONSUMED_PROMOTED + CONSUMED_REJECTED) |
| `voidedCount` | Integer | Codes VOIDED |
| `status` | Enum | ACTIVE, EXPIRED, CONSUMED, REVOKED |
| `issuedAt` | Instant | Date d'émission |
| `expiresAt` | Instant | Date d'expiration (alignée sur `Grant.syncAcceptedUntil`) |

**Invariants :**
- `0 <= consumedCount + voidedCount <= allocatedCount`
- `expiresAt > issuedAt`
- Si `consumedCount + voidedCount == allocatedCount`, alors `status != ACTIVE`

#### `OfflineCode`

| Champ | Type | Description |
|---|---|---|
| `code` | String (PK) | Le code lui-même, ex: `A7K-3FH-92Q` |
| `tenantId` | UUID | Tenant |
| `batchId` | UUID (FK) | Batch parent |
| `status` | Enum | **6 états explicites** (voir machine à états section 4) |
| `reservedForSubmissionId` | UUID (nullable) | Submission qui a tenté de consommer ce code |
| `reservedAt` | Instant (nullable) | Date de réservation côté serveur |
| `consumedAt` | Instant (nullable) | Date effective de consommation |
| `expiresAt` | Instant | Date d'expiration (héritée du batch) |

**Format du code :**
- 9 caractères en groupes 3-3-3 : `XXX-XXX-XXX`
- Alphabet anti-confusion : 27 caractères (lettres + chiffres, hors `O`, `0`, `I`, `1`, `L`)
- Espace ≈ 7.6 × 10^12

**Invariants :**
- Unicité globale du code (clé primaire)
- Si `status ∈ {CONSUMED_PROMOTED, CONSUMED_REJECTED}` alors `reservedForSubmissionId != null` et `consumedAt != null`
- **Un code soumis au serveur (ayant été lié à une submission) ne redevient jamais `AVAILABLE`**
- Transitions valides documentées en section 4.3

#### `OfflineSyncBatch`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant interne |
| `tenantId` | UUID | Tenant |
| `clientBatchId` | UUID | UUID généré par le device |
| `batchPayloadHash` | String | SHA-256 du payload de batch |
| `terminalId` | UUID | Terminal source |
| `outletId` | UUID | Outlet |
| `sellerUserId` | UUID | Seller |
| `deviceId` | String | Device |
| `grantId` | UUID (FK) | Grant utilisé (unique pour tout le batch) |
| `codeBatchId` | UUID (FK) | Code batch utilisé (unique pour tout le batch) |
| `salesSessionId` | UUID (nullable) | Résolu lors de la promotion |
| `receivedAt` | Instant | Date de réception |
| `processedAt` | Instant (nullable) | Date de fin de traitement |
| `status` | Enum | RECEIVED, PROCESSING, COMPLETED, PARTIAL, FAILED |
| `submissionCount` | Integer | Nombre total de submissions |
| `technicalRejectCount` | Integer | Compteur |
| `salesAcceptCount` | Integer | Compteur |
| `salesRejectCount` | Integer | Compteur |
| `reviewCount` | Integer | Compteur |
| `duplicateCount` | Integer | Compteur (résultats DUPLICATE) |
| `rawManifest` | TEXT (@NotAudited) | JSON brut, pour debug |

**Invariants :**
- Unicité de `(tenantId, clientBatchId)` — protège contre les double-sync
- **Toutes les submissions du batch partagent le même `grantId`, `codeBatchId`, `sellerUserId`, `terminalId`, `outletId`, `deviceId`** — invariant fort qui simplifie le traitement et l'audit
- `sum(*Count) <= submissionCount`
- Si `status == COMPLETED` alors `sum(technicalReject + salesAccept + salesReject + duplicate) == submissionCount` ET `reviewCount == 0`

#### `OfflineSubmission`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant interne |
| `tenantId` | UUID | Tenant |
| `syncBatchId` | UUID (FK) | Batch parent |
| `grantId` | UUID (FK) | Grant utilisé |
| `codeBatchId` | UUID (FK) | Code batch utilisé |
| `offlineCode` | String (FK) | Code offline consommé |
| `clientSubmissionId` | UUID | UUID device (idempotence) |
| `payloadHash` | String | SHA-256 du payload canonique |
| `signature` | String | Signature Ed25519 du device |
| `signatureAlgorithm` | String | Ex: "Ed25519" (anti-confusion algorithmique) |
| `canonicalizationVersion` | Integer | Version du schéma de canonicalisation |
| `keyId` | String | Identifiant de la clé device |
| `schemaVersion` | Integer | Version du schéma de payload |
| `deviceId` | String | Device source |
| `sellerUserId` | UUID | Seller |
| `terminalId` | UUID | Terminal |
| `outletId` | UUID | Outlet |
| `salesSessionId` | UUID (nullable) | Résolu lors de la promotion |
| `clientSoldAt` | Instant | Date de vente déclarée par le device |
| `receivedAt` | Instant | Date de réception serveur |
| `processedAt` | Instant (nullable) | Date de fin de traitement |
| `status` | Enum | RECEIVED, TECH_REJECTED, TECH_VALIDATED, BUSINESS_REJECTED, NEEDS_REVIEW, PROMOTED_TO_TICKET |
| `technicalStatus` | Enum | PENDING, VALIDATED, REJECTED |
| `businessStatus` | Enum (nullable) | PENDING, ACCEPTED, REJECTED, NEEDS_REVIEW |
| `rejectionCode` | String (nullable) | Code d'erreur normalisé |
| `rejectionReason` | String (nullable) | Détail humain |
| `totalStakeAmount` | BigDecimal | **Dénormalisé**, somme des stakes des lignes |
| `lineCount` | Integer | **Dénormalisé**, nombre de lignes |
| `rawPayload` | TEXT (@NotAudited) | JSON original, pour audit |
| `createdTicketId` | UUID (nullable) | Dénormalisation, vérité dans `OfflineSubmissionTicketLink` |
| `promotionAttemptId` | UUID (nullable) | Identifiant de la dernière tentative de promotion |
| `promotionRequestedAt` | Instant (nullable) | Date de l'event aller |
| `lastPromotionEventId` | UUID (nullable) | Identifiant du dernier event retour traité |

**Invariants de cohérence multi-status :**

```
status == RECEIVED              => technicalStatus == PENDING
                                   businessStatus == null
                                   processedAt == null

status == TECH_REJECTED         => technicalStatus == REJECTED
                                   businessStatus == null
                                   rejectionCode != null

status == TECH_VALIDATED        => technicalStatus == VALIDATED
                                   businessStatus IN (PENDING, NEEDS_REVIEW)
                                   promotionAttemptId != null

status == BUSINESS_REJECTED     => technicalStatus == VALIDATED
                                   businessStatus == REJECTED
                                   rejectionCode != null

status == NEEDS_REVIEW          => technicalStatus == VALIDATED
                                   businessStatus == NEEDS_REVIEW

status == PROMOTED_TO_TICKET    => technicalStatus == VALIDATED
                                   businessStatus == ACCEPTED
                                   createdTicketId != null
                                   ∃ OfflineSubmissionTicketLink (linkType=CREATED)
```

**Autres invariants :**
- Unicité de `(tenantId, clientSubmissionId)`
- Unicité de `(tenantId, offlineCode)` — un code ne peut être lié qu'à une submission
- `totalStakeAmount == sum(lines.stakeAmount)`
- `lineCount == count(lines)`
- `payloadHash` recalculable depuis `rawPayload`
- Une seule `OfflineSubmissionTicketLink` avec `linkType == CREATED` par submission (toute violation = incident critique)

#### `OfflineSubmissionLine`

| Champ | Type | Description |
|---|---|---|
| `id` | Long (PK, auto) | Identifiant |
| `tenantId` | UUID | Tenant |
| `submissionId` | UUID (FK) | Submission parente |
| `lineNo` | Integer | Numéro de ligne |
| `gameCode` | String | Code du jeu |
| `betType` | String | Type de pari |
| `selectionKey` | String | Sélection |
| `stakeAmount` | BigDecimal | Mise |
| `potentialPayout` | BigDecimal | Gain potentiel **indicatif côté offline** (recalculable par sales) |
| `status` | Enum | PENDING, ACCEPTED, REJECTED |
| `rejectionCode` | String (nullable) | Si rejetée |
| `rejectionReason` | String (nullable) | Détail |

**Invariants :**
- `lineNo` unique par submission, commence à 1, sans trou
- `stakeAmount > 0`
- `potentialPayout >= 0` (assoupli par rapport à v1)

#### `OfflineSubmissionTicketLink`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant |
| `tenantId` | UUID | Tenant |
| `submissionId` | UUID (FK) | Submission |
| `ticketId` | UUID | Ticket dans `core.sales` |
| `linkType` | Enum | CREATED, DUPLICATE_OF, REPLACED_BY, VOIDED_BY_SYNC |
| `linkedAt` | Instant | Date |
| `linkedBy` | UUID (nullable) | Auteur (système ou admin) |

**Invariants :**
- Un seul `linkType == CREATED` par submission
- Pour annuler un ticket : ajouter un nouveau lien `VOIDED_BY_SYNC`, ne pas supprimer

#### `OfflineSubmissionDecision`

| Champ | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Identifiant |
| `tenantId` | UUID | Tenant |
| `submissionId` | UUID (FK) | Submission concernée |
| `decidedBy` | UUID | User admin |
| `decision` | Enum | APPROVE, REJECT, REPLAY, VOID |
| `dryRun` | Boolean | True pour replay dry-run |
| `reason` | String | Motif obligatoire (non vide) |
| `previousStatus` | Enum | État avant décision |
| `newStatus` | Enum | État après décision (== previousStatus si dryRun) |
| `reportJson` | TEXT (nullable) | Rapport détaillé pour replay dry-run |
| `decidedAt` | Instant | Date |

**Invariants :**
- `reason` non vide
- `previousStatus != newStatus` SAUF si `decision == REPLAY` ET `dryRun == true`
- Si `dryRun == true` alors `decision == REPLAY` et aucune autre écriture métier que cette ligne

---

## 4. Machines à états

### 4.1 OfflineGrantStatus

```
                  ┌─────────┐
   issued ─────▶  │ ACTIVE  │
                  └────┬────┘
                       │
        ┌──────────────┼──────────────┬───────────────┬────────────┐
        ▼              ▼              ▼               ▼            ▼
   ┌─────────┐  ┌────────────┐  ┌──────────┐   ┌──────────┐  ┌────────────┐
   │ EXPIRED │  │  REVOKED   │  │ CONSUMED │   │ SUPERSEDED│ │           │
   └─────────┘  └────────────┘  └──────────┘   └──────────┘  │           │
   now>validUntil admin action   quotas         renouvelé
                  ou policy      atteints       avant exp
```

**Transitions autorisées :**
- `ACTIVE → EXPIRED` : automatique quand `now > validUntil`
- `ACTIVE → REVOKED` : action admin ou policy
- `ACTIVE → CONSUMED` : quotas atteints
- `ACTIVE → SUPERSEDED` : renouvellement avec émission d'un nouveau grant

**États terminaux :** EXPIRED, REVOKED, CONSUMED, SUPERSEDED (aucune transition sortante).

**Important :** un grant `SUPERSEDED` continue à pouvoir recevoir des submissions au sync **si** `clientSoldAt` était dans sa fenêtre de validité originale, et **si** `receivedAt <= syncAcceptedUntil`. Cela permet aux ventes faites avec l'ancien grant d'être réconciliées même après un renouvellement.

### 4.2 OfflineCodeBatchStatus

```
ACTIVE → CONSUMED  (allocatedCount == consumedCount + voidedCount)
ACTIVE → EXPIRED   (now > expiresAt)
ACTIVE → REVOKED   (admin, via révocation du grant)
```

### 4.3 OfflineCodeStatus

**6 états :**

```
              ┌──────────┐
  allocated→  │ AVAILABLE│
              └─────┬────┘
                    │
              ┌─────┴────┐
              │          │
              ▼          ▼
       ┌──────────┐  ┌──────────┐
       │ RESERVED │  │ EXPIRED  │
       └────┬─────┘  └──────────┘
            │        batch expiré
     ┌──────┴──────┐    sans usage
     │             │
     ▼             ▼
┌───────────────┐ ┌───────────────┐
│CONSUMED_      │ │CONSUMED_      │
│PROMOTED       │ │REJECTED       │
└───────────────┘ └───────────────┘
TECH_VALIDATED   TECH_REJECTED
+ BUSINESS_ACCEPTED  ou BUSINESS_REJECTED

                  ┌──────────┐
   grant révoqué→ │  VOIDED  │
   avant usage    └──────────┘
   (depuis AVAILABLE)
```

**Transitions autorisées :**
- `AVAILABLE → RESERVED` : au début du traitement d'une submission contenant ce code
- `AVAILABLE → EXPIRED` : batch expiré sans utilisation
- `AVAILABLE → VOIDED` : grant révoqué avant utilisation
- `RESERVED → CONSUMED_PROMOTED` : submission TECH_VALIDATED et BUSINESS_ACCEPTED
- `RESERVED → CONSUMED_REJECTED` : submission TECH_REJECTED ou BUSINESS_REJECTED

**Transitions interdites :**
- `RESERVED → AVAILABLE` (le code ne revient JAMAIS disponible après soumission)
- `CONSUMED_* → *` (états terminaux)
- `EXPIRED → *`
- `VOIDED → *`

**Cas pratique d'un crash serveur pendant la validation** : si le serveur crash entre `RESERVED` et `CONSUMED_*`, le code reste en `RESERVED`. Au redémarrage, le job de récupération inspecte les codes `RESERVED` orphelins (sans submission associée traitée) et les passe en `CONSUMED_REJECTED` avec une raison "ORPHANED_RESERVATION". Si la submission existe et est valide, on poursuit le traitement normal.

### 4.4 OfflineSubmissionStatus

**Note importante :** `DUPLICATE` n'est plus un état persistant. C'est un résultat API retourné quand une submission avec un `clientSubmissionId` déjà connu arrive. Aucune ligne n'est créée pour le duplicate.

```
                    RECEIVED
                       │
              ┌────────┴────────┐
              │                 │
              ▼                 ▼
       TECH_REJECTED      TECH_VALIDATED
       (terminal)              │
                               │ event OfflineSubmissionTechValidatedEvent
                               ▼
                       (sales traite, publie ProcessedEvent)
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       BUSINESS_REJECTED  PROMOTED_TO_TICKET  NEEDS_REVIEW
       (terminal sauf      (terminal)              │
        replay admin)                              │ décision admin
                                                   │
                                  ┌────────────────┴────────────────┐
                                  ▼                                 ▼
                          (APPROVE: event vers sales)        BUSINESS_REJECTED
                          puis PROMOTED_TO_TICKET             (terminal)
                          ou retour NEEDS_REVIEW
                          si nouvel échec
```

**États terminaux :** TECH_REJECTED, BUSINESS_REJECTED, PROMOTED_TO_TICKET

### 4.5 OfflineSyncBatchStatus

```
RECEIVED → PROCESSING → COMPLETED  (toutes submissions en état terminal)
                     → PARTIAL     (au moins une en NEEDS_REVIEW)
                     → FAILED      (erreur système)
```

---

## 5. Use cases principaux

### 5.1 UC-01 : Demander un Grant offline

**Acteur :** Vendeur via app POS (Android)
**Précondition :** Online, session POS ouverte, device a généré sa keypair Ed25519
**Déclencheur :** L'app a besoin d'un grant

**Flow nominal :**

1. Si pas encore fait, l'app génère localement une **paire de clés Ed25519** dans Android Keystore (`devicePrivateKey` non exfiltrable, `devicePublicKey` exportable)
2. App POS envoie `POST /tenant/offline/grants` avec `devicePublicKey`, `keyId`, contexte (terminalId, deviceId)
3. `RequestOfflineGrantCommandHandler` lit l'acteur depuis `TchRequestContext`
4. Appelle `core.session.ResolvePosOperationContextQuery` → `ValidatedPosOperationContext`
5. Appelle `core.limitpolicy.GetOfflineLimitPolicyQuery(tenantId)` → `OfflineLimitPolicy`
6. Vérifie via `OfflineGrantPolicy` :
  - Plan tenant permet offline ?
  - Terminal/device autorisés ?
  - Pas de grant `ACTIVE` existant ?
  - Quotas tenant disponibles ?
7. Génère :
  - Grant avec `validUntil = now + duration`, `syncAcceptedUntil = validUntil + 7 jours`
  - Code batch de N codes uniques (N défini par la policy)
  - **Signe le payload du grant avec la clé privée serveur (Ed25519)** → `grantSignature`
8. Persiste Grant + CodeBatch + Codes en transaction
9. Retourne au device :
  - Métadonnées du grant (limites, validUntil, syncAcceptedUntil)
  - `grantSignature`, `serverKeyId`, `schemaVersion`
  - Liste des codes offline
  - **Aucun secret partagé** (le device garde sa clé privée, le serveur garde la sienne)

**Cas d'erreur :** voir codes d'erreur section 12.

### 5.2 UC-02 : Renouveler un Grant

**Flow :**

1. App détecte besoin de renouveler (25% temps restant ou 80% quotas)
2. Envoie `POST /tenant/offline/grants` avec `renewExisting: true` et la même `devicePublicKey` (ou nouvelle si rotation)
3. Serveur :
  - Vérifie via policy
  - Émet un nouveau grant + nouveau batch
  - Marque l'ancien grant `SUPERSEDED` avec `supersededAt` et `supersededByGrantId`
  - Marque les codes `AVAILABLE` du précédent batch en `EXPIRED`
4. **Important :** l'ancien grant reste utilisable au sync pour les ventes faites pendant sa validité (jusqu'à `syncAcceptedUntil`)
5. App stocke le nouveau, garde l'ancien jusqu'à confirmation totale

### 5.3 UC-03 : Vendre offline

**Flow local Flutter — transaction atomique :**

1. Vérifier grant local (signature, validUntil, quotas) — fail-fast
2. **Démarrer transaction SQLite (Drift)**
3. Réserver le prochain code AVAILABLE → CONSUMED localement
4. Générer `clientSubmissionId` (UUID v4)
5. Construire le payload (lignes, totaux, contexte)
6. Calculer `payloadHash = SHA-256(canonical_json(payload))`
7. **Signer avec la clé privée Ed25519 du device** : `signature = Ed25519.sign(devicePrivateKey, payloadHash)`
8. Persister la submission avec `localStatus = COMMITTED_NOT_PRINTED`, hash, signature
9. Persister les lignes
10. Incrémenter compteurs locaux du grant
11. **Commit de la transaction**
12. **Hors transaction** : imprimer le ticket
13. Marquer `localStatus = PRINTED_PENDING_SYNC`

**Si crash entre 11 et 13 :** au redémarrage, détection des `COMMITTED_NOT_PRINTED`, dialog réimpression.

### 5.4 UC-04 : Synchroniser un batch de ventes offline

**Précondition côté client :**
- Toutes les submissions du batch partagent le même `grantId, codeBatchId, sellerId, terminalId, outletId, deviceId`
- Si plusieurs grants → plusieurs batches

**Flow client :**

1. Lire submissions `PRINTED_PENDING_SYNC` (max 50)
2. Marquer `SYNCING` localement
3. **Générer (ou réutiliser) `clientBatchId`** — persisté avant l'appel HTTP pour idempotence sur retry
4. Calculer `batchPayloadHash`
5. `POST /tenant/offline/sync`

**Flow serveur :**

1. **Idempotence batch :**
  - Si `(tenantId, clientBatchId)` existe avec **même** `batchPayloadHash` → retourner résultat précédent
  - Si existe avec hash **différent** → erreur `BATCH_IDEMPOTENCY_CONFLICT`
2. Créer `OfflineSyncBatch` avec status `RECEIVED`
3. Pour chaque submission :
   a. **Idempotence submission :**
  - Si `(tenantId, clientSubmissionId)` existe avec **même** `payloadHash` → résultat API `DUPLICATE` avec pointer vers l'originale (sans créer de ligne)
  - Si existe avec hash **différent** → résultat API `SUBMISSION_IDEMPOTENCY_CONFLICT`
    b. Sinon : créer `OfflineSubmission` avec status `RECEIVED`
    c. Lancer `OfflineSubmissionTechnicalPolicy.validate(submission)` (section 6.2)
    d. **Code → RESERVED** au début du traitement
    e. Si VALIDATED :
  - status = `TECH_VALIDATED`
  - Code → `CONSUMED_PROMOTED` (anticipation, sera confirmé après promotion)
  - Compteurs Grant incrémentés
  - Générer `promotionAttemptId`
  - Publier `OfflineSubmissionTechValidatedEvent` **self-contained** (avec tout le payload nécessaire à sales)
    f. Si REJECTED :
  - status = `TECH_REJECTED`
  - Code → `CONSUMED_REJECTED` (jamais retour à AVAILABLE)
4. Réponse au device :
   ```json
   {
     "clientBatchId": "...",
     "serverBatchId": "...",
     "results": [
       {"clientSubmissionId": "...", "serverStatus": "TECH_VALIDATED", "promotionAttemptId": "..."},
       {"clientSubmissionId": "...", "serverStatus": "TECH_REJECTED", "rejectionCode": "..."},
       {"clientSubmissionId": "...", "serverStatus": "DUPLICATE", "originalSubmissionId": "..."}
     ]
   }
   ```

**Important :** le résultat business (PROMOTED/BUSINESS_REJECTED) arrive de façon asynchrone via le pipeline d'events. Le client poll `GET /submissions/{clientSubmissionId}/status` pour obtenir le résultat final.

### 5.5 UC-05 : Promotion d'une submission en ticket

**Acteur :** Listener `@ApplicationModuleListener` dans `core.sales`
**Précondition :** `OfflineSubmissionTechValidatedEvent` reçu

**Flow :**

1. Sales reçoit l'event **self-contained** (toutes les données nécessaires sont dedans)
2. **Idempotence côté sales :** vérifier si un ticket existe déjà pour cette `offline_submission_id` (contrainte UNIQUE en base)
  - Si oui : ne pas recréer, publier `ProcessedEvent` avec l'outcome précédent (basé sur l'état du ticket)
3. Sinon : appliquer la logique sales (pricing, draw cutoff, limites, cohérence)
4. Trois résultats possibles :
   a. **OK** → créer ticket avec `offline_submission_id` rempli → publier `OfflineSubmissionProcessedEvent(outcome=PROMOTED, ticketId, promotionAttemptId)`
   b. **Rejet clair** → publier avec `outcome=BUSINESS_REJECTED, rejectionCode, rejectionReason`
   c. **Ambiguïté** → publier avec `outcome=NEEDS_REVIEW, reviewReason`
5. **Création ticket + publication event = même transaction logique** (transactional outbox via Modulith)

**Listener côté offlinesync (`OfflineSubmissionProcessedEventListener`) :**

```
Reçoit OfflineSubmissionProcessedEvent(submissionId, outcome, ticketId?, promotionAttemptId, ...)

Charger submission

Vérifications d'idempotence :
  Si event.promotionAttemptId != submission.promotionAttemptId :
    → event obsolète, ignorer (log warning)
  Si event.id == submission.lastPromotionEventId :
    → déjà traité, no-op
  
  Si submission.status == PROMOTED_TO_TICKET :
    Si outcome == PROMOTED ET event.ticketId == submission.createdTicketId :
      → no-op (rejeu normal)
    Si outcome == PROMOTED ET event.ticketId != submission.createdTicketId :
      → INCIDENT CRITIQUE : deux tickets pour une submission ! Alerter.
    Si outcome == BUSINESS_REJECTED :
      → INCIDENT CRITIQUE : reject sur submission déjà promue ! Alerter.
  
Sinon, appliquer :
  Selon outcome :
    PROMOTED → status=PROMOTED_TO_TICKET, créer TicketLink(CREATED), code=CONSUMED_PROMOTED
    BUSINESS_REJECTED → status=BUSINESS_REJECTED, code=CONSUMED_REJECTED, rejection details
    NEEDS_REVIEW → status=NEEDS_REVIEW, review reason

Persister submission.lastPromotionEventId = event.id
Mettre à jour compteurs du batch
Si tous les comptes du batch sont fixés → batch.status = COMPLETED ou PARTIAL
```

### 5.6 à 5.10 : autres use cases

(Identiques à v1 avec mises à jour de cohérence : ajout `dryRun` à la décision REPLAY, mise à jour des chemins de code, etc. Voir spec.md pour les scénarios formalisés.)

---

## 6. Politiques métier

### 6.1 `OfflineGrantPolicy`

**Inputs :**
- `ValidatedPosOperationContext` (de `core.session`)
- `OfflineLimitPolicy` (de `core.limitpolicy`)

**Sorties :**
- `GrantDecision.granted(batchSize, validityDuration, syncAcceptedExtension, maxTicketCount, maxTotalAmount)`
- `GrantDecision.denied(reason)`

### 6.2 `OfflineSubmissionTechnicalPolicy`

**Étapes (court-circuit au premier échec) :**

1. **Idempotence** : `(tenantId, clientSubmissionId)` :
  - Existe avec même `payloadHash` → résultat API `DUPLICATE` (pas une ligne créée)
  - Existe avec hash différent → `SUBMISSION_IDEMPOTENCY_CONFLICT`
2. **Tenant cohérent** : grant et code appartiennent au même tenant que la requête → sinon `GRANT_TENANT_MISMATCH` / `CODE_TENANT_MISMATCH`
3. **Grant existe** → sinon `GRANT_UNKNOWN`
4. **Grant utilisable** : status IN (ACTIVE, SUPERSEDED) → sinon `GRANT_INACTIVE`
5. **Fenêtre de création** : `clientSoldAt ∈ [validFrom - clock_skew, validUntil + grace_period]` → sinon `CLIENT_SOLD_AT_*`
6. **Fenêtre de réception** : `now <= syncAcceptedUntil` → sinon `GRANT_EXPIRED`
7. **Contexte cohérent** : terminal/seller/device match le grant → sinon `CONTEXT_MISMATCH`
8. **Code existe** dans le batch du grant → sinon `CODE_INVALID`
9. **Code utilisable** : status == AVAILABLE → sinon `CODE_ALREADY_RESERVED`, `CODE_VOIDED`, `CODE_EXPIRED`
10. **Schémas supportés** : `schemaVersion`, `signatureAlgorithm`, `canonicalizationVersion` reconnus → sinon `BATCH_SCHEMA_VERSION_UNSUPPORTED`
11. **Canonicalisation** : `payloadHash == SHA-256(canonical_json(rawPayload))` → sinon `PAYLOAD_HASH_MISMATCH` / `CANONICALIZATION_FAILED`
12. **Signature** : `Ed25519.verify(devicePublicKey, signature, payloadHash) == true` → sinon `SIGNATURE_INVALID`
13. **Cohérence dénormalisation** :
  - `totalStakeAmount == sum(lines.stakeAmount)` → sinon `TOTAL_AMOUNT_MISMATCH`
  - `lineCount == count(lines)` → sinon `LINE_COUNT_MISMATCH`
14. **Lignes valides** : non vides, `stakeAmount > 0` → sinon `LINES_INVALID`
15. **Quotas grant** : `consumedTicketCount + 1 <= maxTicketCount`, `consumedTotalAmount + totalStakeAmount <= maxTotalAmount` → sinon `GRANT_QUOTA_EXCEEDED`

**Constantes :**
- `grace_period = 5 minutes`
- `clock_skew_tolerance = 5 minutes`

---

## 7. Sécurité crypto

### 7.1 Principe : pas de secret partagé

**Le serveur ne stocke aucun secret capable de signer des submissions.** Seule la clé publique Ed25519 du device est stockée. La compromission du backend ne permet pas de forger des ventes.

### 7.2 Émission de Grant

**Côté device (au démarrage de l'app, une seule fois ou lors de rotation) :**
1. Générer une paire Ed25519 dans Android Keystore : `(devicePrivateKey, devicePublicKey)`
2. `devicePrivateKey` est **non exfiltrable** (Android Keystore garantit qu'elle ne peut sortir du TEE/StrongBox)
3. Stocker `keyId` (ex: UUID + version)

**Lors de la demande de grant :**
1. Device envoie `devicePublicKey`, `keyId` au serveur
2. Serveur stocke ces valeurs dans `OfflineGrant`
3. Serveur signe le grant avec sa propre clé privée Ed25519 (clé serveur, gérée par KMS/HSM) → `grantSignature`
4. Serveur retourne `grantSignature`, `serverKeyId`, paramètres du grant
5. Device vérifie `grantSignature` avec la clé publique serveur embarquée dans l'app

### 7.3 Signature de submission

**Device :**
1. Construire le payload incluant **obligatoirement** :
  - `tenantId`, `grantId`, `codeBatchId`, `offlineCode`
  - `clientSubmissionId`, `clientBatchId`
  - `sellerUserId`, `terminalId`, `outletId`, `deviceId`
  - `clientSoldAt`
  - Lignes complètes
  - `totalStakeAmount`, `lineCount`
  - `schemaVersion`, `signatureAlgorithm = "Ed25519"`, `canonicalizationVersion`, `keyId`
2. Sérialiser en **JSON canonique** (clés triées, pas d'espaces, encodage UTF-8 strict)
3. `payloadHash = SHA-256(canonical_json_bytes)`
4. `signature = Ed25519.sign(devicePrivateKey, payloadHash)`

**Serveur :**
1. Recevoir `rawPayload`, `payloadHash`, `signature`
2. Recalculer `expectedHash = SHA-256(canonical_json(parse(rawPayload)))`
3. Vérifier `expectedHash == payloadHash` → sinon `PAYLOAD_HASH_MISMATCH`
4. Charger `devicePublicKey` du grant via `keyId`
5. Vérifier `Ed25519.verify(devicePublicKey, signature, payloadHash) == true` → sinon `SIGNATURE_INVALID`

### 7.4 Gestion des clés serveur

- Clés serveur Ed25519 gérées par KMS (ou équivalent selon l'infra)
- Versionnées avec `serverKeyId`
- Rotation prévue (v2) : plusieurs clés actives, ancienne pour vérification rétroactive, nouvelle pour signature

### 7.5 Anti-replay et anti-forge

- Compromission du backend → **pas** de capacité de forge (le serveur n'a pas la clé privée device)
- Compromission d'un device → vols limités à ce device + révocation possible du grant
- `clientSubmissionId` empêche le rejeu d'une vente déjà soumise
- `payloadHash` détecte toute altération en transit
- Détection root/debugger en prod → wipe du keystore et blocage offline (best-effort)

### 7.6 Sérialisation canonique JSON

Schéma déterministe (`canonicalizationVersion = 1`) :
- Clés triées alphabétiquement à tous les niveaux
- Aucun espace
- UTF-8 strict
- Nombres : représentation décimale, pas de notation scientifique, point décimal
- Booléens : `true` / `false`
- Null : `null`
- Versionnement explicite via `canonicalizationVersion` dans le payload (anti-divergence)

Libs : `canonical_json` (Dart), `json-canonicalization` (Java).

---

## 8. Architecture package

(Identique à v1, avec ajouts/renommages mineurs)

```
core.offlinesync.api.command
  RequestOfflineGrantCommand, RenewOfflineGrantCommand, RevokeOfflineGrantCommand
  SyncOfflineSalesCommand
  ApproveOfflineSubmissionCommand, RejectOfflineSubmissionCommand
  ReplayOfflineSubmissionCommand (dry-run only v1)

core.offlinesync.api.query
  GetOfflineGrantQuery, GetCurrentOfflineGrantQuery
  GetOfflineSubmissionQuery, ListOfflineSubmissionsQuery
  GetOfflineSyncBatchQuery, GetOfflineDashboardQuery

core.offlinesync.api.event
  OfflineSubmissionTechValidatedEvent     # offlinesync → sales (self-contained)
  OfflineSubmissionAdminApprovedEvent     # offlinesync → sales
  OfflineSubmissionProcessedEvent         # sales → offlinesync (avec promotionAttemptId)

core.offlinesync.api.model
  OfflineGrantView, OfflineSubmissionView, ...

core.offlinesync.internal.domain.model
  (entités du domaine)

core.offlinesync.internal.domain.policy
  OfflineGrantPolicy
  OfflineSubmissionTechnicalPolicy

core.offlinesync.internal.domain.service
  OfflineCodeGenerator
  OfflineGrantSigner        # signe les grants avec la clé privée serveur
  OfflineSubmissionVerifier # vérifie les signatures Ed25519 device

core.offlinesync.internal.application.command.handler
  (handlers)

core.offlinesync.internal.application.listener
  OfflineSubmissionProcessedEventListener  # listener strictement idempotent

core.offlinesync.internal.application.port.out
  (repository ports)

core.offlinesync.internal.application.scheduled
  StuckSubmissionRecoveryJob       # détecte TECH_VALIDATED depuis > 10min, alerte
  OrphanedCodeReservationJob       # détecte codes RESERVED orphelins, les passe en CONSUMED_REJECTED
  GrantExpirationJob               # passe grants en EXPIRED quand validUntil dépassé

core.offlinesync.internal.infra.persistence
  (JPA entities, repositories, adapters)

core.offlinesync.internal.infra.web
  (controllers REST)
```

---

## 9. Persistance (schéma SQL)

### 9.1 Tables principales (avec changements v2)

```sql
CREATE TABLE offline_grant (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    seller_user_id UUID NOT NULL,
    terminal_id UUID NOT NULL,
    outlet_id UUID NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    code_batch_id UUID NOT NULL,
    device_public_key VARCHAR(128) NOT NULL,  -- Ed25519, ~44 chars base64
    key_id VARCHAR(64) NOT NULL,
    grant_signature VARCHAR(128) NOT NULL,     -- Ed25519 ~88 chars base64
    server_key_id VARCHAR(64) NOT NULL,
    schema_version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    sync_accepted_until TIMESTAMP NOT NULL,
    max_ticket_count INTEGER NOT NULL CHECK (max_ticket_count > 0),
    max_total_amount NUMERIC(18,2) NOT NULL CHECK (max_total_amount > 0),
    consumed_ticket_count INTEGER NOT NULL DEFAULT 0 CHECK (consumed_ticket_count >= 0),
    consumed_total_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (consumed_total_amount >= 0),
    issued_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(512),
    revoked_by UUID,
    superseded_at TIMESTAMP,
    superseded_by_grant_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_grant_dates CHECK (valid_from < valid_until AND valid_until < sync_accepted_until),
    CONSTRAINT chk_grant_quotas CHECK (consumed_ticket_count <= max_ticket_count
                                       AND consumed_total_amount <= max_total_amount)
);

CREATE UNIQUE INDEX uq_offline_grant_active_trio
    ON offline_grant(tenant_id, seller_user_id, terminal_id, device_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_offline_grant_tenant_seller ON offline_grant(tenant_id, seller_user_id);
CREATE INDEX idx_offline_grant_status ON offline_grant(status);
CREATE INDEX idx_offline_grant_valid_until ON offline_grant(valid_until);
CREATE INDEX idx_offline_grant_sync_accepted_until ON offline_grant(sync_accepted_until);
```

```sql
CREATE TABLE offline_sync_batch (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    client_batch_id UUID NOT NULL,
    batch_payload_hash VARCHAR(128) NOT NULL,
    terminal_id UUID NOT NULL,
    outlet_id UUID NOT NULL,
    seller_user_id UUID NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    grant_id UUID NOT NULL REFERENCES offline_grant(id),
    code_batch_id UUID NOT NULL REFERENCES offline_code_batch(id),
    sales_session_id UUID,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    submission_count INTEGER NOT NULL,
    technical_reject_count INTEGER NOT NULL DEFAULT 0,
    sales_accept_count INTEGER NOT NULL DEFAULT 0,
    sales_reject_count INTEGER NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    raw_manifest TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_offline_sync_batch_client UNIQUE (tenant_id, client_batch_id)
);
```

```sql
CREATE TABLE offline_submission (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sync_batch_id UUID NOT NULL REFERENCES offline_sync_batch(id),
    grant_id UUID NOT NULL REFERENCES offline_grant(id),
    code_batch_id UUID NOT NULL REFERENCES offline_code_batch(id),
    offline_code VARCHAR(16) NOT NULL REFERENCES offline_code(code),
    client_submission_id UUID NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    signature VARCHAR(256) NOT NULL,
    signature_algorithm VARCHAR(32) NOT NULL,
    canonicalization_version INTEGER NOT NULL,
    key_id VARCHAR(64) NOT NULL,
    schema_version INTEGER NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    seller_user_id UUID NOT NULL,
    terminal_id UUID NOT NULL,
    outlet_id UUID NOT NULL,
    sales_session_id UUID,
    client_sold_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    technical_status VARCHAR(32) NOT NULL,
    business_status VARCHAR(32),
    rejection_code VARCHAR(64),
    rejection_reason VARCHAR(1024),
    total_stake_amount NUMERIC(18,2) NOT NULL,
    line_count INTEGER NOT NULL,
    raw_payload TEXT NOT NULL,
    created_ticket_id UUID,
    promotion_attempt_id UUID,
    promotion_requested_at TIMESTAMP,
    last_promotion_event_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_offline_submission_client UNIQUE (tenant_id, client_submission_id),
    CONSTRAINT uq_offline_submission_code UNIQUE (tenant_id, offline_code)
);
```

```sql
CREATE TABLE offline_submission_decision (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    submission_id UUID NOT NULL REFERENCES offline_submission(id),
    decided_by UUID NOT NULL,
    decision VARCHAR(32) NOT NULL,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    reason VARCHAR(1024) NOT NULL CHECK (length(reason) > 0),
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    report_json TEXT,
    decided_at TIMESTAMP NOT NULL
);
```

### 9.2 Côté `core.sales` (modification)

```sql
-- Ajout dans la table ticket existante (ou table dédiée selon design sales)
ALTER TABLE ticket ADD COLUMN offline_submission_id UUID;
ALTER TABLE ticket ADD CONSTRAINT uq_ticket_offline_submission UNIQUE (tenant_id, offline_submission_id);
```

Cette contrainte UNIQUE est **la garantie ultime** qu'on ne peut pas créer deux tickets pour une même submission, même en cas de bug du listener.

---

## 10. Audit

Identique à v1, sauf :
- `OfflineGrant.grantSignature` et `OfflineSubmission.signature` → `@NotAudited` (volumineux)
- `OfflineGrant.devicePublicKey` → `@Audited` (changement important à tracer)

---

## 11. Events Modulith

### 11.1 `OfflineSubmissionTechValidatedEvent` (self-contained)

**Producteur :** `offlinesync`
**Consommateur :** `sales`
**Payload :**

```
- eventId: UUID (idempotence event)
- promotionAttemptId: UUID
- submissionId: UUID
- tenantId: UUID
- clientSubmissionId: UUID
- grantId: UUID
- offlineCode: String
- sellerUserId: UUID
- terminalId: UUID
- outletId: UUID
- deviceId: String
- clientSoldAt: Instant
- receivedAt: Instant
- lines: List<{lineNo, gameCode, betType, selectionKey, stakeAmount, potentialPayout}>
- totalStakeAmount: BigDecimal
- lineCount: Integer
- emittedAt: Instant
```

Tout ce dont sales a besoin pour créer le ticket. Pas de query retour.

### 11.2 `OfflineSubmissionAdminApprovedEvent`

```
- eventId: UUID
- promotionAttemptId: UUID (nouveau, différent de la première tentative)
- submissionId: UUID
- tenantId: UUID
- adminUserId: UUID
- approvalReason: String
- emittedAt: Instant
+ même payload que TechValidated (self-contained)
```

### 11.3 `OfflineSubmissionProcessedEvent`

**Producteur :** `sales`
**Consommateur :** `offlinesync` (`OfflineSubmissionProcessedEventListener`, strictement idempotent)
**Payload :**

```
- eventId: UUID
- promotionAttemptId: UUID (correspond à l'event aller)
- submissionId: UUID
- tenantId: UUID
- outcome: Enum { PROMOTED, BUSINESS_REJECTED, NEEDS_REVIEW }
- ticketId: UUID (nullable, si PROMOTED)
- rejectionCode: String (nullable, si BUSINESS_REJECTED)
- rejectionReason: String (nullable)
- reviewReason: String (nullable, si NEEDS_REVIEW)
- processedAt: Instant
```

---

## 12. Codes d'erreur normalisés

### 12.1 Grant emission

| Code | HTTP |
|---|---|
| `OFFLINE_NOT_ENABLED` | 403 |
| `EXISTING_GRANT_ACTIVE` | 409 |
| `SESSION_INVALID` | 400 |
| `TENANT_QUOTA_EXCEEDED` | 429 |
| `DEVICE_NOT_REGISTERED` | 400 |
| `DEVICE_PUBLIC_KEY_INVALID` | 400 |
| `SELLER_BANNED_OFFLINE` | 403 |

### 12.2 Batch sync

| Code |
|---|
| `BATCH_DUPLICATE` |
| `BATCH_EMPTY` |
| `BATCH_TOO_LARGE` |
| `BATCH_CONTEXT_MISMATCH` |
| `BATCH_PAYLOAD_INVALID` |
| `BATCH_SCHEMA_VERSION_UNSUPPORTED` |
| `BATCH_IDEMPOTENCY_CONFLICT` |

### 12.3 Submission technical

| Code |
|---|
| `SUBMISSION_IDEMPOTENCY_CONFLICT` |
| `OFFLINE_CODE_REQUIRED` |
| `GRANT_UNKNOWN` |
| `GRANT_INACTIVE` |
| `GRANT_EXPIRED` |
| `GRANT_QUOTA_EXCEEDED` |
| `GRANT_TENANT_MISMATCH` |
| `CONTEXT_MISMATCH` |
| `CODE_INVALID` |
| `CODE_ALREADY_RESERVED` |
| `CODE_VOIDED` |
| `CODE_EXPIRED` |
| `CODE_TENANT_MISMATCH` |
| `CODE_BATCH_EXPIRED` |
| `CODE_BATCH_REVOKED` |
| `PAYLOAD_HASH_MISMATCH` |
| `CANONICALIZATION_FAILED` |
| `SIGNATURE_INVALID` |
| `SIGNATURE_ALGORITHM_UNSUPPORTED` |
| `CLIENT_SOLD_AT_IN_FUTURE` |
| `CLIENT_SOLD_AT_BEFORE_GRANT` |
| `CLIENT_SOLD_AT_AFTER_GRANT` |
| `DEVICE_CLOCK_UNTRUSTED` |
| `LINES_INVALID` |
| `LINE_COUNT_MISMATCH` |
| `TOTAL_AMOUNT_MISMATCH` |
| `RAW_PAYLOAD_TOO_LARGE` |

### 12.4 Business (de sales)

| Code |
|---|
| `DRAW_CLOSED` |
| `LIMIT_EXCEEDED` |
| `GAME_UNKNOWN` |
| `BET_INVALID` |
| `STAKE_OUT_OF_RANGE` |

### 12.5 Promotion

| Code |
|---|
| `PROMOTION_TIMEOUT` |
| `SALES_UNAVAILABLE` |
| `PROMOTION_DUPLICATE_TICKET` |
| `PROMOTION_CONFLICT` |
| `TICKET_ALREADY_EXISTS_FOR_SUBMISSION` |
| `ADMIN_APPROVAL_REQUIRED` |
| `ADMIN_APPROVAL_FAILED` |

---

## 13. Edge cases et cas dégradés

### 13.1 Crash device après commit local, avant impression
Au redémarrage : détection `COMMITTED_NOT_PRINTED`, dialog réimpression. Si vendeur refuse, marquer `PRINTED_PENDING_SYNC` avec note "skipped".

### 13.2 Vente offline d'un grant SUPERSEDED sync après renouvellement
**Acceptée** si `clientSoldAt` dans la fenêtre de validité originale et `now <= syncAcceptedUntil`. C'est l'intérêt de séparer les deux fenêtres.

### 13.3 Grant révoqué pendant offline
Au prochain sync :
- Submissions avec `clientSoldAt < revokedAt` → traitées normalement
- Submissions avec `clientSoldAt >= revokedAt` → `TECH_REJECTED` avec code `GRANT_INACTIVE`
- Le client a un ticket papier sans valeur (cas extrême, traité hors système)

### 13.4 Horloge device décalée
Tolérance 5 minutes. Hors fenêtre → rejet. v2 : NTP forcé.

### 13.5 Sync interrompu (réseau coupe au milieu)
Idempotence via `clientBatchId` + `batchPayloadHash`. Au retry : retourne le résultat précédent si identique, sinon erreur de conflit.

### 13.6 Même `clientBatchId`, contenu différent
`BATCH_IDEMPOTENCY_CONFLICT` retourné. Incident à investiguer (bug client probable).

### 13.7 Même `clientSubmissionId`, payload différent
`SUBMISSION_IDEMPOTENCY_CONFLICT`. Incident.

### 13.8 Même `offlineCode`, deux `clientSubmissionId` différents
Premier arrivé gagne. Second reçoit `CODE_ALREADY_RESERVED`. Si payloads/contextes différents → incident sécurité (clonage de code ?), alerte.

### 13.9 Batch contenant submissions de plusieurs grants
**Interdit par contrat client.** Validation server-side : toutes les submissions du batch doivent partager `(grantId, codeBatchId, sellerUserId, terminalId, outletId, deviceId)`. Sinon `BATCH_CONTEXT_MISMATCH`. Le client doit découper en plusieurs batches.

### 13.10 Réception event ProcessedEvent obsolète (après replay)
`promotionAttemptId` permet de détecter. Event ignoré avec log warning.

### 13.11 Réception event ProcessedEvent avec ticketId différent du déjà-promu
**INCIDENT CRITIQUE.** Logger ERROR, alerter immédiatement, ne pas modifier l'état. Indique un bug majeur côté sales (deux tickets pour une submission).

### 13.12 Sales crée le ticket puis crash avant publication event
Avec transactional outbox Modulith : l'event est persisté dans `event_publication` dans la même transaction que la création du ticket. Au redémarrage, l'event est rejoué. Le listener offlinesync est idempotent et finit le travail.

### 13.13 Code RESERVED orphelin (crash serveur pendant validation)
Job `OrphanedCodeReservationJob` :
- Détecte les codes RESERVED depuis > 10 min
- Vérifie : la submission associée existe-t-elle ?
- Si non → code → `CONSUMED_REJECTED` avec raison "ORPHANED_RESERVATION"
- Si oui mais submission TECH_REJECTED → code → `CONSUMED_REJECTED`
- Si oui et TECH_VALIDATED → reprendre le flow normal

### 13.14 Perte du secure storage Android
- Si les submissions ont été signées et persistées en local **avant** la perte → elles peuvent être sync (la signature est déjà calculée)
- Si la perte survient avant signature → la submission ne peut pas être finalisée, données perdues
- D'où l'importance de tout faire dans la même transaction Drift

### 13.15 Désinstallation app avec ventes non synchronisées
**Ventes perdues côté serveur, mais clients ont des tickets papier.**

Mitigations UI :
- Badge permanent "X ventes non synchronisées"
- Blocage du logout / clear data si pending sync (avec override admin)
- Confirmation explicite "Je comprends que X ventes vont être perdues" avant désinstall (limité par Android)

Mitigation backend :
- Dashboard admin "Grants expirés sans sync complète"
- Alerte si un grant atteint son `syncAcceptedUntil` avec un delta significatif entre `consumedTicketCount` local estimé et les submissions réellement reçues (mais le serveur ne connaît pas le local...)

### 13.16 Mode offline forcé
Toggle UI. Device se comporte comme offline même si réseau dispo. Sync continue normalement.

### 13.17 Réimpression DUPLICATA
Disponible sur les submissions des 7 derniers jours. Marquage visuel "DUPLICATA #N".

### 13.18 Le device a un grant local mais le serveur ne le connaît pas
Cas extrême (vol, restauration backup). Au sync, `GRANT_UNKNOWN` pour toutes les submissions. App détecte et bloque le mode offline. Demande nouveau grant.

### 13.19 Rejet métier après ticket imprimé
Client a un ticket papier sans valeur. Trace côté offlinesync (rejectionCode). Vendeur notifié au prochain sync. v2 : workflow remboursement formalisé.

---

## 14. Métriques opérationnelles

### 14.1 Métriques techniques

| Métrique | Type | Utilité |
|---|---|---|
| `offlinesync_grants_issued_total` | Counter | Volume d'émission |
| `offlinesync_grants_revoked_total` | Counter | Révocations |
| `offlinesync_grants_superseded_total` | Counter | Renouvellements |
| `offlinesync_grants_active_count` | Gauge | Actifs en temps réel |
| `offlinesync_sync_batches_received_total` | Counter | Volume sync |
| `offlinesync_submissions_received_total` | Counter | Volume submissions |
| `offlinesync_submissions_by_status` | Gauge | Distribution par status |
| `offlinesync_idempotency_conflicts_total` | Counter | Conflits batch/submission |
| `offlinesync_tech_validation_duration_seconds` | Histogram | Performance validation |
| `offlinesync_promotion_event_emitted_total` | Counter | Events vers sales |
| `offlinesync_promotion_event_processed_total` | Counter | Events de retour |
| `offlinesync_promotion_lag_seconds` | Histogram | Délai TECH_VALIDATED → outcome |
| `offlinesync_stuck_submissions_count` | Gauge | TECH_VALIDATED > 10 min |
| `offlinesync_orphaned_codes_total` | Counter | Codes récupérés par le job |
| `offlinesync_double_ticket_incidents_total` | Counter | **Incident critique** (deux tickets) |
| `offlinesync_codes_consumed_total` | Counter | Codes brûlés |
| `offlinesync_codes_available_count` | Gauge | Codes disponibles par tenant |

### 14.2 Métriques business

| Métrique | Type |
|---|---|
| `offlinesync_offline_sales_ratio` | Gauge (par tenant) |
| `offlinesync_submissions_per_seller` | Histogram |
| `offlinesync_rejection_rate_by_code` | Counter |
| `offlinesync_admin_decisions_total` | Counter |
| `offlinesync_needs_review_count` | Gauge |

### 14.3 Alertes

| Alerte | Seuil | Action |
|---|---|---|
| `OfflineSyncBatchFailureRate` | > 5% en 5min | Investigation |
| `StuckSubmissions` | > 0 depuis 10min | Vérifier sales + event registry |
| `IdempotencyConflicts` | > 10/h | Investigation bug client |
| `DoubleTicketIncident` | >= 1 | **Critique**, page on-call |
| `OrphanedCodes` | > 5/h | Vérifier crashs serveur |
| `HighRejectionRate` | > 20% en 1h sur un tenant | Investigation |
| `NeedsReviewBacklog` | > 50 ouvertes > 24h | Alerter admin tenant |

---

## 15. Roadmap

### v1 (scope ce document)
- Tout ce qui est décrit ci-dessus

### v2
- Workflow remboursement formalisé
- Replay réel (avec annulation ticket préalable)
- Notification device au prochain sync
- Rotation des clés serveur Ed25519
- Métriques basiques de risque par seller (visibilité)
- iOS
- Sync NTP au démarrage

### v3
- Module `core.risk` avec trust levels
- Auto-downgrade / auto-ban configurable
- Politique de risque par tenant
- Job nocturne d'évaluation
- Dashboard de risque

### v4+
- Vérification offline-to-offline (QR signé)
- Promotion async via broker (Kafka) si volume justifie
- ML détection fraude

---

## 16. Questions résolues vs ouvertes

### Résolues en v2

✅ Format code : 9 chars, 3-3-3, alphabet anti-confusion
✅ Crypto : Ed25519 device, serveur ne stocke pas de secret signant
✅ Grace period : 5 minutes
✅ Tolérance horloge : 5 minutes
✅ `syncAcceptedUntil` : `validUntil + 7 jours` par défaut, configurable
✅ Cycle de vie codes : 6 états, jamais retour à AVAILABLE après soumission
✅ `SUPERSEDED` ajouté pour les grants renouvelés
✅ `DUPLICATE` : résultat API, plus état persistant
✅ `totalStakeAmount` et `lineCount` dénormalisés
✅ Events self-contained (pas de query retour)
✅ `promotionAttemptId` pour idempotence
✅ Contrainte UNIQUE côté sales sur `offline_submission_id`
✅ Statuts locaux Flutter enrichis
✅ Timeout SYNCING zombie : 15 min
✅ `potentialPayout >= 0` (assoupli)

### Ouvertes (à valider avec parties prenantes)

1. **Granularité du dashboard admin** : par tenant uniquement, ou cross-tenant pour super-admin ?
2. **Rétention des `raw_payload`** : à vie ou archivage après N jours/mois ?
3. **Comportement si tenant désactive offline avec grants actifs** : révoquer immédiatement ou laisser expirer ? (Position actuelle : laisser expirer)
4. **Modalité de signature des grants côté serveur** : KMS managé (AWS KMS, Azure Key Vault) ou clé Spring Boot avec rotation manuelle ?
5. **Bulk actions admin** : interdire en v1 (position actuelle), ou autoriser avec garde-fous ?
6. **Notification device au sync** : push notification ou simple polling sur prochaine sync ?
