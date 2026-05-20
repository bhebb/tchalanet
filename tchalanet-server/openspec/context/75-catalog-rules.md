# OpenSpec — Catalog Rules (75)

## Status

NORMATIVE.

## 1. Definition

`catalog` contains reference data modules: stable identifiers, lookup data, configuration definitions, registries, read-mostly datasets.

Catalogs are Simple DDD / Reference Catalog modules.

## 2. Structure

```text
catalog/<name>/api/
  XxxCatalog.java
  model/
    XxxView.java
    XxxSummaryView.java
    XxxSearchCriteria.java

catalog/<name>/internal/
  app/ or read/
  write/
  persistence/
  mapper/
  web/
  cache/
```

## 3. Public contract

Only `catalog.<name>.api` is importable by other modules.

## 4. Responsibilities

Catalogs may:

- expose read-only APIs;
- own reference tables;
- provide admin CRUD;
- cache read-mostly data;
- map entities to immutable views.

Catalogs must not:

- own business lifecycle rules;
- emit domain events;
- orchestrate workflows;
- depend on core/platform/features;
- expose internal repositories/entities.

## 5. Domain-driven simple

Catalog modules use domain language and explicit models, but avoid full Clean Architecture ceremony unless justified.

## 6. Examples

```text
catalog.theme       theme presets
catalog.settings    setting definitions/default metadata
catalog.resultslot  result slots
catalog.game        game definitions
catalog.pricing     pricing templates/reference odds
```
