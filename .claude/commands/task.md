# /task

Use this command to execute exactly one bounded task.

Required input:

- Task ID or precise task title.
- Allowed files/folders.
- Files to inspect first.
- Forbidden modules.

Rules:

- Do only this task.
- Do not scan the whole repo.
- Do not load unrelated docs.
- Inspect only listed files first.
- Ask before expanding scope.
- Before editing, list files you will touch.
- Prefer local patterns.
- Stop after implementation and tests.

Output:

1. Files inspected
2. Files changed
3. Tests run
4. Risks
5. Compact handoff

Template:

Task:
<one precise task>

Can edit:

- ...

Must inspect first:

- ...

Do not touch:

- ...

Validation:

- ...
