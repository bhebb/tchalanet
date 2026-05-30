# Offline Grant et Sync — Flow

> Ventes réalisées sans connectivité réseau, soumises et validées lors de la reconnexion.  
> Domaine : `core.offlinesync`  
> Feature : `features.cashier.offline` · `FEATURE_CASHIER_OFFLINE.md`

---

## Pourquoi

En zone sans réseau, le vendeur continue de vendre grâce à des codes offline pré-attribués. L'application signe chaque vente localement. À la reconnexion, le batch de soumissions est envoyé pour validation technique puis métier.

Le grant est la délégation de confiance : il limite la capacité de vente offline (nombre de tickets, montant total, durée).

---

## États du OfflineGrant

| État | Signification |
|---|---|
| `ACTIVE` | Grant utilisable — ventes offline autorisées |
| `EXPIRED` | TTL dépassé — ventes offline bloquées |
| `REVOKED` | Révoqué par l'admin (fraude, terminal perdu) |

---

## États d'une OfflineSubmission

```
RECEIVED
  → TECH_VALIDATED → PROMOTION_REQUESTED → PROMOTED → (traitement sales)
  → TECH_REJECTED
  → NEEDS_ADMIN_REVIEW → ADMIN_APPROVED → (traitement sales)
                       → ADMIN_REJECTED
  → BUSINESS_REJECTED
  → SYNC_FAILED
```

| État | Signification |
|---|---|
| `RECEIVED` | Soumission reçue, en attente de validation |
| `TECH_VALIDATED` | Format et signature OK |
| `TECH_REJECTED` | Signature invalide, hash incorrect, code inconnu |
| `PROMOTION_REQUESTED` | Validation OK — demande de promotion vers tickets |
| `PROMOTED` | Tickets créés dans `core.sales` |
| `BUSINESS_REJECTED` | Règle métier échouée (tirage fermé, limite, etc.) |
| `NEEDS_ADMIN_REVIEW` | Cas ambigu — intervention humaine requise |
| `ADMIN_APPROVED` | Admin valide manuellement |
| `ADMIN_REJECTED` | Admin rejette |
| `SYNC_FAILED` | Erreur technique lors du traitement |

---

## Flow : Obtenir le grant

```
GET /tenant/cashier/offline/grant/current
    ?terminalId=<id>&deviceId=<uuid>
→ {
    grantId, codes:[...],
    validFrom, validUntil,
    maxTicketCount, maxTotalAmount
  }
```

Le mobile récupère le grant en avance (idéalement à l'ouverture de session, quand le réseau est disponible). Les `codes` sont les codes offline à utiliser pour signer les ventes.

---

## Flow : Ventes offline (sans réseau)

```
Pour chaque vente :
  - Sélectionner un code offline disponible depuis le grant
  - Construire le ticket localement (gameCode, betType, selection, stake, drawId)
  - Calculer payloadHash = sha256(payload)
  - Signer avec la clé Ed25519 du device : signature = sign(payloadHash)
  - Stocker localement : { clientSubmissionId, offlineCode, drawId, clientSoldAt,
                           totalStakeAmount, lines, payloadHash, signature }
```

Aucune requête réseau pendant la vente offline.

---

## Flow : Synchronisation (reconnexion)

```
POST /tenant/cashier/offline/submissions
{
  grantId, clientBatchId, batchPayloadHash,
  submissions: [
    {
      clientSubmissionId, offlineCode, drawId, clientSoldAt,
      totalStakeAmount, lines, payloadHash, signature
    }, ...
  ]
}
→ 202 {
    syncBatchId,
    outcomes: [
      { clientSubmissionId, submissionId, outcome, rejectionCode, rejectionReason }
    ]
  }
```

Le serveur traite chaque submission de façon indépendante :

```
1. Vérifier signature Ed25519 device
2. Vérifier payloadHash
3. Vérifier offlineCode appartient au grant
4. Valider règles métier (tirage fermé ?, doublon ?)
5. Créer ticket via core.sales (PROMOTED) ou rejeter
```

---

## Invariants

- Un `offlineCode` ne peut être utilisé qu'une fois
- Le `clientBatchId` est idempotent — re-poster le même batch ne crée pas de doublons
- Le serveur re-vérifie toujours signature + hash — le client ne peut pas falsifier
- Si le tirage est fermé au moment de la sync → `BUSINESS_REJECTED` avec `DRAW_NOT_OPEN`
- `NEEDS_ADMIN_REVIEW` est exceptionnelle — l'admin est notifié

---

## Limites du grant

Le grant définit des bornes strictes :
- `maxTicketCount` — nombre max de tickets vendables offline
- `maxTotalAmount` — montant total max
- `validUntil` — expiration — ventes après cette date rejetées à la sync

Quand les limites sont atteintes, l'app mobile doit bloquer la vente offline et informer le vendeur.

---

## Références

- Domaine : `core/offlinesync/DOMAIN_OFFLINESYNC.md`
- Feature offline : `features.cashier/offline/FEATURE_CASHIER_OFFLINE.md`
- Guide mobile : `features.cashier/MOBILE_FLOW.md §12`
