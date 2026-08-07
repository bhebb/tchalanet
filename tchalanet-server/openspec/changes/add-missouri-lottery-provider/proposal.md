# add-missouri-lottery-provider

## Why

Missouri (`MO_MID`, `MO_EVE`) is currently configured as a manual results provider even though the official Missouri Lottery pages are accessible from staging and expose Pick 3 / Pick 4 winning numbers in parsable HTML.

## What

- Add a Missouri provider HTTP client and mapper for official `molottery.com` Pick 3 / Pick 4 winning-number pages.
- Configure Missouri result URLs and browser-compatible request headers.
- Update Missouri result slot seed configuration from manual-only to automatic Pick 3 / Pick 4 fetch.
- Add focused mapper coverage for midday/evening, Pick 3/Pick 4, Wild Ball extras, date filtering, and malformed HTML tolerance.
- Update provider documentation to mark Missouri as automatic-capable after staging validation.

## Impact

- `MO_MID` and `MO_EVE` can be fetched by the existing draw-result external fetch job.
- No tenant-specific logic is added to `core.uslottery`.
- The provider remains opt-in through existing draw channel activation.

## Non-goals

- Do not implement Tennessee or Illinois automation; both currently return Cloudflare challenges from staging with simple HTTP clients.
- Do not add a browser automation/proxy layer.
- Do not change payout, projection, or settlement behavior.
