# Change: Enforce Clean Architecture with ArchUnit and Bus Naming

## Why

Le backend doit éviter que les règles d’architecture restent uniquement documentaires. Les risques actuels :

- `domain` peut accidentellement dépendre de Spring, JPA, Web ou infra ;
- des controllers peuvent appeler repositories/adapters directement ;
- des handlers peuvent appeler l’infra d’un autre domaine ;
- les boundaries `domain/service` vs `application/service` restent ambiguës ;
- `port.in` peut être recréé alors que le projet utilise déjà CommandBus/QueryBus ;
- les méthodes de bus ne distinguent pas assez clairement command vs query ;
- les communications inter-domaines peuvent dériver vers des appels directs ou écritures cross-domain.

## What

- Ajouter une suite de tests ArchUnit `architecture` pour verrouiller les dépendances de packages.
- Standardiser la structure core :
  - `domain/model`
  - `domain/service`
  - `domain/event`
  - `domain/exception`
  - `application/command/model`
  - `application/command/handler`
  - `application/query/model`
  - `application/query/handler`
  - `application/port/out`
  - `infra/*`
- Confirmer la règle : pas de `port.in` par défaut ; CommandBus/QueryBus sont les ports d’entrée applicatifs.
- Renommer :
  - `CommandBus.send(...)` ou `handle(...)` → `CommandBus.execute(...)`
  - `QueryBus.send(...)` ou `handle(...)` → `QueryBus.ask(...)`
  - `CommandHandler.handle(...)` / `QueryHandler.handle(...)` restent inchangés.
- Documenter les règles de services :
  - `domain/service` = policy/calculator/rule pure, sans IO ;
  - `application/service` = orchestrator/assembler applicatif optionnel.
- Documenter les communications inter-domaines :
  - lecture simple = QueryBus ou reader/API stable minimale ;
  - effet métier = DomainEvent after-commit + listener idempotent ;
  - pas d’adapter persistence d’un aggregate étranger dans le domaine consommateur.
- Mettre à jour la documentation :
  - `clean_architecture.md`
  - `bus.md`
  - `inter_domain_calls.md`
  - `command_query_handlers.md`

## Impact

- Renommage mécanique des appels de bus.
- Possibles corrections de packages si les tests ArchUnit révèlent des violations.
- Les domaines legacy peuvent nécessiter des exceptions temporaires documentées dans les tests avec TODO.
- Aucun changement fonctionnel attendu si la migration est faite mécaniquement.

## Non-goals

- Refonte fonctionnelle de `draw`, `sales`, `payout` ou `drawresult`.
- Introduction d’un bus asynchrone externe.
- Introduction de `port.in`.
- Suppression immédiate de tout legacy sans analyse.
