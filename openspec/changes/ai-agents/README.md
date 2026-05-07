# Tchalanet AI Agents Pack

## Goal

Use small, specialized agents to implement or review one bounded piece at a time without burning tokens.

## Golden rule

```text
1 agent = 1 domain/feature/spec = 1 targeted task
```

Never ask an agent to review or refactor the whole backend.

## Mandatory Tchalanet rules

Every agent must respect:

- Java 25
- Spring Boot 4
- `CommandBus` / `QueryBus`
- typed IDs outside persistence
- `@CurrentContext` in controllers
- `ApiResponse<T>` for JSON success
- `ProblemDetail` for errors
- no JPA/domain leaks in controllers
- no handler direct injection in controllers
- no business logic in features
- no cross-domain writes inside critical transactions
- events after commit
- RLS-first persistence
- no raw `UUID` outside persistence

## Recommended workflow

```text
1. Pick one OpenSpec change
2. Ask implementation agent to plan file changes
3. Let it implement only that change
4. Compile/test
5. Ask review agent to review only that domain
6. Fix findings
7. /clear
8. Move to next domain
```

## Suggested folders

Claude Code:

```text
.claude/agents/
.claude/commands/
```

Codex:

```text
.codex/agents/
.codex/prompts/
```

Copilot:

```text
.github/copilot-instructions.md
.github/prompts/
```
