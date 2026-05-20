# Design — Offline Sync Module

## Target module

```text
core.offlinesync
  api
    command
    query
    event
    model
  internal
    domain
    application
      command.handler
      query.handler
      port.out
      service
    infra
      web
      persistence
      event
      batch
      scheduler
      cache
      config
```

## Lifecycle

```text
RECEIVED
VALIDATED_TECHNICALLY
PROMOTED_TO_SALES
REJECTED
REQUIRES_REVIEW
DUPLICATE
EXPIRED
```

## Responsibility split

| Concern | Owner |
|---|---|
| payload hash/signature | `core.offlinesync` |
| offline grant validity | `core.offlinesync` / `platform.idempotence` if generic replay protection |
| terminal/outlet/session runtime validity | operational context resolver + `core.terminal/outlet/session` APIs |
| draw cutoff | `core.sales` / `core.draw` through sales command validation |
| pricing | `core.sales` |
| limits | `core.limitpolicy` via sales path |
| ticket creation | `core.sales` |
| notification of rejected submissions | `platform.notification` optional, via events |

## Promotion rule

Offline sync records a submission first. Promotion to a real ticket/sale happens only through a sales command/API after all technical and operational gates pass.
