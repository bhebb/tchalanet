# Tchalanet E2E

End-to-end tests for the Tchalanet backend. Runs against a live server (default `http://localhost:8083/api/v1`).

## Run

```bash
cd tchalanet-server/tests/e2e
uv sync                                # or: pip install -e .
uv run pytest tests/cashier            # cashier happy path
uv run pytest tests/tenant_onboarding  # tenant onboarding (later)
```

## Configuration

Read from `tchalanet-server/scripts/.env.local`. Required keys:

- `TCH_BASE_URL`
- `TCH_KEYCLOAK_TOKEN_URL`, `TCH_KEYCLOAK_CLIENT_ID` (default `tchalanet-swagger`), `TCH_KEYCLOAK_CLIENT_SECRET`
- `TCH_SUPER_ADMIN_USERNAME`, `TCH_SUPER_ADMIN_PASSWORD`
- `TCH_SELLER_USERNAME`, `TCH_SELLER_PASSWORD`
- `TCH_TENANT_CODE`, `TCH_OUTLET_ID`, `TCH_TERMINAL_ID`
- `TCH_STAKE_CENTS` (default `100`)

Optional for receipt send:

- `TCH_TEST_SLACK_CHANNEL_KEY`

## Layout

```
e2e/
├── lib/         # HTTP client, auth, context, seed IDs
├── prereqs/     # idempotent setup steps (sync app_user, generate draws, open session...)
├── flows/       # business orchestrators per actor (cashier today; tenant_admin / platform_admin later)
└── tests/
    ├── cashier/             # cashier journey
    └── tenant_onboarding/   # added when we onboard new tenants
```
