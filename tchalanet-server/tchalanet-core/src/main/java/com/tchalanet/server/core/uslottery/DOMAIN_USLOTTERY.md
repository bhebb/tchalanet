# DOMAIN — US LOTTERY

## 1. Purpose

`core.uslottery` is an external integration domain.

It is responsible for:

- calling US lottery providers (NY, FL, GA, TX, TN)
- fetching raw payloads (HTTP / JSON / RSS)
- parsing provider responses
- normalizing results into a simple provider format
- returning results keyed by `gameCode`

It does NOT know:

- result_slot (business)
- draw_result (DB)
- Haiti projection
- tenant
- draw / channel / sales

---

## 2. Boundary with drawresult

drawresult → ExternalResultsFetchPort → uslottery → providers

- drawresult owns the port
- uslottery implements the adapter

---

## 3. Core rule

drawresult decides WHAT to fetch  
uslottery fetches HOW

---

## 4. Contracts separation

### drawresult (outgoing port)

ExternalResultFetchQuery  
ExternalResultFetchBundle  
ExternalResultItem

→ business-facing contract

### uslottery (internal provider)

UsLotteryProviderQuery  
UsLotteryProviderResponse  
UsLotteryProviderResult

→ provider-facing contract

### adapter

External → Provider → External

---

## 5. Providers

Enum:
UsLotteryProvider { NY, FL, GA, TN, TX }

Clients:

- NewYorkDrawResultsClient
- FloridaDrawResultsClient
- GeorgiaDrawResultsClient
- TexasDrawResultsClient
- TennesseeDrawResultsClient

Mappers:

- NewYorkDrawResultsMapper
- FloridaDrawResultsMapper
- GeorgiaDrawResultsMapper
- TexasDrawResultsMapper
- TennesseeDrawResultsMapper

---

## 6. Game codes

NY: NUMBERS, WIN4  
FL: PICK3, PICK4  
GA: PICK3, PICK4  
TX: PICK3, DAILY4

→ gameCode is the ONLY key

---

## 7. Removed concepts

NOT allowed:

- US\_\* codes
- external_draw_type
- external_result_key
- provider channelCode
- composite mappings
- maxDraws

---

## 8. Allowed data

gameCode  
main digits  
extras  
quality  
metadata (optional)

---

## 9. Business mapping

Defined in result_slot.source_cfg

Example:
{
"pick3": { "game_code": "NUMBERS" },
"pick4": { "game_code": "WIN4" }
}

---

## 10. Configuration

Technical only (application-uslottery.yml)

---

## 11. Rest clients

nyLotteryRestClient  
floridaLotteryRestClient  
gaLotteryRestClient  
txLotteryRestClient  
tnLotteryRestClient

---

## 12. Cache

infra.uslottery.provider_raw

Rules:

- best-effort
- never source of truth
- no tenant
- no slot

---

## 13. Query hash

Includes:
provider, date, time, gameCodes, shape

Excludes:
tenant, slotKey, business mapping

---

## 14. Flow

Scheduler / Ops
→ drawresult
→ uslottery adapter
→ provider client
→ mapper
→ drawresult
→ persist

---

## 15. Non-goals

uslottery must never:

- write DB
- read result_slot
- know tenant
- apply results
- compute payouts

---

## 16. Guiding principle

You do not map codes.  
You resolve: which result for this slot.

---

## 17. Analysis V1 (2026-05-05) — Flow Validation

### Provider Integration Validated

✅ **Provider HTTP Clients**:

- NY (Socrata API): via `NewYorkDrawResultsClient`
- FL (Azure API): via `FloridaDrawResultsClient`
- GA: via `GeorgiaDrawResultsClient`
- TX: via `TexasDrawResultsClient`
- All configured via application-uslottery.yaml (base-url, headers, credentials)

✅ **Fetch & Parse flow**:

- ExternalResultFetcher calls provider clients
- Provider-specific mappers normalize response (JSON/XML)
- Return structured result: { gameCode, numbers, extras, quality, metadata }
- No persistence locally (query-only)

✅ **Provider Result Types**:

- NY: NUMBERS, WIN4
- FL: PICK3, PICK4
- GA: PICK3, PICK4
- TX: PICK3, DAILY4
- Game code is unique key for mapping

✅ **Integration with drawresult**:

- Called by FetchExternalResultsWindowCommandHandler
- Input: FetchExternalResultsWindowCommand (baseDate, slotKeys, daysBack, maxSlots)
- Output: structured result keyed by gameCode
- Never blocks draw result fetch (graceful degradation on timeout)

### Architecture Compliance

- ✅ No persistence: Query ports only
- ✅ Global scope: No tenant_id filtering
- ✅ Pure parsing: Adapt JSON/XML → struct
- ✅ Config-driven: application-uslottery.yaml for all settings
- ✅ Circuit-breaker ready: Candidate for retry/timeout patterns
- ✅ No domain logic: External integration layer only

### Configuration (application-uslottery.yaml)

```yaml
tch.us-lottery:
  providers:
    ny:
      enabled: true
      base-url: https://data.ny.gov/api/v3/views/...
      app-token: <token>
    fl:
      enabled: true
      base-url: https://apim-website-prod-eastus.azure-api.net
      headers: { Accept, x-partner, Origin, Referer, User-Agent }
    ga:
      enabled: true
      base-url: https://www.galottery.com
    tx:
      enabled: true
      base-url: https://www.texaslottery.com
```

### Notes

- Cache: Best-effort, never source of truth
- Query hash: Includes provider, date, time, gameCodes, shape (excludes tenant, slotKey, business mapping)
- Idempotency: Via drawresult sourceHash mechanism (not in uslottery)
