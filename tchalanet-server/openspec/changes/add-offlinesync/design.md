# Add offlinesync module — Design

> **Version 2** — décisions techniques mises à jour après revue. Voir DOMAIN_OFFLINESYNC.md pour le détail métier complet.

Décisions techniques organisées par bloc de conception.

---

## Bloc 1 — Grant

### Décision 1.1 — Le Grant est lié à `(seller, terminal, device)`

**Inchangé v1.** Découplé de `salesSessionId` pour survivre aux cycles de session POS.

### Décision 1.2 — Validation locale côté Flutter

**Inchangé v1.** Fail-fast immédiat. Le device vérifie la signature Ed25519 du grant avec la clé publique serveur embarquée dans l'app.

### Décision 1.3 — Renouvellement proactif

**Inchangé v1** dans le principe, **mise à jour v2** dans les détails :

- Seuils inchangés : 25% temps restant ou 80% quotas
- Le grant renouvelé doit transmettre **la même `devicePublicKey`** (ou nouvelle si rotation explicite)
- L'ancien grant passe en `SUPERSEDED` (pas `CONSUMED`)
- L'ancien grant reste **utilisable au sync** pour ses propres ventes jusqu'à `syncAcceptedUntil`

### Décision 1.4 — Pas de Grant émis si offline

**Inchangé v1.**

### Décision 1.5 — Séparation `validUntil` vs `syncAcceptedUntil` (NOUVEAU v2)

**Pourquoi.** En v1, la même limite servait à deux usages incompatibles :
- Empêcher la création de ventes hors période → légitime
- Empêcher la réception de ventes déjà faites mais syncées tardivement → bug, on rejetait des ventes légitimes

En séparant :
- `validUntil` : limite de **création** d'une vente. Le device cesse d'accepter de nouvelles ventes après.
- `syncAcceptedUntil` : limite de **réception serveur**. Le serveur accepte les submissions jusqu'à cette date, à condition que `clientSoldAt` était dans `[validFrom, validUntil + grace]`.

**Valeurs par défaut v1 :**
- `validUntil = now + 8h` (BUSINESS) ou `+ 24h` (PREMIUM)
- `syncAcceptedUntil = validUntil + 7 jours`

Configurables par tenant via `core.limitpolicy`.

**Conséquence opérationnelle.** Un vendeur en zone isolée peut faire des ventes pendant les heures de validité du grant, rester offline pendant plusieurs jours, et synchroniser sans perdre de ventes. Le scénario "vendeur en zone touristique avec réseau pourri" est désormais couvert proprement.

### Décision 1.6 — Statuts terminaux distincts (NOUVEAU v2)

```
EXPIRED    : now > validUntil (sans autre événement)
REVOKED    : action admin/policy
CONSUMED   : quotas atteints
SUPERSEDED : remplacé par un nouveau grant via renouvellement
```

**Pourquoi `SUPERSEDED` séparé de `CONSUMED`.** Deux faits différents méritent deux statuts différents. Un grant remplacé pour renouvellement n'est pas "consommé" au sens des quotas — il reste partiellement utilisé. Cette distinction est essentielle pour les enquêtes (pourquoi ce grant est-il dans cet état ?) et pour la sémantique de réception (un `SUPERSEDED` reste utilisable au sync).

### Décision 1.7 — Paramètres selon le plan tenant

**Mise à jour v2 :** ajout de `syncAcceptedExtension`.

| Plan | Offline | Batch size | validUntil | syncAcceptedExtension | Max tickets | Max amount |
|---|---|---|---|---|---|---|
| BASIC | NON | — | — | — | — | — |
| BUSINESS | OUI | 100 | 8h | 7j | 100 | configurable |
| PREMIUM | OUI | 500 | 24h | 7j | 500 | configurable |

Valeurs dans `core.limitpolicy`, jamais hardcodées dans offlinesync.

---

## Bloc 2 — Code offline

### Décision 2.1 — Génération côté serveur uniquement

**Inchangé v1.**

### Décision 2.2 — Format du code

**Inchangé v1.** `XXX-XXX-XXX`, alphabet anti-confusion.

### Décision 2.3 — Pas de QR code en v1

**Inchangé v1.**

### Décision 2.4 — Réservation atomique côté device

**Mise à jour v2.** Le pattern Drift reste, mais la séquence locale inclut maintenant explicitement la persistance du `payloadHash` et de la `signature` **avant** l'impression et **dans la même transaction** que la consommation du code :

```dart
await db.transaction(() async {
  // 1. Réserver le code
  final code = await db.reserveNextAvailable(grantId);
  
  // 2. Créer submission avec payloadHash et signature DÉJÀ calculés
  await db.insertSubmission(..., payloadHash, signature);
  
  // 3. Créer lignes
  await db.insertLines(...);
  
  // 4. Marquer code CONSUMED
  await db.markCodeConsumed(code.code);
  
  // 5. Incrémenter compteurs grant
  await db.incrementGrantCounters(...);
});
// Impression HORS transaction
```

**Pourquoi.** Si le secure storage Android Keystore devient indisponible après ce point (perte device, restauration backup), la submission peut encore être synchronisée car sa signature est déjà persistée localement.

### Décision 2.5 — Cycle de vie des codes : 6 états explicites (MAJEUR v2)

**Pourquoi le changement.** En v1, on disait qu'un code rejeté techniquement pouvait revenir à `AVAILABLE`. C'était incorrect : si une submission arrive avec un code, c'est qu'un device l'a consommé localement et probablement imprimé un ticket. Remettre `AVAILABLE` ouvre une porte de double-consommation et de confusion d'audit.

**Nouveau modèle :**

```
AVAILABLE             : disponible
RESERVED              : transitoire pendant validation tech
CONSUMED_PROMOTED     : TECH_VALIDATED + BUSINESS_ACCEPTED
CONSUMED_REJECTED     : TECH_REJECTED ou BUSINESS_REJECTED
VOIDED                : admin (révocation grant avant utilisation)
EXPIRED               : batch expiré sans utilisation
```

**Règle d'or :** *un code soumis au serveur ne redevient jamais `AVAILABLE`*.

**Conséquence.** Les statistiques de "codes brûlés" (CONSUMED_PROMOTED + CONSUMED_REJECTED) reflètent fidèlement la consommation réelle, indépendamment du résultat business.

### Décision 2.6 — Récupération des codes orphelins (NOUVEAU v2)

Si le serveur crash entre `RESERVED` et `CONSUMED_*`, un code reste en `RESERVED` sans submission associée traitée. Le job `OrphanedCodeReservationJob` :

- S'exécute toutes les 5 minutes
- Détecte les codes `RESERVED` depuis > 10 minutes
- Si la submission associée existe et est validée → poursuit le flow normal
- Si la submission est rejetée → code → `CONSUMED_REJECTED`
- Si pas de submission associée → code → `CONSUMED_REJECTED` avec raison "ORPHANED_RESERVATION"

---

## Bloc 3 — Sync batch et Submission

### Décision 3.1 — Idempotence stricte avec hash (MAJEUR v2)

**Pourquoi.** En v1, l'idempotence reposait uniquement sur les UUIDs (`clientBatchId`, `clientSubmissionId`). Mais ça ne distingue pas "retry légitime du même contenu" de "deux contenus différents soumis par erreur avec le même UUID" (bug client, attaque).

**Solution v2.**

**Niveau batch :**
- Persister `batchPayloadHash = SHA-256(canonical_json(batch_envelope))`
- Si `(tenantId, clientBatchId)` existe avec **même** `batchPayloadHash` → retourner résultat précédent (idempotence saine)
- Si existe avec hash **différent** → erreur `BATCH_IDEMPOTENCY_CONFLICT` (incident à investiguer)

**Niveau submission :**
- `payloadHash` déjà persisté
- Si `(tenantId, clientSubmissionId)` existe avec **même** `payloadHash` → résultat API `DUPLICATE` avec pointer vers l'originale, **sans créer de ligne**
- Si existe avec hash **différent** → erreur `SUBMISSION_IDEMPOTENCY_CONFLICT`

**Côté client Flutter :** `clientBatchId` doit être **généré et persisté avant** l'appel HTTP, et **réutilisé** en cas de retry du même batch logique. Pas de génération à la volée à chaque retry.

### Décision 3.2 — `DUPLICATE` comme résultat API, pas état persistant (NOUVEAU v2)

**Pourquoi.** En v1, j'avais un état `DUPLICATE` dans la machine à états de submission. Mais une duplicate "submission" n'est pas une vraie submission — c'est une retransmission d'une submission existante. Créer une ligne pour ça pollue la base et complique la machine à états.

**Maintenant :** quand le serveur détecte un duplicate, il **retourne** un résultat API `DUPLICATE` avec un pointer vers la submission originale, **sans créer aucune ligne**. La machine à états de submission a un état terminal en moins.

### Décision 3.3 — Taille de batch limitée

**Inchangé v1.** Max 50 par appel HTTP.

### Décision 3.4 — Validation technique en transaction

**Inchangé v1** dans le principe, **mise à jour v2** dans l'ordre (voir §6.2 du domaine) avec 15 étapes incluant les nouvelles vérifications (cohérence dénormalisation, schemaVersion, etc.).

### Décision 3.5 — Réponse synchrone technique, business asynchrone

**Inchangé v1.** Le résultat business est obtenu via polling.

### Décision 3.6 — Invariant : un batch = un seul grant (NOUVEAU v2)

**Pourquoi.** Sans cette contrainte, un batch peut mélanger des submissions de plusieurs grants/contextes, ce qui complique radicalement le traitement, les compteurs, l'audit et l'idempotence.

**Règle v2 :** toutes les submissions d'un `OfflineSyncBatch` partagent obligatoirement :
- `grantId`
- `codeBatchId`
- `sellerUserId`
- `terminalId`
- `outletId`
- `deviceId`

Si le client a des submissions de plusieurs grants à sync (cas du renouvellement avec submissions pending de l'ancien), il **doit** créer plusieurs batches distincts.

Validation server-side : si le batch contient une submission avec un contexte divergent → `BATCH_CONTEXT_MISMATCH`.

---

## Bloc 4 — Promotion via events (idempotence stricte MAJEUR v2)

### Décision 4.1 — Un event aller, un event retour avec outcome

**Inchangé v1** dans le principe.

### Décision 4.2 — `@ApplicationModuleListener` pour la durabilité

**Inchangé v1.** Spring Modulith garantit la durabilité via `event_publication`.

### Décision 4.3 — Events self-contained (CHANGEMENT v2)

**Pourquoi.** En v1, je proposais que sales fasse une query retour vers offlinesync pour récupérer le détail. Mauvaise idée :
- Couplage runtime entre les modules
- Performance dégradée (une query supplémentaire par event)
- Vulnérabilité : si offlinesync est lent/down, sales ne peut pas traiter ses events

**Maintenant.** L'event `OfflineSubmissionTechValidatedEvent` contient **tout** ce dont sales a besoin pour créer le ticket :
- Identifiants complets
- Lignes détaillées
- Totaux dénormalisés
- Métadonnées (`clientSoldAt`, `clientSubmissionId`, etc.)
- `promotionAttemptId`

Quelques KB par event, acceptable pour Modulith.

### Décision 4.4 — `promotionAttemptId` pour idempotence (NOUVEAU v2)

**Pourquoi.** Sans identifiant de tentative, on ne peut pas distinguer :
- "Cet event de retour correspond bien à mon dernier event aller" (cas nominal)
- "Cet event de retour est obsolète, il correspond à une tentative précédente déjà résolue" (à ignorer)
- "Cet event de retour est dupliqué" (à ignorer)

**Mécanisme.**

À chaque émission d'event aller, offlinesync génère un `promotionAttemptId` (UUID) et le persiste dans `submission.promotionAttemptId`. L'event embarque ce ID.

Côté sales, l'event retour cite le même `promotionAttemptId`.

À la réception côté offlinesync, le listener compare :
- Si `event.promotionAttemptId != submission.promotionAttemptId` → event obsolète, ignorer
- Si `event.id == submission.lastPromotionEventId` → event déjà traité, no-op
- Sinon → traiter normalement, persister `lastPromotionEventId`

### Décision 4.5 — Contrainte UNIQUE côté sales (NOUVEAU v2)

**Pourquoi.** Même avec un listener idempotent et `promotionAttemptId`, un bug pourrait théoriquement créer deux tickets pour une même submission. C'est inacceptable pour des transactions financières.

**Solution :** ajout d'une colonne `offline_submission_id` sur la table ticket de `core.sales` avec contrainte UNIQUE :

```sql
ALTER TABLE ticket ADD COLUMN offline_submission_id UUID;
ALTER TABLE ticket ADD CONSTRAINT uq_ticket_offline_submission 
    UNIQUE (tenant_id, offline_submission_id);
```

Cette contrainte rend physiquement impossible la création d'un doublon, même en cas de bug logiciel. La deuxième tentative reçoit une erreur de base de données → le listener catch et publie `ProcessedEvent` avec l'outcome correspondant au ticket existant.

### Décision 4.6 — Listener strictement idempotent (NOUVEAU v2)

Le `OfflineSubmissionProcessedEventListener` implémente les règles de réception définies en §5.5 du domaine :

```
PROMOTED + même ticketId déjà connu          → no-op
PROMOTED + ticketId différent                → INCIDENT CRITIQUE
BUSINESS_REJECTED sur submission PROMOTED    → INCIDENT CRITIQUE
Event obsolète (promotionAttemptId mismatch) → ignorer + log
Event déjà traité (eventId == lastEventId)   → no-op
```

Les incidents critiques déclenchent une alerte page on-call via `platform.communication`.

---

## Bloc 5 — Workflow admin

### Décision 5.1 — Dashboard admin par tenant

**Inchangé v1.**

### Décision 5.2 — Actions admin

**Mise à jour v2 :** ajout du flag `dryRun` sur `replay`.

| Action | Status accepté | Effet |
|---|---|---|
| `approve` | NEEDS_REVIEW | Génère nouveau `promotionAttemptId`, publie `AdminApprovedEvent` (self-contained), sales retente |
| `reject` | NEEDS_REVIEW | Status → BUSINESS_REJECTED, code → CONSUMED_REJECTED |
| `replay` (dry-run) | tout sauf PROMOTED_TO_TICKET | Simule en mémoire, retourne rapport, crée `Decision(dryRun=true, reportJson=...)` |
| `revoke grant` | Grant ACTIVE | Status → REVOKED, codes AVAILABLE → VOIDED |

### Décision 5.3 — Trace obligatoire des décisions

**Mise à jour v2 :** ajout du champ `dryRun` et `reportJson` sur `OfflineSubmissionDecision`.

Pour un replay dry-run :
- `decision = REPLAY`, `dryRun = true`
- `previousStatus == newStatus` (autorisé en dry-run uniquement)
- `reportJson` contient le détail de la simulation

### Décision 5.4 — Pas de bulk actions v1

**Inchangé.**

### Décision 5.5 — Replay dry-run only en v1

**Inchangé v1.**

---

## Bloc 6 — Sécurité crypto (REFONTE v2)

### Décision 6.1 — Ed25519 device, pas de secret partagé serveur (CHANGEMENT MAJEUR)

**Pourquoi le changement.** En v1, je proposais un schéma HMAC où le serveur stockait une `signingKey` partagée. Risque : la compromission du backend permettait de **forger** des submissions de tous les devices.

**Schéma v2 :**

**Côté device (au premier lancement ou rotation) :**
1. Générer paire Ed25519 dans Android Keystore (StrongBox si dispo)
2. `devicePrivateKey` est **non exfiltrable** (garantie matérielle Android)
3. `devicePublicKey` exportable, transmise au serveur lors de l'émission de grant

**Côté serveur :**
1. Stocker uniquement `devicePublicKey` dans `OfflineGrant`
2. **Aucun secret de signature côté serveur**
3. Le serveur a sa propre paire Ed25519 (KMS-managed) pour signer les grants — mais cette clé ne sert qu'à prouver l'authenticité du grant, pas à signer les submissions

**Bénéfice sécurité.** Compromission du backend → pas de forge possible de submissions. C'est essentiel pour un système financier.

### Décision 6.2 — Signature de submission Ed25519

**Implémentation Dart :**

```dart
final algorithm = Ed25519();
final keyPair = await algorithm.newKeyPair(); // au setup

// Pour chaque submission :
final payloadHash = sha256.convert(canonicalJson(payload)).bytes;
final signature = await algorithm.sign(payloadHash, keyPair: keyPair);
```

Avec `cryptography` ≥ 2.5.0 et `cryptography_flutter` pour utiliser Android Keystore comme backend.

**Vérification serveur (Java) :**

```java
var verifier = Ed25519.verifier();
var devicePublicKey = Ed25519PublicKey.fromBytes(grant.getDevicePublicKey());
boolean valid = verifier.verify(devicePublicKey, payloadHashBytes, signatureBytes);
```

### Décision 6.3 — Payload obligatoirement signé

Le payload **doit** inclure tous ces champs pour empêcher les attaques par substitution :

```
tenantId
grantId
codeBatchId
offlineCode
clientSubmissionId
clientBatchId
sellerUserId
terminalId
outletId
deviceId
clientSoldAt
lines (avec lineNo, gameCode, betType, selectionKey, stakeAmount, potentialPayout)
totalStakeAmount
lineCount
schemaVersion
signatureAlgorithm
canonicalizationVersion
keyId
```

Toute altération de n'importe quel champ change le `payloadHash` et invalide la signature.

### Décision 6.4 — Sérialisation canonique versionnée

**Inchangé v1** dans le principe, **mise à jour v2** : `canonicalizationVersion` explicite dans le payload, pour permettre une évolution future du schéma sans casser les anciens grants.

### Décision 6.5 — Rotation des clés (v2 du produit)

Hors scope v1. À prévoir :
- Versioning via `serverKeyId` et `keyId` (déjà en place)
- Coexistence de plusieurs clés actives serveur (ancienne pour vérifier les grants en cours, nouvelle pour les nouveaux grants)
- Politique de rotation annuelle minimum

### Décision 6.6 — Protection du token côté device

**Refondu v2 :** plus de token partagé à protéger. La clé privée est dans Android Keystore, qui :
- Stockage matériel (TEE ou StrongBox selon device)
- Clés non exportables
- Détection de modifications du déverrouillage de l'appareil

Le device n'a plus besoin de protéger un secret en mémoire — la clé ne sort jamais du Keystore.

### Décision 6.7 — Détection compromission device

Si l'app détecte un device rooté ou un debugger attaché en production :
- Refuser de générer la paire de clés (ou refuser de signer si déjà créée)
- Bloquer le mode offline
- Alerter le serveur au prochain online

Best-effort. La détection root parfaite est impossible, mais on relève la barre.

---

## Bloc 7 — Frontend Flutter (Android)

### Décision 7.1 — Stack technique

**Mise à jour v2 :** ajout `cryptography_flutter`.

| Concern | Choix |
|---|---|
| Storage local relationnel | Drift |
| Secure storage métadonnées | flutter_secure_storage |
| Clé privée Ed25519 | `cryptography` + `cryptography_flutter` (Android Keystore backend) |
| Détection connectivité | connectivity_plus + ping API |
| Background tasks | workmanager |
| Sérialisation canonique | canonical_json |
| Génération UUID | uuid |

### Décision 7.2 — Architecture en couches

**Inchangé v1.**

### Décision 7.3 — Atomicité de la vente offline

**Inchangé v1** dans le principe, **renforcé v2** : la signature Ed25519 est calculée et persistée **dans la transaction Drift**, avant l'impression. Plus de risque de perte de signature suite à une perte de Keystore après l'impression.

### Décision 7.4 — Mode offline forcé

**Inchangé v1.**

### Décision 7.5 — Réimpression DUPLICATA

**Inchangé v1.**

### Décision 7.6 — Sync : déclencheurs et stratégie

**Inchangé v1** dans les déclencheurs. **Mise à jour v2** sur l'idempotence :

- `clientBatchId` **persisté avant l'appel HTTP**, dans une table `local_pending_syncs` avec status `SCHEDULED`
- À l'appel HTTP, status → `IN_FLIGHT`
- À la réponse, status → `COMPLETED` ou `FAILED_RETRYABLE` ou `FAILED_CONFLICT`
- Sur retry : réutilisation du même `clientBatchId`
- Pas de génération à la volée

### Décision 7.7 — Politique de rétention locale

**Inchangé v1.** 7 jours après sync réussi.

### Décision 7.8 — Statuts locaux Flutter (NOUVEAU v2)

**Plus précis qu'en v1.**

```
DRAFT                     # construction en cours
COMMITTED_NOT_PRINTED     # commit Drift OK, impression pending
PRINTED_PENDING_SYNC      # ticket imprimé, en attente de sync
SYNCING                   # batch HTTP en cours
TECH_ACCEPTED             # serveur a validé techniquement, business en attente
PROMOTED                  # ticket créé côté serveur
REJECTED                  # rejeté (tech ou business)
SYNC_FAILED_RETRYABLE     # erreur réseau ou serveur 5xx, retry plus tard
SYNC_FAILED_FINAL         # rejet définitif côté serveur (validation tech)
```

Transitions notables :
- `SYNCING` zombie (> 15 minutes sans changement) → retour à `PRINTED_PENDING_SYNC`
- `TECH_ACCEPTED` reste en attente du polling pour passer à `PROMOTED` ou `REJECTED`

### Décision 7.9 — Horloge device

**Inchangé v1.** v2 : NTP forcé.

### Décision 7.10 — UI critique pour éviter perte de ventes (NOUVEAU v2)

Pour mitiger le scénario "désinstallation app avec submissions non syncées" :

- Badge permanent "X ventes non synchronisées" visible
- Blocage du flow de logout / clear data si pending sync (avec override admin codé)
- Notification système si > 24h avec pending sync
- Dashboard admin "Grants avec activité offline mais sync incomplète"

---

## Bloc 8 — Observabilité

### Décision 8.1 — Métriques Prometheus

Voir §14 du document de domaine. Ajouts v2 :
- `offlinesync_idempotency_conflicts_total`
- `offlinesync_orphaned_codes_total`
- `offlinesync_double_ticket_incidents_total`
- `offlinesync_grants_superseded_total`

### Décision 8.2 — Logs structurés

**Inchangé v1.** Ajout systématique de `promotionAttemptId` dans les logs liés à la promotion.

### Décision 8.3 — Alertes

**Mise à jour v2 :**

| Alerte | Seuil | Action |
|---|---|---|
| `DoubleTicketIncident` | >= 1 | Page on-call critique |
| `IdempotencyConflicts` | > 10/h | Investigation bug client |
| `OrphanedCodes` | > 5/h | Vérifier crashs serveur |
| Autres | (inchangé v1) | |

---

## Risques techniques et mitigations (mise à jour v2)

| Risque | Mitigation v2 |
|---|---|
| Event publication registry grossit | Job de purge des events traités > 30 jours |
| Deadlock JPA sur le code | Index dédié + SELECT FOR UPDATE explicite sur transition AVAILABLE → RESERVED |
| Migration Flyway lente | Découpage en plusieurs scripts, test sur snapshot prod |
| Surcharge sync simultanée multi-devices | Limite débit par tenant (config) |
| Bug d'idempotence | Tests d'intégration + monitoring `IdempotencyConflicts` |
| Compromission Android Keystore d'un device | Révocation grant, pas d'impact cross-device (Ed25519 par device) |
| Perte de la clé privée serveur Ed25519 | KMS avec backup et rotation, plan de continuité documenté |
| Bug régression crypto | Code review crypto obligatoire, tests de propriété, audit externe avant prod |
