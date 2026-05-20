# Agent Task Template

## Agent

`<agent-name>`

## Target OpenSpec

`openspec/changes/<change-name>`

## Scope allowed

```text
<paths>
```

## Scope forbidden

```text
Do not scan or edit unrelated domains.
Do not cleanup the whole repository.
```

## Required docs

```text
AGENTS.md
VERSIONS.md
docs/ARCHITECTURE.md
docs/PLAYBOOK.md
docs/conventions/<needed>.md
```

## Task

```text
<exact task>
```

## Expected output

```text
1. file-change plan
2. implementation
3. tests or test plan
4. list of intentionally untouched items
```
