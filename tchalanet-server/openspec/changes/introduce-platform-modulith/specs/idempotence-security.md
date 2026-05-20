# Spec — De-engraissement common.idempotence and common.security

## Goal

Keep common technical primitives but move application/persistent behavior to platform.

## Idempotence target

```text
common.idempotence
  annotations/interfaces only if pure

platform.idempotence
  persistence, records, replay policy, request hashing, admin/ops visibility
```

## Security target

```text
common.security
  pure technical primitives only: annotations, principal helper interfaces, constants if generic

platform.security or platform.accesscontrol
  permission evaluator implementation, role/permission policy, app-specific access decisions
```

## Migration tasks

- [ ] Inventory idempotence classes.
- [ ] Move DB-backed idempotency records to `platform.idempotence.internal.persistence`.
- [ ] Move idempotency service workflow to `platform.idempotence.internal.app`.
- [ ] Inventory security classes.
- [ ] Move app-specific permission evaluation to `platform.accesscontrol` or `platform.security`.
- [ ] Ensure `common` has no dependency on platform.

## Verification

- [ ] Idempotent endpoints still behave identically.
- [ ] Permission checks still work.
- [ ] `common` dependency graph remains clean.
