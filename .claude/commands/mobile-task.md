# /mobile-task

Use for Tchalanet Flutter mobile tasks.

Default agent:

- mobile-slice-agent

Rules:

- Work under `tchalanet-mobile` only.
- Do one task only.
- Do not scan the whole repo.
- Do not invent backend endpoints.
- Do not modify backend/web/edge/infra unless explicitly requested.
- Do not add native plugins without approval.
- Explain Android/iOS permission changes.
- Run narrow Flutter validation.

Required input:

- Mobile area affected
- Task
- Files to inspect first
- Files/folders allowed to edit
- Validation command

Output:

1. Mobile area affected
2. Files inspected
3. Files changed
4. API assumptions
5. Native/plugin/permission impact
6. Validation command
7. Handoff

Template:

Area:

- sellprocess

Task:

- <one precise task>

Can edit:

- tchalanet-mobile/lib/...

Must inspect first:

- ...

Do not touch:

- tchalanet-server
- tchalanet-web
- tchalanet-edge-service
- tchalanet-infra

Validation:

- cd tchalanet-mobile && flutter analyze
