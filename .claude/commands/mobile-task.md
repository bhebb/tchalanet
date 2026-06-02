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

Do NOT load testing skills unless:
- task explicitly mentions "test" or "coverage"
- `/test-ready-check` is called before PR

Feature-first Flutter. Do not duplicate backend rules. Treat offline submissions as pending, never confirmed.
