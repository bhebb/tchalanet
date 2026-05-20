# Claude — core.uslottery

Scope:

- Provider HTTP clients: NY (via Socrata API), FL (Azure API), GA, TX
- External result fetching and parsing
- No persistence; query ports only
- Result normalization (returns structured result, not persisted)

Out of scope:

- Result persistence (drawresult responsibility)
- Tenant-scoping (global provider data)
- Haiti projection (haiti.HaitiProjectionService responsibility)
- Ticket settlement (sales responsibility)

Rules:

- Configuration via `application-uslottery.yaml`

  - Enable/disable per provider (tch.us-lottery.providers.<code>.enabled)
  - Base URLs, headers, credentials all configurable via env vars

- HTTP clients implemented as adapters (outbound ports)

  - Use circuit-breaker / retry patterns if added
  - Handle 3rd-party API schema changes gracefully
  - Parse JSON/XML → normalized result struct

- UsLotteryProviderQuery port (interface)

  - Query: `(providerCode, resultSlotKey, date?)`
  - Result: structured with numbers, metadata, quality flags
  - Never called directly by domain; used by drawresult.ExternalResultFetcher

- Pure parsing, no Spring beans in domain
- Log all external API failures clearly
- Never block draw result fetch on provider timeout (graceful degradation)

Before editing:

- Load drawresult.ExternalResultFetcher for integration contract
- Load haiti.HaitiProjectionService to understand downstream normalization
- Load application-uslottery.yaml for config scheme
- Load docs/conventions/cache.md if caching provider responses

Output:

1. Files inspected
2. Files changed
3. Tests or manual verification
4. Risks (API contract changes, timeout handling)
5. Compact handoff
