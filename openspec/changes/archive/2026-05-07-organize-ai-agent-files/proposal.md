# Change: Organize AI agent files and context

## Why

The repository has accumulated AI-agent files and folders such as:

- `.claude`
- `.codex`
- `.copilot`
- `.agents`
- `AGENTS.md`
- local prompts
- old instructions
- component-specific AI context files

Some of these files are obsolete or duplicated. This increases token usage and causes agents to follow outdated instructions.

The goal is to make AI-agent guidance explicit, lightweight, and component-owned.

## What

This change will:

- inventory all AI-agent configuration and prompt files;
- identify obsolete and duplicated AI instructions;
- define a global AI-agent router;
- move detailed instructions near each component;
- define what Claude, Codex, Copilot, and generic agents should load;
- create an archive policy for old instructions;
- reduce global context size.

## Impact

- Documentation and configuration organization only.
- No runtime behavior change.
- AI agents should consume less irrelevant context.
- Some files may be moved to archive after review.
- Component-specific instructions become clearer.

## Non-goals

- Do not delete AI-agent files without inventory and archive plan.
- Do not force all agents to use the same file format.
- Do not copy full component docs into global agent prompts.
- Do not make global context huge again.

## Principles

- Global agent context is a router.
- Component agent context owns detailed component instructions.
- Stale instructions are worse than missing instructions.
- Agents should load 2-4 context packs max for a task.
- If an instruction conflicts with `10-non-negotiables.md`, the non-negotiable wins.
