# Design — Clean Architecture + Bus Naming

## 1. Mental model

Le backend applique :

```text
web / batch / event / scheduler
  -> CommandBus.execute(...) ou QueryBus.ask(...)
    -> application handler
      -> domain
      -> application port.out
        -> infra adapter
```

Les dépendances de code pointent vers l’intérieur :

```text
infra -> application -> domain
```

Jamais l’inverse.

## 2. Services

### 2.1 `domain/service`

Un service de domaine est une règle métier pure.

Autorisé :

- `Policy`
- `Calculator`
- `Rule`
- `Matcher`
- `Specification`

Interdit :

- Spring annotations
- repositories
- ports
- CommandBus / QueryBus
- transaction
- HTTP/JPA/cache/messaging
- lecture d’un autre domaine

Exemples :

```text
DrawLifecyclePolicy
PayoutEligibilityPolicy
TicketWinningCalculator
```

### 2.2 `application/service`

Un service applicatif est optionnel. Il sert à extraire une orchestration trop grosse depuis un handler.

Autorisé :

- appeler des ports ;
- appeler QueryBus/CommandBus si l’orchestration cross-domain est assumée ;
- assembler des snapshots ;
- utiliser Clock, IdGenerator, AfterCommit.

Interdit :

- devenir un `XxxService` fourre-tout ;
- contenir les invariants qui appartiennent au domaine ;
- accéder à des repositories ou JPA entities directement.

Nommage préféré :

```text
XxxOrchestrator
XxxAssembler
XxxPlanner
XxxCoordinator
```

Éviter :

```text
DrawService
TicketService
PayoutService
```

## 3. Ports

### 3.1 `port.in`

Non utilisé par défaut.

Raison : le projet utilise CommandBus/QueryBus. Les command/query models sont le contrat d’entrée applicatif.

Interdit par défaut :

```text
application/port/in
port/in
```

Exception : ADR explicite si un module doit exposer une API applicative stable hors bus.

### 3.2 `port.out`

Obligatoire pour les dépendances sortantes du use case :

- persistence du même domaine ;
- external API ;
- provider ;
- cache manuel contrôlé ;
- gateway ;
- reader technique owned by the same domain.

Placement canonique :

```text
core.<domain>.application.port.out
```

## 4. Inter-domain communication

### 4.1 Lecture simple

Un domaine peut lire une projection minimale d’un autre domaine via :

1. `QueryBus.ask(new GetXxxForYyyQuery(...))`, recommandé ; ou
2. API/read interface stable exposée par le domaine owner ; ou
3. catalog read API si la donnée est référentielle/read-mostly.

Le domaine consommateur ne possède pas l’adapter persistence d’un aggregate étranger.

Exemple :

```text
payout -> QueryBus.ask(GetTicketForPayoutQuery)
sales -> lit ticket via son adapter persistence
sales -> retourne TicketForPayoutView
```

### 4.2 Effet métier / mutation

Un domaine ne modifie pas l’aggregate d’un autre domaine directement.

Pattern obligatoire :

```text
source handler
  -> modifie son aggregate
  -> AfterCommit publish DomainEvent

consumer listener
  -> TransactionalEventListener(AFTER_COMMIT)
  -> idempotence
  -> CommandBus.execute(local command)
```

Exemple :

```text
PayoutPaidEvent
  -> sales listener
  -> MarkTicketPaidCommand
```

## 5. Bus naming

### 5.1 Public bus methods

```java
CommandBus.execute(command)
QueryBus.ask(query)
```

### 5.2 Handler method

```java
CommandHandler.handle(command)
QueryHandler.handle(query)
```

Règle de langage :

```text
On execute une command.
On ask une query.
Seul un handler handle.
```

## 6. ArchUnit enforcement strategy

Phase 1 — P0 only :

- domain must not depend on Spring/JPA/Web/infra/application;
- application must not depend on web/persistence/cache/batch/scheduler infra packages;
- infra.web must not depend on infra.persistence;
- controllers must reside in `..infra.web..`;
- JPA entities and repositories must reside in `..infra.persistence..`;
- handlers must reside in `..application.command.handler..` or `..application.query.handler..`;
- no `port.in` package unless ADR/allowlist;
- no raw repositories in controllers.

Phase 2 — P1:

- naming conventions for services;
- no `XxxService` vague in domain/application unless allowlisted;
- no cross-domain infra dependency;
- events/listeners package checks.

Phase 3 — cleanup:

- remove legacy bus method aliases;
- remove temporary ArchUnit ignores.
