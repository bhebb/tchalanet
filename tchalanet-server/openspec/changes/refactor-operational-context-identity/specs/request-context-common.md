# Spec — Request Context in common

## Requirement

`common.context` SHALL own request/runtime context primitives.

## Package

```text
com.tchalanet.server.common.context
com.tchalanet.server.common.web.context
```

## Contains

- `TchRequestContext`
- `TchContext`
- `TchContextResolver`
- HTTP binder/filter integration if generic
- `@CurrentContext` and argument resolver in `common.web.context`
- batch/startup/system binding primitives

## Must not contain

- user profile persistence;
- tenant membership persistence;
- permission decisions;
- terminal/outlet/session validation;
- business rules.

## Acceptance criteria

- `common.context` compiles without depending on platform/core/catalog/features.
- Controllers can still inject `@CurrentContext TchRequestContext`.
- Batch/system jobs can bind explicit context without HTTP.
