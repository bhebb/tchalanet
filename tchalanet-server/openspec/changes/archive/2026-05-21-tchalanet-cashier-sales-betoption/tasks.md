# Tasks — BetOption / Selection Regeneration

## PR A — Catalog option model

- [x] Add `catalog.game.api.model.BetOption` enum.
- [x] Add helpers: `allowedFor`, `requiresOption`, `from`.
- [x] Update `BetType` to expose option support by delegating to `BetOption`.
- [x] Add unit tests for every BetType option requirement.

## PR B — Selection canonicalization

- [x] Change `SelectionKeyCanonicalizer` signature to include `Short betOption`.
- [x] Update Maryaj canonicalization to preserve order.
- [x] Add Loto 3 option-aware validation while preserving 3-digit canonical key.
- [x] Add Loto 4 option-aware canonicalization:
  - [x] exact -> 4 digits
  - [x] box -> 4 digits
  - [x] front pair -> `NN**`
  - [x] back pair -> `**NN`
- [x] Add Loto 5 option-aware validation requiring 5 digits.
- [x] Stop using free-form `*` input in cashier sale path.
- [x] Add tests for valid/invalid selection examples.

## PR C — Sale evaluation/exposure

- [x] Validate `GameCode.supports(betType)`.
- [x] Validate `BetOption.from(betType, betOption)`.
- [x] Use canonical selection from option-aware canonicalizer.
- [x] Include `betOption` in `ExposureKey`.
- [x] Update issue details for invalid option/selection.

## PR D — Result calculator

- [x] Replace raw `Short option` switch with `BetOption` enum.
- [x] Implement Lotto 3 exact/box.
- [x] Implement Maryaj exact/reverse using ordered result facts.
- [x] Implement Lotto 4 exact/box/front/back using Pick 4.
- [x] Implement Lotto 5 option 1/2/3 using explicit lot1/lot2/lot3.
- [x] Update `DrawResultProjection` or add a result facts adapter if needed.
- [ ] Add tests for every option.

## PR E — Cashier game-options endpoint

- [x] Add `GET /tenant/cashier/games/available` or extend current available draws response.
- [x] Return game labels, bet type labels, option labels, descriptions, selection hints.
- [x] Ensure POS does not display enum names or raw option codes.

## PR F — Receipt/backup display

- [x] Update TicketReceiptLineView to include option label.
- [x] Use option labels in print receipt and shareableText.
- [ ] Add test: backup shareableText displays human option labels.

## E2E additions

- [ ] Preview Loto 3 exact accepted.
- [ ] Preview Loto 3 box accepted.
- [ ] Preview Maryaj exact accepted.
- [ ] Preview Maryaj revers accepted.
- [ ] Preview Loto 4 front pair accepts 2-digit input.
- [ ] Preview Loto 4 back pair accepts 2-digit input.
- [ ] Preview Loto 4 exact rejects 2-digit input.
- [ ] Preview Loto 5 option 3 accepts 5-digit input.
- [ ] Sell accepted includes option label in backup/shareableText.
