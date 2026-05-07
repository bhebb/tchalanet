# Example prompts

## Implement core.session

```text
Use the Core Domain Creator agent.

Implement only:
openspec/changes/03-core-session-sales-session

Scope:
tchalanet-server/src/main/java/com/tchalanet/server/core/session/**

Do not scan unrelated domains.
Before editing, produce a file-change plan.
SalesSession is seller-scoped.
Current session is by current user, not terminal.
```

## Review core.terminal

```text
Use the Domain Reviewer agent.

Review only:
tchalanet-server/src/main/java/com/tchalanet/server/core/terminal/**

Against:
openspec/changes/02-core-terminal-runtime
docs/conventions/typed_ids.md
docs/conventions/web_api.md
docs/conventions/command_query_handlers.md
docs/conventions/rls.md

Return P0/P1/P2.
Do not scan unrelated domains.
```

## Generate tests for limitpolicy

```text
Use the Test Generator agent.

Add tests only for:
core.limitpolicy effective policy evaluation

Behaviors:
- tenant default applies
- outlet override wins
- user override wins
- REQUIRE_APPROVAL includes approval role
- terminal target is not supported in MVP

Use JUnit 5 + AssertJ and in-memory ports.
```
