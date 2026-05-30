---
description: Pre-PR review of current changes (read-only by default)
---

Canonical skill: `.agents/skills/pr-readiness/SKILL.md`

Read-only by default. If a fix is needed, ask first.

Check: scope, no secrets in diff, no version bump without `VERSIONS.md`, no unjustified dependency, tests updated, durable docs updated only if a durable rule changed, project boundaries respected. Run the touched project's validation command (see its `AGENTS.md`).

Output: Recommendation (Ship / Not yet), Blockers, Important non-blocking, Missing tests, Overall risk (Low/Medium/High).
