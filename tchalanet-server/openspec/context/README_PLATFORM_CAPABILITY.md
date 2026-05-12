# Platform Capability Template

Replace `<capability>` with the actual module name.

```text
platform/<capability>/
  api/
    <Capability>Api.java
    model/
  internal/
    service/
    persistence/
    web/
    event/
    adapter/
    cache/
    config/
```

## Rules

- Public Java contract lives in `api/`.
- Implementation lives in `internal/`.
- Other modules must not import `internal/`.
- The module must not depend on `core` or `features`.
- Direct dependency on another platform capability requires ADR exception.
- Default transaction behavior: join caller transaction.
- Document any `REQUIRES_NEW` or async behavior.

## README checklist

- [ ] Purpose of capability.
- [ ] Public API types.
- [ ] Persistence tables, if any.
- [ ] Transaction behavior.
- [ ] Context/RLS behavior.
- [ ] Events consumed/published.
- [ ] External adapters, if any.
- [ ] Caches and TTL specs, if any.
