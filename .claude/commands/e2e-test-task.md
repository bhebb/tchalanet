# /e2e-test-task

Design or implement a Tchalanet end-to-end test flow.

## Inputs

Expected format:

```text
/e2e-test-task <slice> <flow> [constraints]
```

Examples:

```text
/e2e-test-task backend-api cashier onboarding terminal session sell verify payout
/e2e-test-task web tenant admin onboarding dashboard navigation
/e2e-test-task mobile pos login bind terminal sell preview
```

## Context loading

Load:

1. root `AGENTS.md`;
2. project `AGENTS.md`;
3. `.agents/skills/testing-strategy/SKILL.md`;
4. relevant slice skill;
5. existing test fixtures/scripts;
6. touched files only.

## Required output

Produce:

1. user journey;
2. prerequisites/fixtures;
3. tenant/user/role/permission setup;
4. operational context setup if POS;
5. API/UI steps;
6. assertions;
7. cleanup strategy;
8. command to run.

## Backend API E2E POS minimum

For POS/cashier flows, include:

- tenant exists;
- cashier/seller exists;
- outlet exists;
- terminal exists and is bound/trusted;
- session is open;
- permission allows action;
- operational context is trusted;
- sell or payout action succeeds;
- negative test for missing/invalid context.

## Rules

- Do not mix performance/load tests with e2e.
- Do not duplicate every backend business unit test.
- Use stable seeded test data.
- Test multi-tenant isolation for critical flows.
- Name the command that proves the e2e passes.
