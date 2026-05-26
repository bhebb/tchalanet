# Terminal onboarding et utilisation

Ce document explique le flow fonctionnel V1 pour mettre un terminal en service et l'utiliser dans une opération sensible.

## 1. Principe

Un terminal est une surface opérationnelle rattachée à un tenant. Il ne remplace pas l'utilisateur, le seller, l'outlet ou la session.

```text
Login utilisateur -> identité
Terminal trusted  -> contexte opérationnel
Seller actif      -> identité commerciale
Session ouverte   -> cadre d'opération
```

## 2. Onboarding POS physique

1. L'admin crée un terminal `PHYSICAL + POS`.
2. Le terminal est rattaché à un outlet si le POS est comptoir.
3. L'admin assigne le terminal au user vendeur.
4. Le terminal passe en `PENDING_ACTIVATION`.
5. Le serveur crée un `TerminalChallengeType.POS_PAIRING`.
6. Le POS soumet le code et la preuve device/app.
7. Le serveur consomme le challenge (`CONSUMED`), crée un binding `POS_DEVICE`, puis active le terminal.
8. Les opérations sensibles exigent ensuite un binding actif et compatible.

## 3. Onboarding mobile vendeur

1. L'admin crée un terminal `VIRTUAL + MOBILE`.
2. Le seller doit être lié au user vendeur.
3. Le seller doit être assigné à l'outlet d'opération.
4. Le tenant doit permettre la vente téléphone si l'usage est `SELL_PHONE`.
5. Le serveur crée un challenge `MOBILE_OTP`.
6. L'app mobile vérifie l'OTP et enregistre un binding `MOBILE_APP`.
7. Le terminal peut vendre seulement si les gates terminal, permission, outlet et session passent.

## 4. Challenge et canaux

Le challenge est toujours un objet métier. Le canal est seulement un moyen de livraison.

```text
TerminalChallenge = preuve ponctuelle, hashée, expirable, attempt-limited
TerminalChallengeChannel = QR | SMS | EMAIL | SLACK | TEST_CAPTURE | ADMIN_MANUAL
```

Le domaine ne doit jamais dépendre du coût ou du fournisseur SMS. Il demande une livraison via une policy :

| Mode | POS_PAIRING | MOBILE_OTP | ADMIN_PAIRING_CODE |
| --- | --- | --- | --- |
| DEV | `QR` | `SLACK` | `ADMIN_MANUAL` |
| E2E | `TEST_CAPTURE` | `TEST_CAPTURE` | `TEST_CAPTURE` |
| LIVE | `QR` | `SMS` | `ADMIN_MANUAL` |

`TEST_CAPTURE` est réservé aux tests automatisés. Il permet de récupérer le code clair via une surface e2e/test-only sans changer le challenge métier. Cette surface n'existe jamais en production.

`SLACK` et `EMAIL` peuvent être utilisés en développement pour éviter le coût SMS. Le code reste hashé dans le domaine ; seul l'adapter de livraison voit le code clair au moment de l'envoi.

En live, le SMS ne doit pas être utilisé à chaque login ni à chaque refresh token normal. Une révocation de refresh token force une ré-authentification. Un nouveau challenge SMS est requis seulement si le binding mobile actif ne peut pas être retrouvé/validé, si l'appareil change, ou si la policy de risque exige un step-up.

## 5. Utilisation dans une vente

Le client envoie le token d'identité et les headers opérationnels :

```http
Authorization: Bearer <access-token>
Idempotency-Key: <operation-key>
X-Terminal-Id: <terminal-id>
X-Outlet-Id: <outlet-id>
X-Sales-Session-Id: <session-id>
X-Device-Binding: <signed-binding-token>
```

En seed local, le terminal POS de demo accepte le credential de binding
`local-dev-binding-token`. Ce token n'est jamais un modèle de production : il
sert uniquement à tester le pipeline `X-Device-Binding` sans SMS ni coût externe.
Le serveur compare son hash tenant-safe au `binding_secret_hash` stocké.

Le payload ne fournit jamais `sellerId`.

Flow serveur :

1. `common.context` crée le `TchRequestContext`.
2. Le contexte opérationnel est attaché comme trusted ou untrusted.
3. Le use case sensible appelle `trustedOperationalContextRequired()`.
4. `core.terminal` valide terminal, assignment, binding et capability.
5. `core.outlet` valide le flag métier de l'outlet.
6. `core.session` valide la session.
7. `core.seller` résout `sellerId` et `sellerAssignmentId`.
8. `core.sales` applique les règles ticket et persiste le snapshot.

## 6. Gates par opération

| Operation | Permission | Capability | Outlet | Session |
| --- | --- | --- | --- | --- |
| `SELL_TICKET` | `ticket.sell` | `SELL_TICKET` | sales enabled | ouverte |
| `SELL_PHONE` | `ticket.sell.phone` | `SELL_PHONE` | sales enabled | ouverte |
| `PAYOUT_CLAIM` | `payout.pay` | `PAYOUT_CLAIM` | payout enabled | ouverte |
| `PRINT_TICKET` | `ticket.print` | `PRINT_TICKET` | print/sales enabled | selon contexte |
| `OFFLINE_GRANT` | `offline.grant` | `OFFLINE_SELL` | offline enabled | offline policy |
| `OFFLINE_SYNC` | `offline.sync` | `OFFLINE_SYNC` | offline enabled | sync policy |

## 7. États importants

- `TerminalStatus.ACTIVE` ne suffit pas à vendre.
- `TerminalSyncState.OFFLINE` ne bloque pas automatiquement le terminal.
- `TerminalBindingStatus.EXPIRED` bloque les opérations sensibles jusqu'au re-binding.
- `TerminalChallengeStatus.CONSUMED` signifie que le challenge ne peut plus être réutilisé.
