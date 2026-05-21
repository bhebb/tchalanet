# Proposal — Cashier/Sales Bet Options & Selection V1

## Status
Approved for implementation as an amendment to the Cashier Features & Sales Ticket Receipt V1 proposal.

## Goal
Stabilize how `gameCode`, `betType`, `betOption`, and `selection` work for the Haiti MVP without over-generalizing the engine.

This amendment keeps the existing `GameCode` / `BetType` model, adds an explicit `BetOption` enum, aligns selection canonicalization with `betType + betOption`, and prevents the POS from exposing technical enum names directly to sellers.

## Non-negotiable decisions

```text
gameCode   = product/game sold by the cashier
betType    = family of bet / technical rule family
betOption  = exact commercial formula inside that family
selection  = seller-entered number or combination
```

`betOption` is required only for bet types whose commercial rules have multiple formulas.

## Existing model retained

The existing MVP `GameCode` mapping is retained:

```text
HT_BOLET  -> MATCH_1_2D, MATCH_2_2D, MATCH_3_2D
HT_MARYAJ -> MARRIAGE_2D2D
HT_LOTO3  -> LOTTO3_3D
HT_LOTO4  -> LOTTO4_PATTERN
HT_LOTO5  -> LOTTO5_PATTERN
```

The POS must not display enum names such as `MATCH_1_2D`, `LOTTO4_PATTERN`, or `MARRIAGE_2D2D` directly. A cashier game-options endpoint exposes human labels, hints, and allowed options.

## V1 exclusions

```text
No promotions.
No automatic free Maryaj.
No generated free lines.
No wildcard free-form input for Loto 4 / Loto 5.
No PDF attachments for send.
```

Wildcard `*` may appear in the backend canonical key only for canonical Loto 4 front/back pair (`12**`, `**45`). The seller does not type `*` in V1.

## Why this matters

The current selection canonicalizer already needs `betOption` for Loto 4 canonicalization, while the winning calculator currently passes `line.betOption()` to Loto 4 and Loto 5 result logic. The code path therefore already expects option-aware behavior; this proposal formalizes it and fixes ambiguity.

