# Migration Component Map

## Core/platform/common movement

| Current                                   | Target                   | Migration style                   |
| ----------------------------------------- | ------------------------ | --------------------------------- |
| `core.audit`                              | `platform.audit`         | direct or 2-step depending fan-in |
| `core.accesscontrol`                      | `platform.accesscontrol` | 3-PR bridge migration             |
| `core.tenantuser`                         | `platform.identity`      | 3/4-PR bridge migration           |
| `core.tenantconfig`                       | `platform.tenantconfig`  | 3-PR bridge migration             |
| `core.tenanttheme`                        | `platform.tenanttheme`   | direct or 2-step                  |
| `core.notification`                       | `platform.notification`  | migrate unless ADR exception      |
| `common.document` state/workflow          | `platform.document`      | direct if low fan-in              |
| `common.communication` state/workflow     | `platform.communication` | direct if low fan-in              |
| `common.idempotence` persistence/workflow | `platform.idempotence`   | split primitives vs persistence   |
| `common.security` app decisions           | `platform.accesscontrol` | split technical glue vs decisions |

## Keep in common

```text
Typed IDs
Bus interfaces
Context primitives
Paging primitives
Problem/exceptions primitives
Validation helpers
Cache abstractions
AfterCommit / tx helpers
Security technical glue
Idempotence annotations/key primitives
```

## Fan-in rule

For modules with more than 20 dependants or broad usage:

```text
PR1: create platform API bridge
PR2: flip imports gradually
PR3: move implementation
PR4: delete legacy package if needed
```

Measure fan-in using IDE search, `jdeps`, or grep/import counts before deciding PR shape.
