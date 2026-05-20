# Using these agents with Codex

Codex may not have named agents in the same way, so use the agent prompt as a task header.

## Pattern

```text
You are the <AGENT_NAME> agent for Tchalanet.

<PASTE_AGENT_RULES>

Task:
<YOUR_SPECIFIC_TASK>

Scope:
<ONLY_PATHS_ALLOWED>

Do not scan unrelated domains.
```

## Example

```text
You are the Reader Port Implementation agent for Tchalanet.

Implement reads only for core.terminal.

Needed queries:
- GetTerminalByIdQuery
- ListTerminalsQuery
- ListTerminalsByOutletQuery
- GetActiveTerminalForUserQuery

Needed endpoints:
- GET /admin/terminals
- GET /admin/terminals/{terminalId}
- GET /tenant/terminals/current

Only inspect:
- tchalanet-server/src/main/java/com/tchalanet/server/core/terminal/**
- common web/paging/types if needed
- openspec/changes/02-core-terminal-runtime

Do not scan unrelated domains.
```

## Cost control

- Ask Codex for a plan first.
- Approve only focused changes.
- Avoid broad “fix compile for repo” unless final cleanup.
