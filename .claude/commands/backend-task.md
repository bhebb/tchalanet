---
description: Scoped backend task in tchalanet-server
---

Canonical skill: `.agents/skills/scoped-task/SKILL.md`
Project router: `tchalanet-server/AGENTS.md`
Default slice: backend

Scope lock:
- Allowed: `tchalanet-server/`
- Forbidden unless explicitly requested: `tchalanet-web/`, `tchalanet-mobile/`, `tchalanet-edge-service/`, `tchalanet-infra/`, `tchalanet-docs/`

Load only:
1. `AGENTS.md`
2. `tchalanet-server/AGENTS.md`
3. `.agents/skills/scoped-task/SKILL.md`
4. files being edited/reviewed

Do NOT load testing skills unless:
- task explicitly mentions "test" or "coverage"
- `/test-ready-check` is called before PR

Backend rules live in `tchalanet-server/docs/` and `openspec/context/` — read by pointer, do not restate here.
