# Design

## Compact cashier home

The home has three independently useful regions:

1. App bar: brand, seller terminal, connectivity, notifications and an operational drawer.
2. Daily summary: sales, tickets and seller commission use equal visual weight in a
   single responsive band.
3. Available draws: one compact card per draw in a vertical list. Each card has an
   optional provider logo, channel label, provider and local schedules, an accessible
   live cutoff countdown, and a direct sell affordance.

The list is intentionally vertical. Forty cards remain usable with one scroll gesture,
whereas a two-column grid makes the card identity, provider time and cutoff unreadable.

## Draw identity

`channelLabel` is the commercial draw-channel identity. `providerDate`,
`providerTime`, and `providerTimezone` are provider facts. `localDate`, `localTime`,
and `localTimezone` are tenant-local facts. The UI must not infer a draw title from a
tenant or country label.

## Countdown

A one-minute ticker refreshes the cutoff label while the home is visible. The server
remains the source of truth: refresh/invalidation fetches the current open draw set.

## Branding

The existing web app shortcut uses the navy rounded square with gold `T`. Mobile reuses
that visual identity for the Flutter loading state and Android launch screen. No new
brand mark is introduced.
