# Tasks: Reconfigure OpenSpec per project

## 1. Create project-local OpenSpec directories

- [x] Create root OpenSpec if missing:

```text
openspec/
├── project.md
├── specs/
├── changes/
└── archive/
```

- [x] Create server OpenSpec:

```text
tchalanet-server/openspec/
├── project.md
├── specs/
├── changes/
└── archive/
```

- [x] Create web OpenSpec:

```text
tchalanet-web/openspec/
├── project.md
├── specs/
├── changes/
└── archive/
```

- [x] Create mobile OpenSpec:

```text
tchalanet-mobile/openspec/
├── project.md
├── specs/
├── changes/
└── archive/
```

- [x] Create edge OpenSpec:

```text
tchalanet-edge-service/openspec/
├── project.md
├── specs/
├── changes/
└── archive/
```

- [x] Add `.gitkeep` to empty directories where needed.

## 2. Add root agent rules

- [x] Create or update `CLAUDE.md` at workspace root.
- [x] Add project-local OpenSpec selection rule.
- [x] Add worktree/sandbox verification rule.
- [x] Add token discipline rule.
- [x] Add "do not modify multiple projects unless explicitly requested".
- [x] Add "do not manually archive OpenSpec with rm/cp/mv".

## 3. Add project-local agent rules

- [x] Create or update `tchalanet-server/CLAUDE.md`.
- [x] Create or update `tchalanet-web/CLAUDE.md`.
- [x] Create or update `tchalanet-mobile/CLAUDE.md`.
- [x] Create or update `tchalanet-edge-service/CLAUDE.md`.

Each project `CLAUDE.md` must include:

- [x] local OpenSpec path
- [x] stack summary
- [x] project responsibility
- [x] forbidden cross-project edits by default
- [x] mandatory context check commands
- [x] archive rule

## 4. Add project.md files

- [x] Add root `openspec/project.md` for cross-project coordination only.
- [x] Add `tchalanet-server/openspec/project.md`.
- [x] Add `tchalanet-web/openspec/project.md`.
- [x] Add `tchalanet-mobile/openspec/project.md`.
- [x] Add `tchalanet-edge-service/openspec/project.md`.

## 5. Move active changes to the right OpenSpec

- [x] Inventory current root `openspec/changes`.
- [x] Classify each change:

```text
server
web
mobile
edge
cross
```

- [x] Move server changes to `tchalanet-server/openspec/changes`.
- [x] Move web changes to `tchalanet-web/openspec/changes`.
- [x] Move mobile changes to `tchalanet-mobile/openspec/changes`.
- [x] Move edge changes to `tchalanet-edge-service/openspec/changes`.
- [x] Keep roadmap/cross-surface changes in root `openspec/changes`.

## 6. Validate

- [x] Validate root OpenSpec:

```bash
cd <workspace-root>
openspec validate --strict
```

- [x] Validate server OpenSpec:

```bash
cd tchalanet-server
openspec validate --strict
```

- [x] Validate web OpenSpec:

```bash
cd tchalanet-web
openspec validate --strict
```

- [x] Validate mobile OpenSpec:

```bash
cd tchalanet-mobile
openspec validate --strict
```

- [x] Validate edge OpenSpec:

```bash
cd tchalanet-edge-service
openspec validate --strict
```

## 7. Verify git tracking

- [x] Check ignored paths:

```bash
git check-ignore -v openspec || true
git check-ignore -v tchalanet-server/openspec || true
git check-ignore -v tchalanet-web/openspec || true
git check-ignore -v tchalanet-mobile/openspec || true
git check-ignore -v tchalanet-edge-service/openspec || true
```

- [x] Check git status:

```bash
git status --short
```

- [ ] Commit OpenSpec structure and rules.

## 8. Acceptance criteria

- [x] Every autonomous project has its own OpenSpec.
- [x] Root OpenSpec is cross-project only.
- [x] Agents know which OpenSpec to use.
- [x] Archive folders exist and are tracked.
- [x] Manual `rm/cp/mv` OpenSpec archiving is forbidden.
- [x] Worktree/sandbox verification is documented.
- [x] OpenSpec validates from root and each project.
