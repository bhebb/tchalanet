# Tasks — BetOption / Selection Regeneration

## PR A — Catalog option model

- [ ] Add `catalog.game.api.model.BetOption` enum.
- [ ] Add helpers: `allowedFor`, `requiresOption`, `from`.
- [ ] Update `BetType` to expose option support by delegating to `BetOption`.
- [ ] Add unit tests for every BetType option requirement.

## PR B — Selection canonicalization

- [ ] Change `SelectionKeyCanonicalizer` signature to include `Short betOption`.
- [ ] Update Maryaj canonicalization to preserve order.
- [ ] Add Loto 3 option-aware validation while preserving 3-digit canonical key.
- [ ] Add Loto 4 option-aware canonicalization:
  - [ ] exact -> 4 digits
  - [ ] box -> 4 digits
  - [ ] front pair -> `NN**`
  - [ ] back pair -> `**NN`
- [ ] Add Loto 5 option-aware validation requiring 5 digits.
- [ ] Stop using free-form `*` input in cashier sale path.
- [ ] Add tests for valid/invalid selection examples.

## PR C — Sale evaluation/exposure

- [ ] Validate `GameCode.supports(betType)`.
- [ ] Validate `BetOption.from(betType, betOption)`.
- [ ] Use canonical selection from option-aware canonicalizer.
- [ ] Include `betOption` in `ExposureKey`.
- [ ] Update issue details for invalid option/selection.

## PR D — Result calculator

- [ ] Replace raw `Short option` switch with `BetOption` enum.
- [ ] Implement Lotto 3 exact/box.
- [ ] Implement Maryaj exact/reverse using ordered result facts.
- [ ] Implement Lotto 4 exact/box/front/back using Pick 4.
- [ ] Implement Lotto 5 option 1/2/3 using explicit lot1/lot2/lot3.
- [ ] Update `DrawResultProjection` or add a result facts adapter if needed.
- [ ] Add tests for every option.

## PR E — Cashier game-options endpoint

- [ ] Add `GET /tenant/cashier/games/available` or extend current available draws response.
- [ ] Return game labels, bet type labels, option labels, descriptions, selection hints.
- [ ] Ensure POS does not display enum names or raw option codes.

## PR F — Receipt/backup display

- [ ] Update TicketReceiptLineView to include option label.
- [ ] Use option labels in print receipt and shareableText.
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

