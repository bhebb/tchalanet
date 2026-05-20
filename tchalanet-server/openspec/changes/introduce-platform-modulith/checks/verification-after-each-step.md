# Verification After Each Migration Step

## Targeted validation

Use targeted module verification during development:

```bash
./mvnw -pl tchalanet-platform -am verify
./mvnw -pl tchalanet-core -am verify
./mvnw -pl tchalanet-features -am verify
```

## Full validation before merge

```bash
./mvnw clean verify
```

## Required checks

- [ ] Java compilation passes.
- [ ] Unit tests pass.
- [ ] Integration tests impacted by the migration pass.
- [ ] Spring Modulith verification passes or has documented legacy allowlist.
- [ ] ArchUnit passes or has documented legacy allowlist.
- [ ] Flyway migration validation passes if persistence moved.
- [ ] `ddl-auto=validate` passes if JPA entities moved.
- [ ] No accidental `/api/v1/platform/**` route change unless explicitly intended.
- [ ] No new direct import of another module's `internal` package.
