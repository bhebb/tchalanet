# features-cashier Specification

## Purpose
TBD - created by archiving change tchalanet-cashier-sales-betoption. Update Purpose after archive.
## Requirements
### Requirement: Cashier exposes seller-facing game option metadata

The cashier feature SHALL expose:

```http
GET /tenant/cashier/games/available
```

The endpoint SHALL return cashier game choices with game labels, bet type
labels, option labels, option descriptions, and selection hints.

#### Scenario: Loto 4 options include labels and hints

When the cashier requests `/tenant/cashier/games/available`
Then the response includes `HT_LOTO4`
And it includes options for exact, box, front pair, and back pair
And each option includes a seller-facing label and selection hint.

### Requirement: POS payload remains stable

Cashier preview and sell payloads SHALL continue to send raw seller input as
`selection` and numeric option code as `betOption`.

#### Scenario: Loto 4 front pair payload is canonicalized by backend

Given the POS sends `HT_LOTO4`, `LOTTO4_PATTERN`, bet option `3`, and selection
`12`
When backend evaluates the line
Then backend canonicalizes it as `12**`
And seller-facing responses display `12` with the front-pair label.

### Requirement: POS does not depend on technical enum names

The POS SHALL use labels and hints from the game-options endpoint for display.

#### Scenario: Cashier sees a human option label

Given the available-games response contains Loto 3 option code `2`
When the POS renders the option
Then it displays `Désordre / Box`
And does not display only `2` or `LOTTO3_BOX`.
