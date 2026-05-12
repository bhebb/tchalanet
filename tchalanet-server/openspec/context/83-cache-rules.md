# OpenSpec — Cache Rules aligned to Modulith (83)

> Status: NORMATIVE

## 1. Cache as infra optimization

Cache is never a source of truth.

## 2. Location

Cache specs and implementation live in module internals:

- `catalog.<x>.internal.cache`
- `platform.<x>.internal.cache`
- `core.<x>.internal.infra.cache`

Common may define cache abstractions and shared key helpers.

## 3. Public API restriction

Public `api` packages must not expose cache implementation details, Redis keys or Caffeine types.

## 4. Eviction

Eviction after writes must happen after transaction commit where the write is transactional.

## 5. Migration rule

When moving a component to platform, move its cache names/spec provider with it unless the cache is purely low-level common infrastructure.
