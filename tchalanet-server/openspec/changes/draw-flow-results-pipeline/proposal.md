# Change: draw-flow-results-pipeline

## Summary

Stabiliser le flow complet tirages/résultats Tchalanet en séparant strictement les concepts `game_code`, `draw_channel`, `result_slot`, `draw_result` et codes provider US Lottery.

## Motivation

Le code actuel contient encore des usages ambigus de `gameCode`, `channelCode`, `drawChannelCode`, `slotKey`, `externalGameKey` et codes provider. Cette ambiguïté crée des risques de mauvais fetch provider, mauvais apply de résultat, duplication de schedulers, difficultés de debug local, et couplage entre `core.draw`, `catalog.drawchannel`, `core.drawresult`, `core.haiti` et `core.uslottery`.

## Core decisions

- `game_code` désigne uniquement le produit vendu (`HT_BOLET`, `HT_MARYAJ`, `HT_LOTO3/4/5`, etc.).
- `draw_channel` est un calendrier tenant-scoped de vente/tirage.
- `result_slot` est le slot externe global attendu, source de vérité pour fetch provider.
- `draw_result` est global, attaché à un `result_slot`, puis appliqué aux `draw` tenant-scoped.
- Les codes provider (`US_FL_PICK3_MID`, `US_NY_NUM3_EVE`, etc.) sont des `external_game_code`, jamais des `draw_channel.code` ni des `game_code` vendus.

## Affected areas

- `catalog.game`
- `catalog.drawchannel`
- `catalog.resultslot`
- `core.draw`
- `core.drawresult`
- `core.haiti`
- `core.uslottery`
- `features.ops`

## MVP scheduler model

Only three schedulers are required:

1. `draw_generate_daily` — generate draws D..D+N per tenant once per day.
2. `draw_lifecycle_tick` — open and close due draws in one tick.
3. `draw_results_tick` — refresh results by fetching due result slots and applying available results.

Schedulers must be disableable locally. Ops endpoints must support manual `dryRun`, `force`, `reason`, and `maxSlots/maxItems` debugging.

## Ops model

Keep Ops endpoints as the primary debug/replay/backfill surface.

Target routes:

- `POST /platform/ops/draws/generate`
- `POST /platform/ops/draws/open-due`
- `POST /platform/ops/draws/close-due`
- `POST /platform/ops/draws/lifecycle-refresh`
- `POST /platform/ops/draw-results/fetch` — global, `tenantId = null`
- `POST /platform/ops/draw-results/apply` — tenant-scoped, `tenantId` required
- `POST /platform/ops/draw-results/refresh` — tenant-scoped orchestrator, fetch global then apply tenant
- `POST /platform/ops/draw-results/manual` — tenant-scoped, audited
- `POST /platform/ops/draw-results/override` — tenant-scoped, audited

## Compatibility

This change preserves the product-facing behavior while refactoring internals. Existing legacy repositories/fields should be migrated or kept only behind compatibility adapters until removed.
