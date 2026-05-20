# OpenSpec — Persistence Rules aligned to Modulith (77)

> Status: NORMATIVE

## 1. Persistence location

Persistence belongs in module internals:

- `catalog.<name>.internal.persistence`
- `platform.<capability>.internal.persistence`
- `core.<domain>.internal.infra.persistence`

No feature may access repositories or JPA entities.

## 2. Public API restriction

No JPA entity, repository, JDBC row mapper or persistence adapter may appear in an `api` package.

## 3. UUID rule

Raw `UUID` is allowed only in persistence, SQL, low-level external payload mapping and explicit low-level keys.

Public module APIs use typed IDs.

## 4. RLS

Tenant-scoped tables use `tenant_id` and PostgreSQL RLS.

Read-side Java code must not reimplement tenant isolation.

## 5. Flyway

Pre-go-live migrations should be absorbed into original migration files unless the user explicitly approves a new `V*.sql`.

When moving tables to platform, update:

- JPA entities;
- audit tables;
- read views;
- seeds;
- RLS policies;
- module package names.

## 6. Views

When a table changes, verify all read-model SQL views that depend on it.
