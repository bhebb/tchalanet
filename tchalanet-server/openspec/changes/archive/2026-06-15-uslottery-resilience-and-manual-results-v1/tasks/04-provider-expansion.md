# Task 04 — Provider Expansion

## Scope

`core.uslottery` — enum, config, clients, mappers

## Minimum viable for each provider

1. Add to `UsLotteryProvider` enum.
2. Add YAML block in `application-uslottery.yml` with `enabled`, `timezone`, `base-url`, `transport`.

If no client is registered, the adapter returns an empty bundle → results handled manually via Slice 5.

## Full implementation (client + mapper) — first candidates

### PA — RSS (first priority)

- Transport: `OFFICIAL_RSS`
- Add `PennsylvaniaLotteryRestClient`.
- Add `PennsylvaniaDrawResultsMapper` with per-entry resilience (`mapEntriesSafely`).
- Game codes: PICK3, PICK4 (verify against actual RSS feed).
- YAML: `trust-policy: AUTO_CONFIRM_HIGH_CONFIDENCE` if feed is reliable.

### MO — HTML / internal JSON

- Transport: `OFFICIAL_INTERNAL_JSON` preferred, fallback `OFFICIAL_HTML`.
- Add client + mapper if internal JSON endpoint is stable and documented.
- If HTML only: enum + YAML only, manual results.

### CA — internal JSON / HTML

- Transport: `OFFICIAL_INTERNAL_JSON` preferred.
- Add client + mapper if API is accessible without scraping.
- YAML: `trust-policy: REQUIRE_PLATFORM_REVIEW` for HTML sources.

### NJ — internal JSON / HTML

- Same approach as CA.

## Enum-only providers (no client this V1)

IL, MI, OH: add to enum + YAML (`enabled: true`, transport noted). No client registered.

## Mapper resilience (required for every new mapper)

```java
private List<UsLotteryProviderResult> mapEntriesSafely(...) {
    var results = new ArrayList<UsLotteryProviderResult>();
    for (var entry : entries) {
        try {
            var mapped = mapEntry(entry, ...);
            if (mapped != null) results.add(mapped);
        } catch (Exception ex) {
            log.warn("provider-client skipped invalid entry provider={} ...", ex.getMessage(), ex);
        }
    }
    return List.copyOf(results);
}
```

## TN reconciliation

`TennesseeDrawResultsClient` already exists in the enum but game codes are not documented in `DOMAIN_USLOTTERY §6`. Verify and add TN game codes to the domain doc before or during this slice.

## Tests

- Each new full client: happy path + malformed response (mapper skips bad entry, continues).
- Enum-only providers: adapter returns empty bundle, no throw.

## YAML config pattern

```yaml
tch:
  us-lottery:
    providers:
      pa:
        enabled: true
        timezone: America/New_York
        base-url: https://...
        transport: OFFICIAL_RSS
        trust-policy: AUTO_CONFIRM_HIGH_CONFIDENCE
      mo:
        enabled: true
        timezone: America/Chicago
        base-url: https://...
        transport: OFFICIAL_INTERNAL_JSON
        trust-policy: REQUIRE_PLATFORM_REVIEW
```

`fallback-path` and `fallback-transport` are optional; add only if a real fallback is implemented.
