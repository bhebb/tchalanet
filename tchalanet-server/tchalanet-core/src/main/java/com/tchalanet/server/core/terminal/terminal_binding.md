# core.terminal — Résumé et modèle

Ce document décrit le périmètre et les règles du sous-domaine `core.terminal` : agrégats, value objects, invariants et algorithmes principaux (création, activation, résolution du contexte opérationnel et validation fail-fast).

## 1. Périmètre

D'après la documentation existante, `core.terminal` expose quatre concepts principaux. La décision structurante est de découper en agrégats séparés selon leurs cycles de vie :

- `Terminal` — agrégat racine (cycle de vie long, statut, invariants forts)
- `TerminalAssignment` — agrégat séparé (cycle de vie indépendant : assign / revoke)
- `TerminalDeviceBinding` — agrégat séparé (un terminal peut être re-bindé, changement d'appareil)
- `TerminalActivationChallenge` — agrégat séparé (éphémère, expirable, jetable)

Pourquoi ne pas tout mettre dans un seul agrégat `Terminal` ? Les cycles de vie sont désynchronisés : un challenge peut expirer sans toucher le terminal, un binding peut être révoqué sans révoquer le terminal, une assignation peut changer pendant que le terminal reste `ACTIVE`. Les regrouper créerait des transactions trop larges et des locks inutiles. Chaque agrégat possède sa propre transaction.

La règle de cohésion : les agrégats référencent `TerminalId`, mais ne se modifient jamais mutuellement dans la même transaction. La cohérence est éventuelle — via invariants vérifiés à la lecture, pas via cascades transactionnelles.

## 2. Modèle logique — agrégats et value objects

### 2.1 Value objects (Java, internal/domain/model)

- `TerminalId` — wrapper autour d'un UUID
- `TerminalCode` — varchar, unique par tenant, immuable après création
- `TerminalKind` — enum: `PHYSICAL` | `VIRTUAL`
- `TerminalSurface` — enum: `POS` | `MOBILE` | `WEB` | `BACK_OFFICE`
- `TerminalStatus` — enum: `REGISTERED` | `PENDING_ACTIVATION` | `ACTIVE` | `LOCKED` | `REVOKED` | `RETIRED`
- `TerminalSyncState` — enum: `ONLINE` | `OFFLINE` | `SYNC_PENDING` | `SYNC_CONFLICT`
- `TerminalCapability` — enum: `SELL_TICKET` | `SELL_PHONE` | `PAYOUT_CLAIM` | `PRINT_TICKET` | `REPRINT_TICKET` | `OFFLINE_SELL` | `OFFLINE_SYNC` | `SCAN_TICKET`
- `TerminalOperation` — enum: `SELL_TICKET` | `SELL_PHONE` | `PAYOUT_CLAIM` | `PRINT_TICKET` | `REPRINT_TICKET` | `OFFLINE_GRANT` | `OFFLINE_SYNC` | `SCAN_TICKET`
- `TerminalBindingType` — enum: `POS_DEVICE` | `MOBILE_APP` | `ADMIN_SELECTION`
- `TerminalBindingStatus` — enum: `ACTIVE` | `REVOKED` | `EXPIRED`
- `TerminalChallengeType` — enum: `POS_PAIRING` | `MOBILE_OTP` | `ADMIN_PAIRING_CODE`
- `TerminalChallengeChannel` — enum: `QR` | `SMS` | `EMAIL` | `SLACK` | `TEST_CAPTURE` | `ADMIN_MANUAL`
- `TerminalChallengeStatus` — enum: `PENDING` | `CONSUMED` | `EXPIRED` | `CANCELLED`

Remarque : les `capabilities` méritent d'être une table séparée (`terminal_capability`) plutôt qu'un `jsonb`. Une table permet l'indexation, l'audit ligne à ligne et des requêtes sans parsing JSON. Le `jsonb` n'a d'intérêt que si les capabilities deviennent un sac d'attributs libres — ce qui n'est pas le cas ici (enum fermé).

### 2.2 Agrégat `Terminal` — invariants

- INV-T1 : `tenantId` non-null, immuable
- INV-T2 : `code` unique par tenant, immuable après création
- INV-T3 : `kind` et `surface` immuables après création
- INV-T4 : transitions de statut autorisées (machine à états) :
  - `REGISTERED` → `PENDING_ACTIVATION`
  - `REGISTERED` → `REVOKED` | `RETIRED`
  - `PENDING_ACTIVATION` → `ACTIVE` (via binding vérifié)
  - `PENDING_ACTIVATION` → `REVOKED` | `RETIRED`
  - `ACTIVE` → `LOCKED` (réversible)
  - `LOCKED` → `ACTIVE`
  - `ACTIVE` | `LOCKED` → `REVOKED` | `RETIRED`
  - `REVOKED` → (aucune transition)
  - `RETIRED` → (aucune transition)

- INV-T5 : `VIRTUAL + MOBILE` exige `outlet_id` null OU outlet validé (la vente par téléphone n'est pas forcément rattachée à un comptoir physique)
- INV-T6 : `PHYSICAL + POS` exige `outlet_id` non-null pour passer `ACTIVE`
- INV-T7 : un terminal `ACTIVE` doit avoir au moins un binding `ACTIVE` compatible (invariant vérifié à la lecture/validation, pas porté dans la table)

INV-T4 est le cœur du domaine : implémentez une machine à états explicite dans le domaine (ne pas fournir un `setStatus()` libre).

### 2.3 Agrégat `TerminalAssignment` — invariants

- INV-A1 : (terminal, user, tenant) — une seule assignation `ACTIVE` à la fois
- INV-A2 : contrainte structurelle — unique active `PHYSICAL + POS` par user par tenant
- INV-A3 : contrainte structurelle — unique active `VIRTUAL + MOBILE` par user par tenant
- INV-A4 : `REVOKED` est terminal ; révoquer = créer une nouvelle ligne pour réassigner
- INV-A5 : on ne peut pas assigner un terminal `REVOKED` ou `EXPIRED`

INV-A2 / INV-A3 sont à la frontière core/entitlement : « unique per user » est structurel, mais le plafond (`MAX_PHYSICAL_TERMINALS_PER_USER`) vient de l'entitlement. Le domaine impose l'unicité ; le plafond est lu via une query au moment de l'assignation.

### 2.4 Agrégat `TerminalDeviceBinding` — invariants

- INV-B1 : un binding appartient à un terminal du même tenant
- INV-B2 : au plus un binding `ACTIVE` par terminal
- INV-B3 : `bindingType` doit être cohérent avec `terminalType` :
  - `PHYSICAL + POS` → `POS_DEVICE`
  - `VIRTUAL + MOBILE` → `MOBILE_APP`
  - `VIRTUAL + WEB|BACK_OFFICE` → `ADMIN_SELECTION`
- INV-B4 : `binding_secret_hash` / `binding_public_key` : jamais le secret en clair
- INV-B5 : `expires_at` dépassé → statut `EXPIRED` (scheduler ou lazy à la validation)
- INV-B6 : re-binding (changement d'appareil) = `REVOKED` l'ancien + créer un nouveau (ne jamais muter la clé en place)

### 2.5 Agrégat `TerminalActivationChallenge` — invariants

- INV-C1 : stocker uniquement `code_hash` (jamais le code en clair, jamais loggé)
- INV-C2 : `expires_at` court (ex. POS pairing 15 min, OTP 5 min — TTL par policy)
- INV-C3 : `attempt_count ≤ max_attempts` ; dépassement → `CANCELLED`
- INV-C4 : `PENDING` est le seul statut depuis lequel on peut vérifier
- INV-C5 : un seul challenge `PENDING` par `(terminal, user)` à la fois — créer un nouveau challenge annule le précédent
- INV-C6 : `CONSUMED` est à usage unique : un challenge consommé ne peut pas être réutilisé

Le challenge est séparé de son canal de livraison. Le domaine garde le hash, le TTL, les attempts et le statut ; les adapters de livraison gèrent SMS, email, Slack, QR, manuel ou capture test.

Canaux recommandés :

| Mode | POS_PAIRING | MOBILE_OTP | ADMIN_PAIRING_CODE |
| --- | --- | --- | --- |
| DEV | `QR` | `SLACK` ou `EMAIL` | `ADMIN_MANUAL` |
| E2E | `TEST_CAPTURE` | `TEST_CAPTURE` | `TEST_CAPTURE` |
| LIVE | `QR` | `SMS` | `ADMIN_MANUAL` |

`TEST_CAPTURE` est interdit en production. Il sert uniquement aux tests e2e automatisés pour récupérer le code clair via une surface test-only après création du challenge. Le code n'est toujours pas stocké dans l'agrégat.

En production, `MOBILE_OTP` par SMS est réservé aux activations, changement d'appareil, reset binding, suspicion de fraude ou step-up explicite. Un refresh token révoqué force une ré-authentification ; il ne déclenche un nouveau SMS que si aucun binding mobile actif et compatible ne peut être validé, ou si la policy de risque l'exige.

## 3. Algorithmes principaux

Les opérations sont découpées en commandes (cf. `PLAYBOOK.md` §7). Chaque handler est annoté `@UseCase` et s'exécute dans une transaction `@TchTx`.

### 3.1 Création de terminal

CreateTerminalCommand → CreateTerminalHandler

1. valider le tenant depuis le contexte (jamais depuis le client)
2. valider l'unicité du `code` pour le tenant — INV-T2
3. créer `Terminal(status = REGISTERED, kind = PHYSICAL, surface = POS)`
4. persister
5. AfterCommit → publier `TerminalCreatedEvent`

### 3.2 Assignation d'un terminal à un user

AssignTerminalToUserCommand → AssignTerminalToUserHandler

1. charger le terminal, vérifier `status ∈ {PENDING_ACTIVATION, ACTIVE}` — INV-A5
2. interroger l'entitlement : `MAX_PHYSICAL_TERMINALS_PER_USER`
3. compter les assignations `ACTIVE` du user → comparer au plafond — INV-A2
4. révoquer toute assignation `ACTIVE` existante sur ce terminal
5. créer `TerminalAssignment(status = ACTIVE)`
6. AfterCommit → publier `TerminalAssignedEvent`

### 3.3 Création d'un challenge d'activation (pairing POS)

CreateTerminalActivationChallengeCommand

1. charger le terminal, vérifier qu'il a une assignation `ACTIVE` pour le user
2. annuler tout challenge `PENDING` existant (terminal, user) — INV-C5
3. générer un code aléatoire (CSPRNG), calculer `code_hash`
4. créer le challenge (`type = POS_PAIRING`, `channel = QR`, `expires_at = now + TTL_pairing`, `max_attempts = 5`)
5. persister — retourner SEULEMENT le code en clair dans la réponse (jamais stocké)
6. AfterCommit → audit `CHALLENGE_CREATED`

### 3.4 Vérification d'un challenge & binding (pairing)

VerifyTerminalActivationChallengeCommand + BindPhysicalTerminalDeviceCommand

C'est l'algorithme central (idéalement deux commandes séparées ou une commande composite « pair ») :

1. charger le challenge, vérifier `status == PENDING` — INV-C4 ; sinon renvoyer `ProblemRest 409/410`
2. si `now > expires_at` → marquer `EXPIRED`, rejeter
3. incrémenter `attempt_count`
4. si `attempt_count > max_attempts` → marquer `CANCELLED`, rejeter
5. comparer le hash du code fourni au `code_hash` → en cas de mismatch : persister `attempt_count`, rejeter
6. si match :
   a. marquer challenge → `CONSUMED`, `consumed_at = now` (transaction agrégat challenge)
   b. créer `TerminalDeviceBinding(type = POS_DEVICE, status = ACTIVE, device_fingerprint_hash, binding_public_key)`
   c. passer le terminal `PENDING_ACTIVATION` → `ACTIVE` — INV-T4
   d. révoquer tout binding `ACTIVE` antérieur — INV-B2
7. AfterCommit → publier `TerminalActivatedEvent` + audit `ACTIVATE`

Point transactionnel : les étapes 6a/6b/6c touchent trois agrégats. Étant dans le même domaine et la même base, une seule transaction `@TchTx` est acceptable. Toutefois, ajouter un version guard sur le terminal (cf. `user-contexte-operational.md` §Concurrency) pour prévenir deux pairings concurrents qui pourraient activer deux bindings.

L'activation d'un terminal virtuel téléphone suit le même schéma, avec `TerminalChallengeChannel = SMS | EMAIL`, l'OTP envoyé via `platform.communication` (appel à `CommunicationApi`, après-commit du challenge) et un check d'entitlement `PHONE_SALES_ENABLED` en amont.

## 4. Algorithme — ResolveOperationalContextQuery

Ceci est appelé par `OperationalContextResolver` dans le pipeline HTTP. Lecture pure, pas de mutation.

Entrée : contexte (`tenant`, `user`), headers / claims (`X-Terminal-Id`, `X-Device-Binding`, `X-Outlet-Id`, `X-Sales-Session-Id`), `apiScope`

1. déterminer la source candidate :
   - binding signé valide présent → `SIGNED_DEVICE_BINDING`
   - sélection admin POS en session → `ADMIN_SELECTION`
   - bootstrap serveur → `SERVER_BOOTSTRAP`
   - sinon → `CLIENT_CLAIM` ou `NONE`
2. si source ∈ {`CLIENT_CLAIM`, `NONE`} :
   → retourner un `OperationalRequestContext` non-trusté (lecture OK, vente bloquée plus tard par `trustedOperationalContextRequired`)
3. si `SIGNED_DEVICE_BINDING` :
   a. vérifier la signature du binding token (clé publique stockée)
   b. binding `ACTIVE`, non expiré, `terminalId` du token == `X-Terminal-Id`
   c. terminal `ACTIVE`, `tenant == ctx.tenant`
   d. assignation `ACTIVE` (terminal, user) existe
   e. type de binding cohérent avec type du terminal
4. construire `OperationalRequestContext(terminalId, outletId, sessionId, source)`
5. ne PAS valider la session ici en profondeur — la validation fine (status, match seller/outlet) est faite "late, per action" (cf. convention). Le resolver attache les informations structurelles ; la validation détaillée se fera par action.

## 5. Algorithme — ValidateTerminalForOperationQuery (fail-fast)

Appelé par `core.sales` / `core.payout` au moment de l'action. Implémente les étapes 6→9 du fail-fast order de `user-contexte-operational.md`. Chaque échec lève un `ProblemRest` distinct (codes d'erreur explicites).

Entrée : `tenantId`, `terminalId`, `userId`, `requiredCapability`, `operationalCtx`

1. le terminal existe ET `terminal.tenant == tenantId` → sinon `TERMINAL_NOT_FOUND`
2. `terminal.status == ACTIVE` → sinon `TERMINAL_NOT_ACTIVE` (distinguer `LOCKED` / `REVOKED` / `EXPIRED`)
3. assignation `ACTIVE` (terminal, user) existe → sinon `TERMINAL_NOT_ASSIGNED`
4. binding `ACTIVE` compatible, non expiré → sinon `BINDING_INVALID`
5. `requiredCapability ∈ terminal.capabilities` → sinon `CAPABILITY_DENIED` (ex. `SELL_TICKET` → `SELL_TICKET`; `OFFLINE_GRANT` → `OFFLINE_SELL`)
6. retour `ValidateTerminalResult(ok, terminalSnapshot)`

Remarque : le "seller match" évoqué en relecture est résolu en deux temps. À l'étape 3, le `userId` vient de l'actor context, pas de `OperationalRequestContext`. Ensuite `core.sales` appelle `core.seller.ResolveSellerForOperationQuery(tenantId, userId, outletId, sessionId)` avant persistance pour obtenir `sellerId` et `sellerAssignmentId`. Le client ne fournit jamais `sellerId`.

## 6. Décision V1 — axes indépendants

Règle centrale :

```text
User permission != Terminal capability != Outlet flag != Session validity
```

Ces quatre gates sont indépendants et complémentaires.

`TerminalStatus.ACTIVE` ne suffit jamais. Une opération sensible exige aussi la capability terminal, la permission user, l'outlet flag, la session et les règles métier applicables.

| Operation | Permission user | Capability terminal | Outlet flag | Session |
| --- | --- | --- | --- | --- |
| `SELL_TICKET` | `ticket.sell` | `SELL_TICKET` | sales enabled | ouverte |
| `SELL_PHONE` | `ticket.sell.phone` | `SELL_PHONE` | sales enabled | ouverte |
| `PAYOUT_CLAIM` | `payout.pay` | `PAYOUT_CLAIM` | payout enabled | ouverte |
| `PRINT_TICKET` | `ticket.print` | `PRINT_TICKET` | print/sales enabled | selon contexte |
| `OFFLINE_GRANT` | `offline.grant` | `OFFLINE_SELL` | offline enabled | offline policy |
| `OFFLINE_SYNC` | `offline.sync` | `OFFLINE_SYNC` | offline enabled | sync policy |

## 7. Transition depuis le modèle terminal V0

Le code existant expose encore le vocabulaire historique :

```text
TerminalKind  = PHYSICAL | VIRTUAL
TerminalState = REGISTERED | ACTIVE | LOCKED | OFFLINE | UNREGISTERED
```

Le modèle cible du présent change introduit :

```text
TerminalKind  = PHYSICAL | VIRTUAL
TerminalSurface = POS | MOBILE | WEB | BACK_OFFICE
TerminalStatus = REGISTERED | PENDING_ACTIVATION | ACTIVE | LOCKED | REVOKED | RETIRED
TerminalSyncState = ONLINE | OFFLINE | SYNC_PENDING | SYNC_CONFLICT
```

La migration doit être progressive :

- `TerminalType` est retiré au profit de `TerminalKind + TerminalSurface` ;
- `PHYSICAL_POS` devient `PHYSICAL + POS` ;
- `VIRTUAL_PHONE` devient `VIRTUAL + MOBILE` ;
- `VIRTUAL_WEB` devient `VIRTUAL + WEB` ;
- `REGISTERED` reste `REGISTERED` quand le terminal est seulement inventorié ;
- `PENDING_ACTIVATION` représente l'étape assignée/challenge en cours/binding attendu ;
- `UNREGISTERED` devient `REVOKED` ;
- `OFFLINE` ne devient pas un statut lifecycle cible ; l'état offline relève de la sync/connectivité, pas du cycle de vie terminal ;
- la machine d'état cible est portée par `TerminalLifecyclePolicy`.

Tant que les migrations et les DTO publics ne sont pas basculés, les mappers JPA restent sur `TerminalKind/TerminalState`. Le domaine peut en revanche introduire les policies et les nouveaux agrégats sans casser la persistance existante.
