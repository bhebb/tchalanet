---
description: Review open Dependabot PRs and merge safe ones (patch/minor, green CI, no breaking change)
---

Slice: cross-project (read-only review, merge only if safe).
Safety: `.agents/skills/ai-safety/SKILL.md` applies. Never merge a Dependabot PR that touches auth, RLS, migrations, or a major version bump.

## Steps

1. **List** all open Dependabot PRs via GitHub MCP (`list_pull_requests`, filter `dependabot/`).
2. **Classify each PR**:
   - ✅ SAFE to merge: patch or minor bump, CI green, no `VERSIONS.md` impact, no breaking change noted in PR body.
   - ⚠️ REVIEW: minor bump with behaviour changes, or CI unknown.
   - 🔴 HOLD: major version bump, touches auth/RLS/migrations, or CI red.
3. **Merge** all SAFE PRs one by one via `merge_pull_request` (merge commit, not squash).
4. **Report** in terminal + send summary to `#tchalanet-agents` (`C0B76AV9WAW`):

```
📦 Dependabot review — <date>
✅ Merged: <list PR# title>
⚠️  Needs review: <list PR# + reason>
🔴 On hold: <list PR# + reason>
```

## Do not

- Merge a PR with red or unknown CI.
- Merge a major version bump without explicit human approval.
- Merge anything touching `tchalanet-infra/` without dry-run confirmation.
- Merge more than 5 PRs in one run without a pause for human review.
