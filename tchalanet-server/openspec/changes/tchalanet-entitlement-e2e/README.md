# Tchalanet — Entitlement E2E & Integration OpenSpec ZIP

This package frames the next step after `platform.entitlement` compiles.

Use it to guide Python E2E work before broad mapping into business modules.

## Main flow

1. Validate plan seeds.
2. Validate subscription lifecycle.
3. Validate tenant capability snapshot.
4. Validate cache invalidation.
5. Validate quotas.
6. Validate features.
7. Integrate into onboarding/multitenant tests.
8. Update page generation and dashboards.
9. Progressively map service/handler entitlement checks.

## Key rule

Before creating a new API or handler, ask:

```text
Does this action depend on a paid feature or quota?
```

If yes, add entitlement gates and tests.
