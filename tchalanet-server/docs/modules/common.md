# Module common — Technical Shared Kernel

## Rôle

`common` contient uniquement des primitives techniques partagées par tous les autres modules.

## Autorisé

```text
common.bus
common.types.id
common.context primitives
common.web.api
common.web.paging
common.problem
common.cache abstractions
common.tx AfterCommit / transaction helpers
common.validation generic validators
common.stereotype annotations
```

## Interdit

```text
business audit
access control rules
role assignment persistence
user profile / app user
communication delivery
message templates métier/applicatifs
document metadata workflow
tenant config values
tenant theme overrides
idempotency persistence/workflow
```

## Migration rule

Si une classe `common` possède une table, une policy applicative, un workflow, un adapter externe ou une notion utilisateur/tenant métier : elle doit probablement migrer vers `platform`.
