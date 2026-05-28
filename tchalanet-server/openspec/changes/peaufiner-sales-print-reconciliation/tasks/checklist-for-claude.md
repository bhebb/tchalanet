# Checklist for Claude / coding agent

Before coding:

- Read `docs/ARCHITECTURE.md`, `docs/PLAYBOOK.md`, `docs/conventions/clean_architecture.md`, `docs/conventions/command_query_handlers.md`, `docs/conventions/inter_domain_calls.md`, `docs/conventions/batch.md`, `docs/conventions/timezone.md`, and `docs/conventions/idempotency.md`.

Do not:

- create `platform.reconciliation` that imports `core.*`;
- expose internal aggregates through public API;
- re-evaluate promotion rules during payout/reconciliation;
- use current pricing odds for expected outcome;
- silently repair sales or payout state during reconciliation;
- create duplicate anomalies on rerun.

Must:

- use typed IDs outside persistence;
- publish side effects after commit;
- keep scheduler/controller thin;
- use QueryBus for cross-core read models from `core.reconciliation`;
- keep CSV generated from persisted anomalies;
- include ticket public/display code in anomalies and CSV.
