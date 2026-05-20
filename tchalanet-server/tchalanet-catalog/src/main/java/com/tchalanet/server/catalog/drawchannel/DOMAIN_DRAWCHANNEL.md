# Domaine Catalog DrawChannel

> Tenant-scoped catalog that exposes the sale channels a tenant can use for draws.

## Role

`draw_channel` links a tenant-facing channel such as `HT_NY_MID` to a global `result_slot`
such as `NY_MID`. It is a catalog module: it stores reference/configuration data and exposes
read APIs to domains and features, but it does not own draw lifecycle business rules.

## Responsibilities

- Provide active channels per tenant for draw generation and public display.
- Carry tenant-facing labels, local cutoff seconds, active status, sort order, and channel games.
- Point to exactly one global `result_slot` when the channel is backed by an external provider slot.
- Expose read contracts through `catalog.drawchannel.api`.

## Non Responsibilities

- No domain events.
- No draw state transitions.
- No provider HTTP calls.
- No Haiti projection.
- No ticket settlement.

## Pipeline Position

`catalog.drawchannel` is consumed by `core.draw` during `generate/open/close/apply/settle` flows and
by `features.publicdraw` for public read models. The external result is fetched globally from
`catalog.resultslot`; the tenant draw is then applied through channels that point to that slot.

## Boundaries

- Other modules must call `DrawChannelCatalog`; they must not depend on `internal.*`.
- Business invariants remain in `core.draw`.
- Provider mapping remains in `catalog.resultslot.source_cfg` and `application-uslottery.yaml`.
