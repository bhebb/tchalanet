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
