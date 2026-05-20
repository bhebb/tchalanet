# OpenSpec — Typed IDs aligned to Modulith (84)

> Status: NORMATIVE

## 1. Core rule

Outside persistence, use typed ID wrappers.

No raw `UUID` in public module APIs, commands, queries, events, domain models, feature models or platform API models.

## 2. Ownership

Typed ID wrappers live in `common` when shared by multiple modules.

Capability-specific IDs may live in the owning module API only if not reused elsewhere.

## 3. Mapping

Persistence maps UUID <-> typed IDs using central mappers or local mappers.

## 4. Migration rule

When moving packages, imports must preserve typed IDs. Do not temporarily replace typed IDs with UUID/String to make migration easier.
