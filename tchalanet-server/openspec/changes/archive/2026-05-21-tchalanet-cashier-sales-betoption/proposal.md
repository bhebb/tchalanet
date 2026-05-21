# Proposal — Cashier/Sales Bet Options & Selection V1

## Why

The cashier sale flow already carries `gameCode`, `betType`, `betOption`, and
`selection`, but the rule ownership is still ambiguous. Some code paths need
`betOption` for Loto 4 and Loto 5, while others treat selections as if
`betType` alone were enough. This creates inconsistent validation,
canonicalization, exposure, receipt display, and settlement behavior.

The POS must also avoid exposing technical enum names such as
`LOTTO4_PATTERN` or raw option codes without seller-facing labels.

## What Changes

- Add a public `BetOption` model scoped by `BetType`.
- Make option requirements explicit:
  - no option for Bolet/Numero 2D match bet types;
  - required option for Maryaj, Loto 3, Loto 4, and Loto 5 pattern bet types.
- Canonicalize selections using `betType + betOption + rawSelection`.
- Preserve Maryaj order for exact-order play.
- Disable seller-entered wildcard masks in cashier V1; generated canonical
  Loto 4 pair keys may still contain `*`.
- Validate game support, bet option support, and canonical selection during sale
  preview/sell evaluation.
- Include `betOption` in exposure keys.
- Make result evaluation option-aware for Maryaj, Loto 3, Loto 4, and Loto 5.
- Add/update the cashier available-games endpoint so the POS receives game
  labels, bet type labels, option labels, descriptions, and selection hints.
- Use human option labels in receipt and backup/shareable text output.

## Capabilities

- `catalog-game`
- `core-sales`
- `features-cashier`

## Impact

- Backend catalog API gains `BetOption`.
- Core sales validation, canonicalization, exposure, receipt formatting, and
  result calculation become option-aware.
- Cashier feature endpoints expose seller-facing game and option metadata.
- Existing dev tickets with old canonical values may need a reset. If production
  data exists before this change lands, a legacy settlement strategy or version
  flag is required before rollout.

## Non-Goals

- No promotions.
- No automatic free Maryaj.
- No generated free lines.
- No free-form wildcard seller input for Loto 4 or Loto 5.
- No PDF attachments for send/share.
