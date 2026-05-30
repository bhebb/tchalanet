---
description: Scoped mobile task in tchalanet-mobile
---

Canonical skill: `.agents/skills/scoped-task/SKILL.md`
Project router: `tchalanet-mobile/AGENTS.md`
Default slice: mobile

Scope lock:
- Allowed: `tchalanet-mobile/`
- Forbidden unless explicitly requested: `tchalanet-server/`, `tchalanet-web/`, `tchalanet-edge-service/`, `tchalanet-infra/`, `tchalanet-docs/`

Load only:
1. `AGENTS.md`
2. `tchalanet-mobile/AGENTS.md`
3. `.agents/skills/scoped-task/SKILL.md`
4. files being edited/reviewed

Feature-first Flutter. Do not duplicate backend rules. Treat offline submissions as pending, never confirmed.
