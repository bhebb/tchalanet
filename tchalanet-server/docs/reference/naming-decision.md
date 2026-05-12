# Naming Decision — `platform`

## Decision

Use `platform` as the Java/backend layer and Maven module name.

```text
com.tchalanet.server.platform.*
tchalanet-platform
```

## Required distinction

```text
platform/ package layer
  = internal Java/backend layer for transversal application service modules

/api/v1/platform/**
  = HTTP platform admin scope, generally SUPER_ADMIN-facing
```

## Documentation vocabulary

Use these terms consistently:

| Concept            | Preferred wording                                          |
| ------------------ | ---------------------------------------------------------- |
| Java package/layer | `platform layer`, `platform module`, `platform capability` |
| HTTP route scope   | `platform admin scope`                                     |
| Route prefix       | `/api/v1/platform/**`                                      |

Avoid writing “platform endpoint” without qualifying whether it means Java platform module or HTTP platform admin scope.

## Rationale

`platform` is concise, readable, and reflects the role of transversal application services.
The ambiguity with the HTTP scope is accepted and controlled through documentation vocabulary.
