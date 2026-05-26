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
- `TerminalType` — enum: `PHYSICAL_POS` | `VIRTUAL_PHONE` | `VIRTUAL_WEB`
- `TerminalStatus` — enum: `PENDING_ACTIVATION` | `ACTIVE` | `LOCKED` | `REVOKED` | `EXPIRED`
- `TerminalCapability` — enum: `SELL_TICKET` | `SELL_PHONE` | `PAYOUT` | `OFFLINE_GRANT` | ...
- `BindingType` — enum: `PHYSICAL_DEVICE` | `VIRTUAL_PHONE`
- `BindingStatus` — enum: `ACTIVE` | `REVOKED` | `EXPIRED`
- `ChallengeType` — enum: `POS_PAIRING` | `VIRTUAL_PHONE_OTP` | `ADMIN_CODE`
- `ChallengeChannel` — enum: `QR` | `SMS` | `EMAIL` | `ADMIN_MANUAL`
- `ChallengeStatus` — enum: `PENDING` | `VERIFIED` | `EXPIRED` | `CANCELLED`

Remarque : les `capabilities` méritent d'être une table séparée (`terminal_capability`) plutôt qu'un `jsonb`. Une table permet l'indexation, l'audit ligne à ligne et des requêtes sans parsing JSON. Le `jsonb` n'a d'intérêt que si les capabilities deviennent un sac d'attributs libres — ce qui n'est pas le cas ici (enum fermé).

### 2.2 Agrégat `Terminal` — invariants

- INV-T1 : `tenantId` non-null, immuable
- INV-T2 : `code` unique par tenant, immuable après création
- INV-T3 : `type` immuable après création
- INV-T4 : transitions de statut autorisées (machine à états) :
  - `PENDING_ACTIVATION` → `ACTIVE` (via binding vérifié)
  - `PENDING_ACTIVATION` → `REVOKED`
  - `ACTIVE` → `LOCKED` (réversible)
  - `LOCKED` → `ACTIVE`
  - `ACTIVE` | `LOCKED` → `REVOKED` (irréversible)
  - `ACTIVE` | `PENDING` → `EXPIRED` (via TTL / scheduler)
  - `REVOKED` → (aucune transition)

- INV-T5 : `VIRTUAL_PHONE` exige `outlet_id` null OU outlet validé (la vente par téléphone n'est pas forcément rattachée à un comptoir physique)
- INV-T6 : `PHYSICAL_POS` exige `outlet_id` non-null pour passer `ACTIVE`
- INV-T7 : un terminal `ACTIVE` doit avoir au moins un binding `ACTIVE` compatible (invariant vérifié à la lecture/validation, pas porté dans la table)

INV-T4 est le cœur du domaine : implémentez une machine à états explicite dans le domaine (ne pas fournir un `setStatus()` libre).

### 2.3 Agrégat `TerminalAssignment` — invariants

- INV-A1 : (terminal, user, tenant) — une seule assignation `ACTIVE` à la fois
- INV-A2 : contrainte structurelle — unique active `PHYSICAL_POS` par user par tenant
- INV-A3 : contrainte structurelle — unique active `VIRTUAL_PHONE` par user par tenant
- INV-A4 : `REVOKED` est terminal ; révoquer = créer une nouvelle ligne pour réassigner
- INV-A5 : on ne peut pas assigner un terminal `REVOKED` ou `EXPIRED`

INV-A2 / INV-A3 sont à la frontière core/entitlement : « unique per user » est structurel, mais le plafond (`MAX_PHYSICAL_TERMINALS_PER_USER`) vient de l'entitlement. Le domaine impose l'unicité ; le plafond est lu via une query au moment de l'assignation.

### 2.4 Agrégat `TerminalDeviceBinding` — invariants

- INV-B1 : un binding appartient à un terminal du même tenant
- INV-B2 : au plus un binding `ACTIVE` par terminal
- INV-B3 : `bindingType` doit être cohérent avec `terminalType` :
  - `PHYSICAL_POS` → `PHYSICAL_DEVICE`
  - `VIRTUAL_PHONE` → `VIRTUAL_PHONE`
- INV-B4 : `binding_secret_hash` / `binding_public_key` : jamais le secret en clair
- INV-B5 : `expires_at` dépassé → statut `EXPIRED` (scheduler ou lazy à la validation)
- INV-B6 : re-binding (changement d'appareil) = `REVOKED` l'ancien + créer un nouveau (ne jamais muter la clé en place)

### 2.5 Agrégat `TerminalActivationChallenge` — invariants

- INV-C1 : stocker uniquement `code_hash` (jamais le code en clair, jamais loggé)
- INV-C2 : `expires_at` court (ex. POS pairing 15 min, OTP 5 min — TTL par policy)
- INV-C3 : `attempt_count ≤ max_attempts` ; dépassement → `CANCELLED`
- INV-C4 : `PENDING` est le seul statut depuis lequel on peut vérifier
- INV-C5 : un seul challenge `PENDING` par `(terminal, user)` à la fois — créer un nouveau challenge annule le précédent
- INV-C6 : `VERIFIED` est à usage unique : un challenge vérifié ne peut pas être réutilisé

## 3. Algorithmes principaux

Les opérations sont découpées en commandes (cf. `PLAYBOOK.md` §7). Chaque handler est annoté `@UseCase` et s'exécute dans une transaction `@TchTx`.

### 3.1 Création de terminal

CreateTerminalCommand → CreateTerminalHandler

1. valider le tenant depuis le contexte (jamais depuis le client)
2. valider l'unicité du `code` pour le tenant — INV-T2
3. créer `Terminal(status = PENDING_ACTIVATION, type = PHYSICAL_POS)`
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
   a. marquer challenge → `VERIFIED`, `verified_at = now` (transaction agrégat challenge)
   b. créer `TerminalDeviceBinding(type = PHYSICAL_DEVICE, status = ACTIVE, device_fingerprint_hash, binding_public_key)`
   c. passer le terminal `PENDING_ACTIVATION` → `ACTIVE` — INV-T4
   d. révoquer tout binding `ACTIVE` antérieur — INV-B2
7. AfterCommit → publier `TerminalActivatedEvent` + audit `ACTIVATE`

Point transactionnel : les étapes 6a/6b/6c touchent trois agrégats. Étant dans le même domaine et la même base, une seule transaction `@TchTx` est acceptable. Toutefois, ajouter un version guard sur le terminal (cf. `user-contexte-operational.md` §Concurrency) pour prévenir deux pairings concurrents qui pourraient activer deux bindings.

L'activation d'un terminal virtuel téléphone suit le même schéma, avec `ChallengeChannel = SMS | EMAIL`, l'OTP envoyé via `platform.communication` (appel à `CommunicationApi`, après-commit du challenge) et un check d'entitlement `PHONE_SALES_ENABLED` en amont.

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
5. `requiredCapability ∈ terminal.capabilities` → sinon `CAPABILITY_DENIED` (ex. `ticket.sell` → `SELL_TICKET`; `ticket.sell.phone` → `SELL_PHONE`)
6. retour `ValidateTerminalResult(ok, terminalSnapshot)`

Remarque : le "seller match" évoqué en relecture est résolu ici à l'étape 3 : le `userId` vient de l'actor context, pas de `OperationalRequestContext`. Le resolver attache terminal/outlet/session ; l'actor context indique qui est l'acteur ; la validation croise les deux.
