# Observability

## Stack

| Signal  | Tool               | Protocol | Destination        |
|---------|--------------------|----------|--------------------|
| Traces  | Spring Boot OTel   | OTLP/HTTP | Grafana Cloud Tempo |
| Metrics | Micrometer OTLP    | OTLP/HTTP | Grafana Cloud Prometheus |
| Logs    | Spring Boot OTel   | OTLP/HTTP | Grafana Cloud Loki |

**Local dev**: Jaeger (traces) via `docker-compose-otel.yml` on ports 4317/4318. No cloud export.

**Staging/Prod**: Grafana Cloud via the `grafana-cloud` Spring profile.

## Grafana Cloud

- **Dashboard**: https://silverkiwi1488.grafana.net
- **OTLP gateway**: `https://otlp-gateway-prod-ca-east-0.grafana.net/otlp`
- **Instance ID**: 1723074
- **Free tier**: 50 GB/month (traces + metrics + logs combined)

### Activation

The `grafana-cloud` profile is added to `SPRING_PROFILES_ACTIVE` in staging and production compose env files:
- `tchalanet-infra/envs/staging/compose.env`
- `tchalanet-infra/envs/prod/compose.env`

Each runtime also sets an explicit OTel service name so traces are easy to find in Tempo:

| Environment | `API_OTEL_SERVICE_NAME` |
|-------------|--------------------------|
| dev | `tchalanet-api-dev` |
| staging | `tchalanet-api-staging` |
| prod | `tchalanet-api-prod` |

### Secrets

| Secret | Location | Description |
|--------|----------|-------------|
| `GRAFANA_OTLP_AUTH_HEADER` | Doppler (`dev`, `stg`, `prd`) | `Basic <base64(instanceId:apiKey)>` |

The API container receives the secret and non-secret OTel settings via `docker-compose-api.yml` environment variables.

Important runtime variables passed explicitly:

| Env var | Purpose |
|---------|---------|
| `GRAFANA_OTLP_ENDPOINT` | Base OTLP endpoint, default `https://otlp-gateway-prod-ca-east-0.grafana.net/otlp` |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` | Spring Boot trace endpoint, `${GRAFANA_OTLP_ENDPOINT}/v1/traces` |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_HEADERS_AUTHORIZATION` | Spring Boot trace auth header |
| `OTEL_SERVICE_NAME` | Tempo service name, for example `tchalanet-api-staging` |
| `OTEL_RESOURCE_ATTRIBUTES` | Includes `service.name` and `deployment.environment` |

## Log Export Strategy

To stay within the 50 GB/month free tier, log export is filtered by level.

**Default**: only `WARN` and `ERROR` logs are exported to Grafana Cloud. Business-critical packages (`draw`, `sale`, `payout`) export at `INFO`.

### Per-package override via env vars

Override any package level without redeploying by setting env vars in Doppler:

| Env var | Package | Default |
|---------|---------|---------|
| `GRAFANA_LOG_ROOT_LEVEL` | everything | WARN |
| `GRAFANA_LOG_APP_LEVEL` | `com.tchalanet.server` | WARN |
| `GRAFANA_LOG_DRAW_LEVEL` | `com.tchalanet.server.core.draw` | INFO |
| `GRAFANA_LOG_SALE_LEVEL` | `com.tchalanet.server.core.sale` | INFO |
| `GRAFANA_LOG_PAYOUT_LEVEL` | `com.tchalanet.server.core.payout` | INFO |
| `GRAFANA_LOG_SECURITY_LEVEL` | `com.tchalanet.server.common.security` | WARN |
| `GRAFANA_LOG_SPRING_SECURITY_LEVEL` | `org.springframework.security` | WARN |
| `GRAFANA_LOG_SQL_LEVEL` | `org.hibernate.SQL` | WARN |

Example: to debug auth issues temporarily:
```
GRAFANA_LOG_SECURITY_LEVEL=DEBUG
GRAFANA_LOG_SPRING_SECURITY_LEVEL=DEBUG
```

### Trace sampling

`GRAFANA_TRACE_SAMPLING` controls the sampling probability (default `1.0` = 100%). Reduce to `0.1`–`0.5` if trace volume becomes too high.

### Tempo smoke check

After deployment, generate API traffic and query Tempo with:

```text
service.name = tchalanet-api-staging
```

Use a short time range first, for example **Last 15 minutes**.

## What happens at 50 GB?

Grafana Cloud stops ingesting new data. The API continues running normally — OTLP exports fail silently (HTTP 429). No crash, no blocking. Existing data remains queryable.

## CI Static Analysis

Runs on every PR to `main` (see `.github/workflows/server-pr.yml`):

| Tool | What it checks | Mode |
|------|---------------|------|
| Spotless | Code formatting | warn |
| Checkstyle | Structural rules (imports, naming) | warn |
| PMD | Bug patterns, complexity | warn |
| SpotBugs | Bytecode analysis (security, null) | warn |
| OWASP Dependency-Check | Known CVEs in dependencies | parallel job, PR comment |
| JaCoCo | Test coverage | PR comment |

All static analysis tools run in `continue-on-error` mode (warn, not block).

## Files

- `application-grafana-cloud.yaml` — Spring profile with OTLP endpoints + log level config
- `logback-spring.xml` — Console/file appenders (OTLP logs handled by Spring Boot native)
- `docker-compose-api.yml` — Passes Grafana Cloud/OTel variables to API container
- `docker-compose-otel.yml` — Local Jaeger for dev
