# OpenSpec — Bus Rules aligned to Modulith (82)

> Status: NORMATIVE

## 1. Bus ownership

`CommandBus` and `QueryBus` live in `common`.

They are technical in-process synchronous dispatchers.

## 2. Core usage

Core domains MUST use Command/Query messages for use-cases.

Public messages live in:

- `core.<domain>.api.command`
- `core.<domain>.api.query`

Handlers live in:

- `core.<domain>.internal.application.command.handler`
- `core.<domain>.internal.application.query.handler`

## 3. Platform usage

Platform MAY use CommandBus/QueryBus internally but is not required to.

Platform public APIs are usually service/facade interfaces under `platform.<capability>.api`.

## 4. Features usage

Features orchestrate core use-cases via CommandBus/QueryBus and platform/catalog APIs.

## 5. Catalog usage

Catalog does not require CommandBus/QueryBus by default.

Catalog uses `XCatalog` read APIs and internal admin services.
