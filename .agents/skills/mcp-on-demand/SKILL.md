# mcp-on-demand

## Use when

A task might benefit from an MCP server (GitHub, Nx Cloud, etc.), or you are reviewing which MCPs are active.

## Rule

No permanent MCP by default. An MCP is activated only when the current task needs it, and deactivated after use when practical. Monthly audit: any MCP unused for 2 weeks is deactivated.

## Load

- This file.
- `.agents/mcp-activations.md` (the activation log).

## Do

- Activate an MCP only for the task that requires it.
- Log every activation in `.agents/mcp-activations.md` (Date / MCP / Task / Deactivation due).
- Prefer the narrowest MCP scope for the task.
- Deactivate after the task when practical.

## Status reference

- GitHub MCP: available, off by default. Activate for PR/issue work.
- Nx Cloud MCP: on only while web/Nx work is active.
- Trello / Slack / Filesystem MCP: not enabled at this stage.

## Do not

- Leave an MCP enabled "just in case".
- Activate a broad-surface MCP (e.g. filesystem) without explicit need and approval.

## Output

When activating, append one row to `.agents/mcp-activations.md` and state which MCP and why.
