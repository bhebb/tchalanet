# Spec — Sell Controller

## Requirement

Sell controller SHALL remain thin and delegate operational validation and sales command execution.

## Rules

- Use `@CurrentContext TchRequestContext`.
- Read terminal/outlet/session ids from request/header as defined by API contract.
- Call resolver or build command that causes resolver to run.
- Dispatch to sales command/API.
- Return API response mapped from sales result.
- No repository access.
