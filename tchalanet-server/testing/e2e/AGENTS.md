# AGENTS.md — E2E (testing/e2e)

**Working on E2E tests? Start here:**

```bash
cd tchalanet-server/testing/e2e
bash scripts_agent_run.sh agent
```

That script is the operational entry point for agents. It sets the local
Firebase/Traefik defaults and runs the current canonical check sequence. Read
[`README.md`](./README.md) for stack mechanics and
[`docs/business-day-scenarios.md`](./docs/business-day-scenarios.md) for the
single scenario truth. Do not invent or run ad hoc scenario matrices.

Current traps that waste the most time:

1. **Do not hand-build pytest commands first.** Run `bash scripts_agent_run.sh agent`.
2. **`external_identity.not_linked` means Firebase bootstrap drift.** Recreate/restart the API
   with Firebase bootstrap enabled and `TCH_IDENTITY_FIREBASE_BOOTSTRAP_USERS=superadmin,admin`.
3. **Code changes require a recreated API container.** A stale image/container can make E2E
   results meaningless; verify `tchl-api-dev` is healthy and uses the expected local image.

Quickstart, env vars, markers, edge/Slack verification, and a troubleshooting table are all
in the README. Do not duplicate that content here — update the README instead.
