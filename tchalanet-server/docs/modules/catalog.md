# Module catalog — Simple DDD / Reference Catalog

## Rôle

Référentiels, lookup data, registries et configuration read-mostly.

## Pattern

```text
catalog/<name>/api/
  XxxCatalog.java
  model/

catalog/<name>/internal/
  app/ or read/
  write/
  persistence/
  mapper/
  web/
  cache/
```

## Règle

`catalog` peut être domain-driven simple : noms explicites, concepts métier, validations structurelles, modèles immutables. Mais il ne porte pas de lifecycle métier critique, pas d’orchestration de workflows, pas d’events métier.

## Exemples

```text
catalog.theme          theme presets
catalog.settings       definitions/defaults metadata
catalog.resultslot     result slots
catalog.game           game definitions
catalog.pricing        pricing definitions/templates
```
