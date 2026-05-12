# Module core — Clean Architecture / Hexagonal / CQRS

## Rôle

`core` contient les domaines métier critiques de Tchalanet.

## Pattern

```text
core/<domain>/api/
  command/
  query/
  event/
  model/

core/<domain>/internal/
  domain/
  application/
    command/handler/
    query/handler/
    port/out/
    service/
  infra/
    persistence/
    web/
    event/
    batch/
    scheduler/
    cache/
    config/
```

## API publique Java

`api` expose :

```text
commands
queries
public integration/application events
read models
result models
```

`api` n’expose jamais :

```text
aggregates internes
JPA entities
repositories
handlers
ports out
controllers
cache adapters
```

## Règle CQRS

Writes : CommandBus.  
Reads : QueryBus.  
Events : after-commit.
