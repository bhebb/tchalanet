# AI Context Loading

Agents should load the smallest useful context for the task.

Mandatory starting point:

- `AGENTS.md`
- `VERSIONS.md`
- `openspec/context/00-index.md`
- `openspec/context/10-non-negotiables.md`

Then load only what is relevant:

- one component `AGENTS.md`
- at most one technical context pack
- at most one domain context pack
- near-code docs for the touched component

Avoid:

- loading all OpenSpec packs;
- copying component instructions into root prompts;
- using archived instructions as active context;
- deleting old AI files without an archive step.
