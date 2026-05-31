# Domaine core.terminal — Terminaux, Binding Et Contexte Transactionnel

> Source de vérité détaillée : `terminal_binding.md`.

`core.terminal` est le domaine de sécurité qui relie un tenant, un acteur, un terminal, un binding d'appareil/application, un outlet et une session opérationnelle avant les opérations sensibles.

## Rôle

- Modéliser les terminaux physiques POS, les terminaux mobiles virtuels et les surfaces web/back-office éventuelles.
- Gérer le cycle de vie terminal : enregistrement, activation, lock/unlock, révocation et retrait propre.
- Gérer l'assignation active d'un terminal à un utilisateur.
- Gérer les bindings d'appareil ou d'application.
- Gérer les challenges d'activation éphémères.
- Produire un snapshot terminal validé pour les domaines appelants.

## Hors Périmètre

- Authentification identité : Keycloak et `common.context`.
- Permissions globales : `platform.accesscontrol`.
- Plan et quotas : `platform.entitlement.api`.
- Vente ticket : `core.sales`.
- Session de vente détaillée : `core.session`.
- Eligibility seller/outlet : `core.seller`.

## Modèle Normatif V1

Terminal V1 sépare lifecycle, connectivité, binding, capability et opération.

```text
TerminalKind        = nature physique/logique
TerminalSurface     = surface d'utilisation
TerminalStatus      = cycle de vie métier du terminal
TerminalSyncState   = connectivité / état sync
TerminalCapability  = capacité autorisée du terminal
TerminalOperation   = action demandée
TerminalBinding     = preuve active de liaison device/app/terminal
TerminalChallenge   = procédure ponctuelle pour créer/renouveler un binding
TerminalAssignment  = assignation terminal -> user, sans outlet
```

Enums normatives :

```text
TerminalKind        = PHYSICAL | VIRTUAL
TerminalSurface     = POS | MOBILE | WEB | BACK_OFFICE
TerminalStatus      = REGISTERED | PENDING_ACTIVATION | ACTIVE | LOCKED | REVOKED | RETIRED  ← internal/domain only
TerminalState       = REGISTERED | ACTIVE | LOCKED | OFFLINE | UNREGISTERED                 ← exposé dans TerminalView (api/)
TerminalSyncState   = ONLINE | OFFLINE | SYNC_PENDING | SYNC_CONFLICT
TerminalCapability  = SELL_TICKET | SELL_PHONE | PAYOUT_CLAIM | PRINT_TICKET | REPRINT_TICKET | OFFLINE_SELL | OFFLINE_SYNC | SCAN_TICKET
TerminalOperation   = SELL_TICKET | SELL_PHONE | PAYOUT_CLAIM | PRINT_TICKET | REPRINT_TICKET | OFFLINE_GRANT | OFFLINE_SYNC | SCAN_TICKET
```

> **Deux enums de lifecycle** — intentionnel : `TerminalStatus` (internal/domain, 6 valeurs) modélise le lifecycle métier riche. `TerminalState` (api/, 5 valeurs) est le subset exposé publiquement dans `TerminalView`, orienté consommateur. La traduction Status → State est faite dans l'adapter.  
> `OFFLINE` est dans `TerminalState` (état connectivité visible) mais PAS dans `TerminalStatus` (lifecycle interne) — cf. règle ci-dessous.

Binding :

```text
TerminalBindingType   = POS_DEVICE | MOBILE_APP | ADMIN_SELECTION
TerminalBindingStatus = ACTIVE | REVOKED | EXPIRED
```

Challenge :

```text
TerminalChallengeType   = POS_PAIRING | MOBILE_OTP | ADMIN_PAIRING_CODE
TerminalChallengeStatus = PENDING | CONSUMED | EXPIRED | CANCELLED
```

Assignment :

```text
TerminalAssignmentStatus = ACTIVE | REVOKED
```

## Axes Indépendants

Règle centrale :

```text
User permission != Terminal capability != Outlet flag != Session validity
```

Les quatre gates sont indépendants. Aucun ne remplace l'autre.

Un terminal actif ne suffit jamais à autoriser une opération.
Une permission utilisateur ne suffit jamais à autoriser une opération.
Une capability terminal ne suffit jamais à autoriser une opération.
Une session ouverte ne suffit jamais à autoriser une opération.

Les validations sont complémentaires :

| Gate | Propriétaire | Exemple |
| --- | --- | --- |
| Permission user | `platform.accesscontrol` | `ticket.sell`, `payout.pay` |
| Capability terminal | `core.terminal` | `SELL_TICKET`, `OFFLINE_SELL` |
| Outlet flag | `core.outlet` | ventes, payout ou offline activés |
| Session validity | `core.session` | session ouverte et compatible |

## Mapping Kind + Surface

| Cas | kind | surface |
| --- | --- | --- |
| POS physique | `PHYSICAL` | `POS` |
| Téléphone vendeur V1 | `VIRTUAL` | `MOBILE` |
| Web | `VIRTUAL` | `WEB` |
| Back-office | `VIRTUAL` | `BACK_OFFICE` |

Le téléphone vendeur reste `VIRTUAL + MOBILE` en V1. Le binding/fingerprint prouve l'application active sans prétendre gérer l'inventaire hardware complet.

## Lifecycle

### `TerminalStatus` — lifecycle interne (`internal/domain/model/`)

| Valeur | Sens |
|---|---|
| `REGISTERED` | Créé côté admin, pas encore prêt |
| `PENDING_ACTIVATION` | Assigné ou challenge en cours, binding attendu |
| `ACTIVE` | Utilisable si binding, capabilities et gates applicables passent |
| `LOCKED` | Blocage temporaire |
| `REVOKED` | Arrêt sécurité, compromis ou sanction |
| `RETIRED` | Fin de vie propre |

### `TerminalState` — état exposé dans `TerminalView` (`api/`)

| Valeur | Sens |
|---|---|
| `REGISTERED` | Terminal enregistré, non activé |
| `ACTIVE` | Terminal opérationnel |
| `LOCKED` | Bloqué temporairement |
| `OFFLINE` | Hors ligne (syncState) — exposé comme état composite |
| `UNREGISTERED` | Retiré / révoqué |

`OFFLINE` n'est pas dans `TerminalStatus` (lifecycle interne). Il est dans `TerminalState` comme projection du `TerminalSyncState` dans la vue API.

`EXPIRED` n'est pas un status terminal. Ce qui expire : binding, challenge, session, offline grant.

## Capabilities Et Operations

`TerminalCapability` décrit ce que le terminal est autorisé/capable de faire.
`TerminalOperation` décrit l'action demandée.

Le mapping opération -> capability est porté par `TerminalOperationPolicy`.

Exemples :

| Operation | Capability requise |
| --- | --- |
| `SELL_TICKET` | `SELL_TICKET` |
| `SELL_PHONE` | `SELL_PHONE` |
| `PAYOUT_CLAIM` | `PAYOUT_CLAIM` |
| `OFFLINE_GRANT` | `OFFLINE_SELL` |
| `OFFLINE_SYNC` | `OFFLINE_SYNC` |

`OFFLINE_GRANT` est une opération serveur. La capability terminal associée est `OFFLINE_SELL`.

## Assignment

`TerminalAssignment` est une assignation terminal -> user.

Il ne porte pas `outletId`.

Pour éviter plusieurs sources de vérité :

- `Terminal` porte l'`outletId` quand le terminal est rattaché à un outlet ;
- `TerminalAssignment` porte `terminalId + userId` ;
- `SellerOutletAssignment` porte `sellerId + outletId` ;
- `SalesSession` confirme l'outlet courant.

## Device Proof (POS/Mobile → Backend)

Pour les opérations sensibles (vente, payout, offline grant, offline sync), le POS signe une charge canonique avec sa clé privée avant d'envoyer la requête. Le backend vérifie cette signature via `VerifyTerminalDeviceProofQuery`.

**Headers requis (V1) :**
```http
X-Terminal-Id:         <terminalId>
X-Terminal-Binding-Id: <bindingId>
X-Terminal-Nonce:      <nonce unique>
X-Terminal-Signed-At:  <Instant ISO-8601>
X-Terminal-Signature:  <base64url signature Ed25519>
```

**Payload canonique signé (V1) :**
```
purpose\nmethod\npath\nbodyHash\nterminalId\nbindingId\noutletId\nsessionId\nnonce\nsignedAt
```
`bodyHash` = `""` en V1 (à compléter quand `ContentCachingFilter` sera disponible).

**Purposes :** `SELL_TICKET`, `PAYOUT_CONFIRM`, `OFFLINE_GRANT_REQUEST`, `OFFLINE_SYNC`

Le contrôleur appelle `TerminalDeviceProofGate.verify()` avant de dispatcher la commande. Si rejeté → `ProblemRest.forbidden(code)`.

## Nouveaux types (terminal-reorg-security)

```text
TerminalPublicKeyAlgorithm = ED25519
TerminalProofPurpose       = SELL_TICKET | PAYOUT_CONFIRM | OFFLINE_GRANT_REQUEST | OFFLINE_SYNC | HEARTBEAT | SYNC_STATE
```

`terminal_binding` porte maintenant : `publicKeyAlgorithm`, `publicKeyHash`, `credentialHash` (ex `bindingSecretHash`), `boundBy`.

`terminal_device_nonce` : anti-replay log, TTL 65 minutes, unique `(tenantId, bindingId, purpose, nonce)`.

## Validation

Les ventes et autres opérations sensibles doivent utiliser le modèle fail-fast défini dans `terminal_binding.md` :

1. contexte authentifié ;
2. permission ;
3. idempotence si requise ;
4. **device proof signature** (`TerminalDeviceProofGate.verify` → `VerifyTerminalDeviceProofQuery`) ;
5. contexte opérationnel trusted ;
6. terminal actif et tenant-compatible ;
7. assignation active pour l'acteur ;
8. binding actif compatible ;
9. capability terminal requise ;
10. outlet flag ;
11. session valide ;
12. seller/outlet eligibility ;
13. entitlement/règles métier selon l'action.

`core.terminal` ne décide pas seul qu'une opération métier peut avoir lieu. Il produit un snapshot terminal validé pour les domaines appelants.
