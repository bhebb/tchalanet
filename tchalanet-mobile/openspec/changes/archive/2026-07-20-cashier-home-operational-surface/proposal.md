# Cashier Home Operational Surface

## Why

The current seller-terminal home is a demo-oriented grid. It repeats a tenant/location
label instead of draw identity, wastes most of the compact screen on uneven KPI cards,
and does not scale when a seller can sell dozens of draws. The header menu is inert and
the Android launch surface still exposes Flutter-default branding.

## What changes

- Present a dense, vertically scrollable draw list built from the existing available-draw
  contract: provider identity/time, tenant-local time, and a live cutoff countdown.
- Use the channel identity as primary content and never use the tenant/location label as
  a draw title.
- Recompose the three daily KPIs into one balanced, scannable band.
- Make the compact shell operational: a working drawer, an explicit active navigation
  accent, a branded header, and a Tchalanet loading/Android launch identity.
- Keep the existing sell bottom sheet and provider contracts; no business calculations
  move to Flutter.

## Non-goals

- Changing draw availability, cutoff, pricing, or seller permissions.
- Adding client-side pagination; the list remains one scroll surface for the available
  draw set. Search/filtering can be added once the operational catalog requires it.
- Replacing the shared navigation architecture across every routed screen.
