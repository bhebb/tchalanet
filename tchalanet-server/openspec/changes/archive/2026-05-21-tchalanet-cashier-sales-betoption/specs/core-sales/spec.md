# core-sales Spec Delta

## ADDED Requirements

### Requirement: Sales canonicalization uses bet type and bet option

Core sales SHALL canonicalize sale selections with:

```java
SelectionKey canonicalize(BetType betType, Short betOption, String rawSelectionKey)
```

The old two-argument overload MAY remain only as a compatibility bridge.
New cashier sale paths SHALL call the option-aware method.

#### Scenario: Maryaj preserves seller order

Given bet type `MARRIAGE_2D2D`
And bet option `1`
When the seller enters `45/12`
Then the canonical selection is `45-12`.

#### Scenario: Loto 4 front pair generates a backend mask

Given bet type `LOTTO4_PATTERN`
And bet option `3`
When the seller enters `12`
Then the canonical selection is `12**`.

#### Scenario: Cashier wildcard input is rejected

Given bet type `LOTTO4_PATTERN`
And bet option `1`
When the seller enters `12**`
Then the sale validation rejects the selection.

### Requirement: Sale evaluation validates game, option, and selection

Sale evaluation SHALL validate:

- `GameCode.supports(betType)`
- `BetOption.from(betType, betOption)`
- option-aware selection canonicalization

#### Scenario: Unsupported option is rejected before persistence

Given a sale line with `LOTTO4_PATTERN`
And bet option `5`
When the cashier previews or sells the ticket
Then the sale is rejected with an invalid bet option issue.

### Requirement: Exposure identity includes bet option and canonical selection

Exposure keys SHALL include game code, bet type, bet option, and canonical
selection so formulas with identical digits do not collapse into one exposure
bucket.

#### Scenario: Loto 4 exact and box have different exposure keys

Given two sale lines for `LOTTO4_PATTERN`
And both use selection `1245`
And one uses option `1`
And the other uses option `2`
When exposure is grouped
Then the lines are grouped under different exposure keys.

### Requirement: Ticket settlement is option-aware

`TicketWinningCalculator` SHALL resolve `BetOption.from(line.betType(), line.betOption())`
for option-bearing bet types and evaluate against ordered result facts.

#### Scenario: Loto 3 box matches sorted digits

Given drawn pick 3 `123`
And a Loto 3 line with option box and selection `321`
When settlement runs
Then the line wins.

#### Scenario: Maryaj exact requires ordered appearance

Given ordered two-digit result facts where `12` appears before `45`
And a Maryaj exact line with selection `45-12`
When settlement runs
Then the line loses.

#### Scenario: Loto 5 mixed uses lot1 last digit plus lot2 and lot3

Given `lot1_3d = 123`
And `lot2_2d = 45`
And `lot3_2d = 67`
When a Loto 5 option 3 line uses selection `34567`
Then the line wins.

### Requirement: Receipt and backup text display human option labels

Receipts and shareable backup text SHALL use `BetOption.label()` when a line has
an option.

#### Scenario: Loto 4 front pair receipt label is human-readable

Given a sold Loto 4 front-pair line
When the receipt is formatted
Then the line label contains `2 premiers chiffres`
And it does not display only raw option code `3`.
