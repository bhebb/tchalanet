# Spec delta: core.uslottery

## ADDED Requirements

### Requirement: Dedicated provider clients

Each US Lottery provider SHALL have a dedicated client because provider response shapes differ.

### Requirement: Provider clients normalize to LatestDraw

All provider clients SHALL return `List<LatestDraw>`.

### Requirement: external game code naming

`ProviderDrawQuery.channelCodes` SHALL be renamed to `externalGameCodes`.

### Requirement: LatestDraw external game code naming

`LatestDraw.channelCode` SHALL be renamed to `externalGameCode` or `providerGameCode`.

### Requirement: Provider clients are isolated from Tchalanet draw model

Provider clients SHALL NOT know tenant, draw, draw_channel, draw_result, or Haitian sold game codes.

### Requirement: Fetch port adapter maps result_slot to provider query

`UsLotteryExternalResultsFetchPortAdapter` SHALL read `result_slot.source_cfg`, call the provider client once per slot/date, and recompose pick3/pick4 into an ExternalBundle.

### Requirement: result_slot time is authoritative fallback

Provider clients SHOULD NOT approximate draw times when result_slot provides draw time and timezone. The adapter SHALL resolve `occurredAt` using result_slot fallback.

### Requirement: Provider cache name

Raw provider cache SHALL use functional cache name `infra.uslottery.provider_raw`.

### Requirement: Clock usage

Provider clients and flags SHALL use injected `Clock` and avoid direct `Instant.now()`.

### Requirement: Registry purpose

`UsLotteryGameRegistry` SHALL only resolve provider configuration. It SHALL NOT resolve sold `game_code` values.
