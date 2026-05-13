# AI Agent Playbook

## Before editing

- Identify the layer and module archetype.
- Read module `MODULE.md` if present.
- Do not import another module's `internal` package.
- Prefer public APIs: `core.<x>.api`, `platform.<x>.api`, `catalog.<x>.api`.

## During migration

- Move boundaries first, then refactor implementation in a separate PR.
- Keep handlers temporarily if needed; do not rewrite everything at once.
- Use bridge APIs for high fan-in modules.
- Avoid `Object` in public APIs.
- Avoid `UUID` outside persistence.

## Verification

Targeted:

```bash
./mvnw -pl tchalanet-platform -am verify
./mvnw -pl tchalanet-core -am verify
```

Before merge:

```bash
./mvnw clean verify
```

Check:

- ArchUnit OK;
- Spring Modulith verification OK;
- no forbidden imports;
- tests updated;
- docs updated if public contract changed.
