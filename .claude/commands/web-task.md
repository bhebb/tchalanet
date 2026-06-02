---
description: Scoped web task in tchalanet-web
---

Canonical skill: `.agents/skills/scoped-task/SKILL.md`
Project router: `tchalanet-web/AGENTS.md`
Web skills: `tchalanet-web/.agents/skills/` (`angular-developer`, `nx-workspace`, `nx-generate`, ...)
Default slice: web

Scope lock:
- Allowed: `tchalanet-web/`
- Forbidden unless explicitly requested: `tchalanet-server/`, `tchalanet-mobile/`, `tchalanet-edge-service/`, `tchalanet-infra/`, `tchalanet-docs/`

Load only:
1. `AGENTS.md`
2. `tchalanet-web/AGENTS.md`
3. one relevant web skill
4. files being edited/reviewed

Do NOT load testing skills unless:
- task explicitly mentions "test" or "coverage"
- `/test-ready-check` is called before PR

The web still moves: do not invent UI/routing/lib structure that does not exist. Prefer documented, stable directions.
