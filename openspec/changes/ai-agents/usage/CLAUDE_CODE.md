# Using these agents in Claude Code

## Option A — Project agents

Copy files:

```bash
mkdir -p .claude/agents
cp agents/*.md .claude/agents/
```

Then ask Claude Code:

```text
Use the Domain Reviewer agent.
Review only core/session against openspec/changes/03-core-session-sales-session and 03b-core-session-draw-lifecycle-auto-open-close.
Do not scan unrelated domains.
```

## Option B — Slash commands

Create commands:

```bash
mkdir -p .claude/commands
```

Example `.claude/commands/review-domain.md`:

```text
Review only $ARGUMENTS against its OpenSpec.
Use AGENTS.md, VERSIONS.md, ARCHITECTURE.md, PLAYBOOK.md and relevant conventions.
Return P0/P1/P2.
Do not scan unrelated domains.
```

Use:

```text
/review-domain core/session openspec/changes/03-core-session-sales-session
```

## Token tips

- Use `/clear` after each domain.
- Give only one OpenSpec change at a time.
- Ask for a file-change plan before edits.
- Never say “review the backend”.
