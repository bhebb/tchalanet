# OpenSpec — Feature Rules (81)

## Status

NORMATIVE.

## 1. Definition

`features` contains UI-oriented vertical slices / BFF endpoints.

Features exist because there is a screen, flow, menu entry, dashboard, public page, wizard, or UI composition need.

## 2. Structure

```text
features/<feature>/<slice>/
  web/
  app/
  model/
  mapper/
  dynamic/
  shared/
```

Apply the Rule of 3: create role packages only when they contain at least 3 elements.

## 3. Public contract

Features expose HTTP/OpenAPI contracts, not Java APIs.

No `features/<feature>/api` package by default.

## 4. Dependency rules

Features may depend on:

- common;
- catalog APIs;
- platform APIs;
- core APIs/commands/queries/events.

Features must not:

- be imported by any other module;
- expose Java APIs;
- contain business invariants;
- own persistence;
- access repositories/entities;
- import any other module's `internal` package.

## 5. Shared feature logic

If two features need the same logic, extract it to the appropriate owner:

```text
core.<domain>.api          for business truth
platform.<capability>.api  for transversal application service
catalog.<name>.api         for reference data
common                     only for pure technical primitives
```
