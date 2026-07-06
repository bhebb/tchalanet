# Design: supported-bet-options-combinations-v1

## Supported Matrix

### Bòlèt / 2D Lots

| Game | Seller choice | Bet type | Bet option | Settlement variant | Winning rule | Seller/client label | Admin/support label |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Bòlèt | 1er lot | `MATCH_1_2D` | `null` | `MATCH_1_2D` | Selection matches the last 2 digits of lot 1 | 1er lot | 1er lot |
| Bòlèt | 2e lot | `MATCH_2_2D` | `null` | `MATCH_2_2D` | Selection matches lot 2 | 2e lot | 2e lot |
| Bòlèt | 3e lot | `MATCH_3_2D` | `null` | `MATCH_3_2D` | Selection matches lot 3 | 3e lot | 3e lot |

Bòlèt has no explicit `BetOption`; the `BetType` carries the lot choice.

### Maryaj

| Game | Seller choice | Bet type | Bet option | Settlement variant | Winning rule | Seller/client label | Admin/support label |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Maryaj | Ordre exact | `MARRIAGE_2D2D` | `1` | `MARRIAGE_EXACT_ORDER` | Two 2D numbers appear in played order | Ordre exact | Maryaj · ordre exact |
| Maryaj | Revers / double | `MARRIAGE_2D2D` | `2` | `MARRIAGE_REVERSE_ALLOWED` | Both numbers appear, order can be reversed | Revers / double | Maryaj · revers/double |

### Loto 3

| Game | Seller choice | Bet type | Bet option | Settlement variant | Pattern | Example | Seller/client label | Admin/support label |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Loto 3 | Exact | `LOTTO3_3D` | `1` | `LOTTO3_STRAIGHT` | Exact order | `123` wins only on `123` | Exact | Exact |
| Loto 3 | Permuté | `LOTTO3_3D` | `2` | `LOTTO3_BOX_3_WAY` | 2 identical digits | `112` wins on `112`, `121`, `211` | Permuté | Permuté · 3-way |
| Loto 3 | Permuté | `LOTTO3_3D` | `2` | `LOTTO3_BOX_6_WAY` | 3 different digits | `123` wins on any permutation | Permuté | Permuté · 6-way |

The seller only chooses Exact or Permuté. Backend computes 3-way or 6-way from the played number.

### Loto 4

| Game | Seller choice | Bet type | Bet option | Settlement variant | Pattern | Example | Seller/client label | Admin/support label |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Loto 4 | Exact | `LOTTO4_PATTERN` | `1` | `LOTTO4_STRAIGHT` | Exact order | `1234` wins only on `1234` | Exact | Exact |
| Loto 4 | Permuté | `LOTTO4_PATTERN` | `2` | `LOTTO4_BOX_4_WAY` | 3 identical digits | `1112` | Permuté | Permuté · 4-way |
| Loto 4 | Permuté | `LOTTO4_PATTERN` | `2` | `LOTTO4_BOX_6_WAY` | 2 pairs | `1122` | Permuté | Permuté · 6-way |
| Loto 4 | Permuté | `LOTTO4_PATTERN` | `2` | `LOTTO4_BOX_12_WAY` | 1 pair + 2 different digits | `1123` | Permuté | Permuté · 12-way |
| Loto 4 | Permuté | `LOTTO4_PATTERN` | `2` | `LOTTO4_BOX_24_WAY` | 4 different digits | `1234` | Permuté | Permuté · 24-way |
| Loto 4 | Deux premiers | `LOTTO4_PATTERN` | `3` | `LOTTO4_FRONT_PAIR` | First 2 digits match | `12` / `12**` | Deux premiers | Deux premiers |
| Loto 4 | Deux derniers | `LOTTO4_PATTERN` | `4` | `LOTTO4_BACK_PAIR` | Last 2 digits match | `34` / `**34` | Deux derniers | Deux derniers |

The seller does not choose 4-way, 6-way, 12-way, or 24-way.

### Loto 5

| Game | Seller choice | Bet type | Bet option | Settlement variant | Winning rule | Example | Seller/client label | Admin/support label |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Loto 5 | 1er lot + 2e lot | `LOTTO5_PATTERN` | `1` | `LOTTO5_LOT1_LOT2` | `lot1_3d + lot2_2d` | `36175` | 1er + 2e lot | 1er + 2e lot |
| Loto 5 | 1er lot + 3e lot | `LOTTO5_PATTERN` | `2` | `LOTTO5_LOT1_LOT3` | `lot1_3d + lot3_2d` | `36176` | 1er + 3e lot | 1er + 3e lot |
| Loto 5 | Mixte 1er/2e/3e lot | `LOTTO5_PATTERN` | `3` | `LOTTO5_MIXED_1_2_3` | last digit lot1 + lot2 + lot3 | `17576` | Mixte | Mixte 1er/2e/3e lot |

## Domain Model

Add `SettlementVariant` in core sales domain/application internals with:

- `MATCH_1_2D`
- `MATCH_2_2D`
- `MATCH_3_2D`
- `MARRIAGE_EXACT_ORDER`
- `MARRIAGE_REVERSE_ALLOWED`
- `LOTTO3_STRAIGHT`
- `LOTTO3_BOX_3_WAY`
- `LOTTO3_BOX_6_WAY`
- `LOTTO4_STRAIGHT`
- `LOTTO4_BOX_4_WAY`
- `LOTTO4_BOX_6_WAY`
- `LOTTO4_BOX_12_WAY`
- `LOTTO4_BOX_24_WAY`
- `LOTTO4_FRONT_PAIR`
- `LOTTO4_BACK_PAIR`
- `LOTTO5_LOT1_LOT2`
- `LOTTO5_LOT1_LOT3`
- `LOTTO5_MIXED_1_2_3`

Add a pure `SettlementVariantResolver` responsible for:

- mapping `BetType + BetOption + selection` to `SettlementVariant`;
- detecting Loto 3 box 3-way/6-way;
- detecting Loto 4 box 4-way/6-way/12-way/24-way;
- rejecting invalid patterns.

The resolver stays deterministic and side-effect free: no Spring injection, no repositories, no bus.

## Settlement Flow

Sale preview:

1. Validate `gameCode`, `betType`, `betOption`, and selection.
2. Resolve `SettlementVariant`.
3. Compute potential payout from the supported option/variant rules.
4. Return the sale label and optional admin/debug computed variant.

Sale confirmation:

1. Persist `betType`, `betOption`, selection, odds/potential payout snapshot.
2. Decide whether to persist `computed_settlement_variant`.
3. If persisted, use it as sale-time snapshot during settlement; otherwise resolve again from
   `betType + betOption + selection`.

Result application:

1. `TicketWinningCalculator` builds `TicketResultFacts` from `DrawResultProjection`.
2. It resolves or reads `SettlementVariant`.
3. It evaluates each ticket line and returns existing `TicketLineResult`.

## Ticket Facts Correction

Current Bòlèt matching must be corrected:

- `MATCH_1_2D` checks lot 1 only.
- `MATCH_2_2D` checks lot 2 only.
- `MATCH_3_2D` checks lot 3 only.
- `lot1_2d` is the last 2 digits of lot 1 when lot 1 is a 3D result.

This prevents a Bòlèt line for one lot from winning because the same 2D value appeared in another
lot.

## Display Rules

Seller terminal:

- show sale-friendly choices only;
- never show `SettlementVariant` enum values;
- never show 3-way/6-way/12-way/24-way as seller choices in v1.

Client ticket/receipt:

- show commercial labels (`Exact`, `Permuté`, `Deux premiers`, etc.);
- do not show technical variants by default.

Admin/support:

- may show `Permuté · 24-way` or `Variante calculée: 24-way`;
- useful in ticket detail, settlement audit, result detail, support investigation, reports, and
  rules/combination tabs.

Result page:

- may add a `Combinaisons & règles` tab backed by the current result facts or
  `SettlementExplanationApi`, not a static frontend-only matrix;
- do not pollute the main result table.
