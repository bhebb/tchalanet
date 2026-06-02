# /unit-test-task

Generate or update focused unit tests for a Tchalanet slice.

## Inputs

Expected format:

```text
/unit-test-task <slice> <target> [risk]
```

Examples:

```text
/unit-test-task backend core.sales SellTicketHandler cutoff and idempotency boundaries
/unit-test-task backend core.payout PayoutEligibilityPolicy payable/blocked transitions
/unit-test-task web tenant-admin users page ApiResponse and ProblemDetail display
/unit-test-task mobile pos sell form validation and provider state
```

## Context loading

Load:

1. root `AGENTS.md`;
2. project `AGENTS.md`;
3. `.agents/skills/testing-strategy/SKILL.md`;
4. one slice skill:
   - backend -> `backend-testing/SKILL.md`;
   - web -> `web-testing/SKILL.md`;
   - mobile -> `mobile-testing/SKILL.md`;
5. backend extras (load conditionally based on task target or args):
   - if "idempotency" or "idempotent" mentioned -> `backend-testing/idempotency.md`;
   - if "sell|payout|pos|operational" mentioned -> `backend-testing/pos-context.md`;
   - if "event|listener|projector" mentioned -> `backend-testing/events.md`;
   - if test data strategy unclear -> `java-test-data/SKILL.md`;
6. only files touched by the task.

## Required output

Produce:

1. risk being tested;
2. exact test class names;
3. scenario list;
4. dependencies to fake/mock/real;
5. fixture strategy;
6. files to create/update;
7. command to run.

## Rules

- Keep unit tests small and deterministic.
- Do not boot the full app unless required.
- Do not add e2e tests from this command.
- For Java, prefer explicit fixtures for business values and Instancio only for non-essential bulk data.
- For backend handlers, test the handler directly unless wiring is the target.
