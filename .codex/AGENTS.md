# Codex - Tchalanet Light Context

## Mode

Work only on the files explicitly requested.
Do not scan the whole repository unless asked.
Before editing, inspect the smallest relevant set of files.
Prefer small diffs.

## Stack

- Backend: Java 25, Spring Boot 4, Maven
- Frontend: Nx, Angular 20, Angular Material, Ionic later
- DB: PostgreSQL + RLS
- Docs/spec: OpenSpec

## Architecture Rules

- common = technical only
- catalog = read-mostly reference data
- core = business truth, invariants, lifecycle
- features = UI/BFF orchestration
- Use CommandBus / QueryBus.
- Use Typed IDs outside persistence.
- No business logic in controllers.
- No raw UUID outside persistence.

## OpenSpec Rule

For new capabilities, architecture changes, or big refactors:

1. create/change OpenSpec proposal first
2. do not implement directly unless asked

## Coding Style

- Mobile-first Angular
- Signals + OnPush
- @if / @for
- CSS tokens, no hardcoded colors
- Backend: thin controllers, commands/queries, ProblemDetail errors
