# Design — Bet Options, Selection Canonicalization, and Result Evaluation

## Context

The Haiti MVP sale line is identified commercially by:

```text
gameCode   = product/game sold by the cashier
betType    = technical rule family
betOption  = commercial formula inside that family
selection  = seller-entered number or combination
```

The current implementation is only partially option-aware. Loto 4/Loto 5 paths
already pass an option in places, but Loto 3 is exact-only, Maryaj ignores order,
and some selection canonicalization still accepts free-form wildcard masks that
are excluded from cashier V1.

## Goals / Non-Goals

**Goals:**

- Make `BetOption` the single source of truth for option codes, labels,
  descriptions, and option requirements.
- Canonicalize cashier selections with `betType + betOption`.
- Validate sale lines consistently before exposure and persistence.
- Settle option-aware tickets using explicit result facts.
- Expose POS-friendly labels and hints without leaking technical enum names.
- Keep the change inside backend catalog/core/features boundaries.

**Non-Goals:**

- No new promotion or free-line behavior.
- No generalized lottery rule engine.
- No new external dependency.
- No seller-entered wildcard masks in V1.
- No production data migration unless legacy production tickets are confirmed.

## Decisions

### Decision 1 — Model options as `catalog.game.api.model.BetOption`

`BetOption` is a public catalog API enum because it is stable reference data
consumed by core sales and cashier features. Each enum constant is scoped to one
`BetType`, with stable short code, label, and description.

Alternatives considered:

- Keep raw short switches in core sales. Rejected because option code `1` is not
  globally meaningful and would keep duplicating validation rules.
- Store options in database now. Rejected for MVP because the option set is
  small and stable, and no runtime administration is needed yet.

### Decision 2 — Let `BetType` delegate option knowledge to `BetOption`

`BetType.requiresOption()`, `allowedOptions()`, and `supportsOption(Short)` call
`BetOption` helpers. This preserves the existing `BetType` entry point while
centralizing option rules.

Alternatives considered:

- Put all option logic directly in `BetType`. Rejected because labels and
  descriptions belong to the option concept, not the bet type family.

### Decision 3 — Canonicalize with `betType + betOption + selection`

The sale path calls the option-aware canonicalizer. Maryaj preserves order,
Loto 4 front/back pair generates `NN**` or `**NN`, and cashier input containing
`*` is rejected in V1.

Alternatives considered:

- Keep the old two-argument canonicalizer as the primary path. Rejected because
  it cannot distinguish exact/box/front/back pair rules safely.

### Decision 4 — Include `betOption` in exposure identity

Exposure keys include `betOption` alongside draw, game, bet type, canonical
selection, scope, and scope reference. This prevents distinct formulas sharing
the same canonical digits from collapsing into one risk bucket.

Alternatives considered:

- Share exposure across options with the same selection. Rejected because odds,
  limits, and settlement rules can differ by option.

### Decision 5 — Build explicit result facts for settlement

The winning calculator resolves `BetOption.from(line.betType(), line.betOption())`
and evaluates against explicit ordered facts: ordered two-digit lots, lot1 3D,
lot2 2D, lot3 2D, pick3, and pick4. If the draw projection does not expose these
facts directly, core sales derives a local facts adapter before calculation.

Alternatives considered:

- Continue using only a set of two-digit values. Rejected because ordered Maryaj
  and Loto 5 option mapping require positional facts.

### Decision 6 — Cashier metadata comes from `/tenant/cashier/games/available`

The feature endpoint returns game labels, bet type labels, option labels,
descriptions, and selection hints. POS payloads remain stable and still submit
raw seller input; the backend canonicalizes it.

Alternatives considered:

- Let POS hard-code labels. Rejected because the backend owns the commercial
  option map for this MVP and labels must stay aligned with validation.

## Risks / Trade-offs

- Legacy tickets with old canonical selections may settle differently → confirm
  whether data is dev-only; otherwise add a legacy/versioned settlement path.
- Enum labels are not fully i18n-ready → acceptable for MVP; later catalog/i18n
  can externalize labels without changing option codes.
- Result projections may not expose all required facts → derive a focused
  `TicketResultFacts` adapter in core sales instead of widening unrelated APIs.
- The endpoint may duplicate existing game metadata formatting → keep it in the
  cashier feature slice and use catalog/core public APIs only.

## Migration Plan

1. Add `BetOption` and option helpers.
2. Migrate sale canonicalization and validation to the option-aware path.
3. Include `betOption` in exposure keys and receipt/shareable display.
4. Add option-aware result facts and settlement tests.
5. Add the cashier game-options endpoint metadata.
6. Validate with focused unit tests first, then relevant Maven module tests.

Rollback is a normal code rollback before production data exists. If legacy
production tickets are present, rollback must preserve the settlement logic that
matches the ticket version used at sale time.

## Open Questions

- Is there any production-like persisted ticket data for this MVP, or can dev DB
  be reset when canonicalization changes?
- Should duplicate-digit Loto 3 box plays such as `111` be invalid or equivalent
  to straight? The implementation should follow existing product rules if they
  are already encoded; otherwise reject ambiguous cases only if tests/specs are
  updated.
