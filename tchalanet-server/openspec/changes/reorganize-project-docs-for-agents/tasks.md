# Tasks — Reorganize Docs for Agents

## 1. Create agent docs

- [ ] Create `docs/agents/AGENT_PLAYBOOK.md`.
- [ ] Create `docs/agents/DECISION_TREE.md`.
- [ ] Create `docs/agents/CHECKLISTS.md`.

## 2. Create module docs

- [ ] Add `MODULE.md` template for common/catalog/platform/core/features.
- [ ] Add module-local `MODULE.md` to complex modules as they are migrated.
- [ ] Add `AGENTS.md` only for complex/high-risk modules.

## 3. Clean existing docs

- [ ] ADRs contain decisions only, not mutable module inventories.
- [ ] Move inventories to `docs/reference/*`.
- [ ] Ensure references use current names: `platform.identity`, not the former user-context naming.

## 4. Update triggers

Docs must be updated when:

- package/module is renamed;
- public API changes;
- ArchUnit/Modulith rule changes;
- persistence table/entity/view changes;
- route/controller contract changes;
- migration status changes.

## 5. Verification

- [ ] No obsolete `usercontext` references except deprecated pointer docs.
- [ ] Each migrated platform capability has a short module README/MODULE.md.
- [ ] OpenSpec change includes tasks/specs/design.
