# Design — Documentation Layout

## Target documentation tree

```text
docs/
  adr/
  architecture/
  conventions/
    common/
    catalog/
    platform/
    core/
    features/
    persistence/
    web/
  reference/
    platform-modules.md
    module-map.md
    naming-decision.md
  agents/
    AGENT_PLAYBOOK.md
    DECISION_TREE.md
    CHECKLISTS.md

openspec/
  context/
  changes/

src/main/java/com/tchalanet/server/<layer>/<module>/
  README.md or MODULE.md
  AGENTS.md optional for complex modules
```

## Rule

Stable decisions live in ADRs. Living inventories live in reference docs. Module-local instructions live next to the module.
