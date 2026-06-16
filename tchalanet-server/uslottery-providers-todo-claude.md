# TODO — US Lottery Providers V0

## Objectif

Ajouter les providers US Lottery manquants dans `core.uslottery` sans complexifier le domaine.

Le pipeline reste inchangé :

```text
uslottery provider client
  -> UsLotteryProviderResponse
  -> drawresult external fetch adapter
  -> draw_result upsert
  -> apply tenant draws
  -> settle tickets
```

Règle importante : `core.uslottery` ne fait jamais de settlement et n'écrit pas dans `sales` / `payout`.

---

## État cible V0

### Providers automatiques

```text
NY -> existing JSON/API
FL -> existing JSON/API
GA -> existing JSON/API
TX -> existing RSS
NJ -> internal JSON
CA -> internal JSON
PA -> internal feed/data endpoint
IL -> HTML partial ciblé
OH -> internal JSON with bearer token
MI -> internal GraphQL
```

### Providers manuels V0

```text
TN -> manual proposal only
MO -> manual proposal only
```

Les tenants admins peuvent proposer un résultat manuel pour TN/MO. Le super admin valide avant que le résultat devienne officiel.

---

## Décisions de simplicité

Ne pas créer une grosse mécanique de provider registry en base pour V0.

On garde :

```text
YAML provider.enabled      = kill switch technique par env
result_slot.active         = kill switch global slot/provider
draw_channel.active        = kill switch tenant/channel
draw_channel_game.enabled  = kill switch tenant/game
source_cfg.pickX.active    = kill switch game externe dans un slot
```

Le fetch doit être fail-soft : un provider cassé ne bloque pas les autres.

---

## TODO 1 — Resilience / fail-soft

### Adapter

Modifier `UsLotteryExternalResultsFetchAdapter` :

- [ ] Ne pas throw si `query.provider()` est inconnu.
- [ ] Ne pas throw si `registry.get(provider)` ne trouve pas de client.
- [ ] Ne pas throw si `client.fetch(...)` échoue.
- [ ] Retourner `ExternalResultFetchBundle.empty(...)` en cas d'erreur.
- [ ] Logger `warn` avec provider, drawDate, drawTime, providerSlotCode, gameCodes.

### Registry

- [ ] Ajouter `ProviderClientRegistry.find(provider): Optional<UsLotteryProviderClient>`.
- [ ] Garder `get(provider)` seulement si nécessaire pour les usages fail-fast internes.

### Bundle

- [ ] Ajouter une factory :

```java
ExternalResultFetchBundle.empty(provider, drawDate, drawTime, timezone)
```

### Mappers

- [ ] Dans chaque mapper, une entrée invalide doit être skipped, pas faire échouer tout le provider.
- [ ] Parser en boucle `for` avec `try/catch` par entry/block.

---

## TODO 2 — Config YAML providers

Ajouter ces providers dans `tch.us-lottery.providers` :

```yaml
nj:
  enabled: ${TCH_US_NJ_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_NJ_TIMEZONE:America/New_York}
  base-url: ${TCH_US_NJ_LOTTERY_BASEURL:https://www.njlottery.com}
  latest-path: ${TCH_US_NJ_LATEST_PATH:/api/v1/draw-games/draws/page}

ca:
  enabled: ${TCH_US_CA_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_CA_TIMEZONE:America/Los_Angeles}
  base-url: ${TCH_US_CA_LOTTERY_BASEURL:https://www.calottery.com}
  latest-path: ${TCH_US_CA_LATEST_PATH:/api/DrawGameApi/DrawGamePastDrawResults}

pa:
  enabled: ${TCH_US_PA_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_PA_TIMEZONE:America/New_York}
  base-url: ${TCH_US_PA_LOTTERY_BASEURL:https://www.palottery.pa.gov}
  latest-path: ${TCH_US_PA_LATEST_PATH:/Custom/feeds/DrawingsData.aspx}

il:
  enabled: ${TCH_US_IL_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_IL_TIMEZONE:America/Chicago}
  base-url: ${TCH_US_IL_LOTTERY_BASEURL:https://www.illinoislottery.com}
  latest-path: ${TCH_US_IL_LATEST_PATH:/dbg/results}

oh:
  enabled: ${TCH_US_OH_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_OH_TIMEZONE:America/New_York}
  base-url: ${TCH_US_OH_LOTTERY_BASEURL:https://api-solutions.ohiolottery.com}
  latest-path: ${TCH_US_OH_LATEST_PATH:/1.0/Games/DrawGames}
  bearer-token: ${TCH_US_OH_BEARER_TOKEN:}

mi:
  enabled: ${TCH_US_MI_LOTTERY_ENABLED:true}
  timezone: ${TCH_US_MI_TIMEZONE:America/New_York}
  base-url: ${TCH_US_MI_LOTTERY_BASEURL:https://www.michiganlottery.com}
  latest-path: ${TCH_US_MI_LATEST_PATH:/api}
```

Do not commit browser cookies, Cloudflare cookies, or copied `sec-ch-*` headers.

---

## TODO 3 — Enum provider

Extend `UsLotteryProvider` :

```java
NY, FL, GA, TX, NJ, CA, PA, IL, OH, MI, TN, MO
```

TN and MO can exist in the enum even if V0 uses manual result proposals only.

---

## TODO 4 — Result slots / draw channels seed

Add `result_slot` rows for:

```text
NJ_MID, NJ_EVE
CA_MID, CA_EVE
PA_DAY, PA_EVE
IL_MID, IL_EVE
OH_MID, OH_EVE, OH_SAT_EVE
MI_MID, MI_EVE
TN_MOR, TN_MID, TN_EVE   # active false or manual only if desired
MO_MID, MO_EVE           # active false or manual only if desired
```

Add tenant `draw_channel` rows for the same slots.

Keep `draw_channel_game` unchanged if it already uses `channels CROSS JOIN games`.

---

## TODO 5 — Provider clients and mappers

### NJ — internal JSON

Endpoint pattern:

```text
/api/v1/draw-games/draws/page?date-to=...&date-from=...&game-names=Pick+3&status=CLOSED&size=1000&page=0
```

Tasks:

- [ ] Create `NewJerseyDrawResultsClient`.
- [ ] Create `NewJerseyDrawResultsMapper`.
- [ ] Support `Pick 3` and `Pick 4`.
- [ ] Filter by `query.drawDate()` and `query.providerSlotCode()`.

### CA — internal JSON

Endpoint pattern:

```text
/api/DrawGameApi/DrawGamePastDrawResults/{gameId}/1/20
```

Known:

```text
DAILY3 -> gameId 9
DAILY4 -> confirm gameId before enabling pick4
```

Tasks:

- [ ] Create `CaliforniaDrawResultsClient`.
- [ ] Create `CaliforniaDrawResultsMapper`.
- [ ] Enable DAILY3 first.
- [ ] Keep DAILY4 disabled or `VERIFY_DAILY4_GAME_ID` until confirmed.

### PA — internal feed/data

Endpoint pattern:

```text
/Custom/feeds/DrawingsData.aspx?game=pick%203
/Custom/feeds/DrawingsData.aspx?game=pick%204
```

Tasks:

- [ ] Create `PennsylvaniaDrawResultsClient`.
- [ ] Create `PennsylvaniaDrawResultsMapper`.
- [ ] Mapper should tolerate JSON or XML/HTML-like feed until response shape is fixed with fixture.

### IL — HTML partial

Endpoint pattern:

```text
/dbg/results/pick3?dateFrom=YYYY-MM-DD&dateTo=YYYY-MM-DD&page=1
/dbg/results/pick4?dateFrom=YYYY-MM-DD&dateTo=YYYY-MM-DD&page=1
```

Tasks:

- [ ] Create `IllinoisDrawResultsClient`.
- [ ] Create `IllinoisDrawResultsMapper` using Jsoup.
- [ ] Mark source as `OFFICIAL_HTML_PARTIAL`.
- [ ] If parser returns `SUSPECT`, do not auto-confirm.

### OH — internal JSON with bearer token

Endpoint pattern:

```text
/1.0/Games/DrawGames/{pick3|pick4|pick5}/SearchDrawDateRange?sinceDate=MM-dd-yyyy&toDate=MM-dd-yyyy
```

Tasks:

- [ ] Add `bearerToken` support in `UsLotteryProperties.ProviderConfig`.
- [ ] Create `OhioDrawResultsClient`.
- [ ] Create `OhioDrawResultsMapper`.
- [ ] If bearer token is blank/expired, return empty and warn. Do not fail whole fetch.

### MI — internal GraphQL

Endpoint:

```text
POST https://www.michiganlottery.com/api
```

Operations:

```text
daily3Data / daily4Data
logicalGameIdentifier = DAILY_3 / DAILY_4
drawDate = yyyy-MM-ddT04:00:00.000Z for local date
```

Mapping:

```text
MIDDAY  -> winningNumbers.drawNumbersMid
EVENING -> winningNumbers.drawNumbersEve
```

Tasks:

- [ ] Create `MichiganDrawResultsClient`.
- [ ] Create `MichiganDrawResultsMapper`.
- [ ] Post GraphQL JSON body.
- [ ] Support `DAILY_3` and `DAILY_4`.
- [ ] Ignore payout data for V0.

---

## TODO 6 — Manual-only TN/MO V0

For V0, do not block on TN/MO scraping.

Tasks:

- [ ] Keep TN/MO result slots optional / inactive if automatic fetch is not ready.
- [ ] Allow tenant admin result proposal for TN/MO.
- [ ] Super admin approves/rejects/corrects.
- [ ] Only approved result becomes `draw_result CONFIRMED`.
- [ ] Only `CONFIRMED` result triggers apply/settle.

---

## TODO 7 — Tests

For each provider:

- [ ] Add fixture raw payload under test resources.
- [ ] Mapper test: extracts correct game code, date, slot, numbers.
- [ ] Mapper test: wrong slot returns no result.
- [ ] Mapper test: wrong date returns no result.
- [ ] Mapper test: malformed entry does not fail whole mapping.
- [ ] Client test: disabled config returns empty.
- [ ] Client test: fetch exception returns empty.

---

## Acceptance criteria

- [ ] A broken provider does not stop other providers.
- [ ] A broken game inside a provider does not stop other games.
- [ ] Provider can be disabled by YAML `enabled=false`.
- [ ] A result slot can be disabled by `result_slot.active=false`.
- [ ] A tenant channel can be disabled by `draw_channel.active=false`.
- [ ] A single provider game can be disabled using `source_cfg.pickX.active=false`.
- [ ] `uslottery` returns normalized `UsLotteryProviderResult` only.
- [ ] `uslottery` never settles tickets.
- [ ] All source results include origin, sourceHash, url, metadata.
- [ ] TN/MO can work manually through proposal + super admin validation.

