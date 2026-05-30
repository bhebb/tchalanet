# ai-safety

## Use when

You are about to:
- Run a shell command that modifies, removes, or pushes anything.
- Touch auth, RLS, permissions, secrets, env files, tokens, certs.
- Touch DB migrations, infra, Docker, runtime versions, dependencies.
- Cross a project boundary (slice change).
- Commit or push.

Load this skill before doing any of the above.

## Load

- This file.
- The relevant project `AGENTS.md` if a project is touched.
- `VERSIONS.md` if versions are touched.

## Do

- Stay inside the declared slice.
- Use targeted reads (path + line range) first.
- Ask for confirmation before any destructive or hard-to-reverse action.
- Stop after 3 failed attempts on the same problem.
- Log MCP activations in `.agents/mcp-activations.md`.
- Pointer rather than paraphrase when a rule already lives in `docs/` or `openspec/context/`.

## Do not

- Run `rm -rf`, `git reset --hard`, `git push --force`, `git clean -f`, `docker prune`, `docker volume rm` without explicit user approval.
- Skip hooks (`--no-verify`, `--no-gpg-sign`).
- Merge to `main`. Auto-merge anything.
- Modify `.env`, `.env.*`, `*.pem`, `*.key`, tokens, secrets.
- Modify DB migrations already applied. Modify auth / RLS / permissions.
- Bump dependency or runtime versions without an update to `VERSIONS.md`.
- Touch `docs/`, `tchalanet-*/docs/`, `tchalanet-*/openspec/`, `openspec/specs/` unless instructed.
- Touch `tchalanet-web/.agents/skills/` or `tchalanet-web/.claude/`.
- Run a global scan (`grep -R`, `find /`, `tree` at root) without an explicit reason.
- Create business/domain skills. Workflow skills only.

## Output

Before acting on a sensitive action, output one block:

```
Action: <what>
Slice: <project>
Files touched: <list>
Risk: <low|medium|high>
Reversible: <yes|no>
Approval needed: <yes|no — why>
```

If `Approval needed: yes`, wait for the user. Do not proceed.
