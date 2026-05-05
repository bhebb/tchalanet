# Design: AI-agent context organization

## Current problem

AI guidance is spread across multiple files and folders. Examples:

```text
.claude/
.codex/
.copilot/
.agents/
AGENTS.md
openspec/context/
component prompts
```

Risks:

- duplicated instructions;
- obsolete commands;
- agents loading too much context;
- global files conflicting with component files;
- project-specific decisions hidden in old prompts.

## Target model

Use a router + component ownership model.

```text
repo-root/
├── AGENTS.md                         # global short router
├── openspec/context/                 # short context packs
├── tchalanet-server/AGENTS.md         # backend-specific instructions
├── tchalanet-web/AGENTS.md            # web-specific instructions
├── tchalanet-mobile/AGENTS.md         # mobile-specific instructions
├── tchalanet-edge-service/AGENTS.md   # edge-specific instructions
├── tchalanet-infra/AGENTS.md          # infra-specific instructions
└── tchalanet-docs/AGENTS.md           # docs-specific instructions
```

Tool-specific folders may exist but must point to the same canonical rules.

```text
.claude/
.codex/
.copilot/
.agents/
```

These should contain only:

- tool-specific startup prompts;
- command shortcuts;
- minimal routing rules;
- links to canonical `AGENTS.md` and `openspec/context`.

## Global AGENTS.md

Global `AGENTS.md` should be short.

It should contain:

- repository map;
- rule to read `VERSIONS.md`;
- rule to load only required OpenSpec context packs;
- rule to prefer component docs near code;
- rule to avoid editing unrelated components;
- rule to validate before broad rewrites.

It should not contain:

- full backend rules;
- full frontend rules;
- full infra rules;
- old implementation details;
- archived decisions.

## Component AGENTS.md

Each component may have its own `AGENTS.md`.

Example ownership:

| Component | File |
| --- | --- |
| Backend | `tchalanet-server/AGENTS.md` |
| Web | `tchalanet-web/AGENTS.md` |
| Mobile | `tchalanet-mobile/AGENTS.md` |
| Edge | `tchalanet-edge-service/AGENTS.md` |
| Infra | `tchalanet-infra/AGENTS.md` |
| Docs | `tchalanet-docs/AGENTS.md` |

Component agent files should include:

- local commands;
- local OpenSpec path;
- local docs path;
- allowed workflows;
- validation commands;
- task-specific constraints.

## Context loading rule

For a task, an agent should load:

```text
mandatory:
- openspec/context/10-non-negotiables.md
- VERSIONS.md

then:
- at most one technical pack
- at most one domain pack
- component AGENTS.md
```

Target: 2-4 packs, not everything.

## AI file inventory

Create a generated inventory:

```text
build/ai-agent-files-inventory.json
tchalanet-docs/docs/99-reference/ai-agent-files-inventory.md
```

Scan for:

```text
AGENTS.md
*.prompt.md
.claude/**
.codex/**
.copilot/**
.agents/**
CLAUDE.md
CODEX.md
COPILOT.md
```

Classify:

```text
CANONICAL
TOOL_ROUTER
COMPONENT_SPECIFIC
DUPLICATE
OBSOLETE
ARCHIVE
UNKNOWN
```

## Archive policy

Do not delete first.

Move obsolete AI files to:

```text
<component>/docs/archive/ai-agents/
```

or:

```text
.ai-archive/
```

after review.

## Tool-specific guidance

### Claude

Claude should load:

- root `AGENTS.md`;
- component `AGENTS.md`;
- minimal OpenSpec context packs;
- active change spec.

Claude should not load all docs unless explicitly auditing docs.

### Codex

Codex should load:

- root `AGENTS.md`;
- component `AGENTS.md`;
- exact task prompt;
- minimal OpenSpec context packs.

Codex should run targeted checks, not broad rewrites.

### Copilot

Copilot should use short repository instructions only.

Copilot-specific files should avoid long architecture docs and instead link to canonical docs.

## Acceptance criteria

- AI-agent files are inventoried.
- Duplicates and obsolete files are listed.
- Root `AGENTS.md` becomes a lightweight router.
- Component `AGENTS.md` files exist or are proposed.
- Tool-specific folders do not duplicate long rules.
- Old AI instructions are archived, not silently deleted.
