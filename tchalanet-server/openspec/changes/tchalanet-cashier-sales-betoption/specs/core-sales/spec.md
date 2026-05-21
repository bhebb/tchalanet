# Spec — core.sales Option-aware Sale, Selection, and Results

## 1. Selection canonicalization

Change canonicalizer signature:

```java
SelectionKey canonicalize(BetType betType, Short betOption, String rawSelectionKey)
```

Keep the old two-arg overload only as a temporary bridge if needed. New sale paths must call the option-aware method.

## 2. Canonicalization table

```text
MATCH_1_2D, MATCH_2_2D, MATCH_3_2D:
  selection: 2 digits, padded left
  betOption: null

MARRIAGE_2D2D:
  selection: 2D-2D
  accepted separators: -, /, space
  canonical: preserve order, no sort
  betOption: required, 1 exact, 2 revers

LOTTO3_3D:
  selection: 3 digits, padded left
  betOption: required, 1 exact, 2 box

LOTTO4_PATTERN:
  option 1 exact: selection 4 digits -> canonical 4 digits
  option 2 box: selection 4 digits -> canonical 4 digits
  option 3 front pair: selection 2 digits -> canonical NN**
  option 4 back pair: selection 2 digits -> canonical **NN

LOTTO5_PATTERN:
  selection: 5 digits
  betOption: required, 1/2/3 per Loto 5 option map
```

## 3. Disable free wildcard input in V1

`*` is not accepted from seller input except as generated canonical output for Loto 4 front/back pair. The previous `canonicalizePatternStrict` behavior must not be used by the cashier sale path in V1.

## 4. Result calculator changes

`TicketWinningCalculator` must use `BetOption.from(line.betType(), line.betOption())` instead of switching on raw short values.

### 4.1 Lotto 3

Replace exact-only behavior with option-aware behavior:

```text
LOTTO3_STRAIGHT: selection equals drawn pick3
LOTTO3_BOX: sorted digits match drawn pick3 sorted digits
```

### 4.2 Maryaj

The current set-based logic is not enough for exact order. The result projection must expose ordered two-digit lots.

```text
MARRIAGE_EXACT_ORDER: A appears before B in ordered lots
MARRIAGE_REVERSE_ALLOWED: A and B are both present in any order
```

### 4.3 Lotto 4

Requires drawn Pick 4 value.

```text
LOTTO4_STRAIGHT: selection == pick4
LOTTO4_BOX: sortedDigits(selection) == sortedDigits(pick4)
LOTTO4_FRONT_PAIR: canonical NN** matches pick4[0..2)
LOTTO4_BACK_PAIR: canonical **NN matches pick4[2..4)
```

### 4.4 Lotto 5

Requires explicit lot1_3d, lot2_2d, lot3_2d.

```text
LOTTO5_LOT1_LOT2: selection == lot1_3d + lot2_2d
LOTTO5_LOT1_LOT3: selection == lot1_3d + lot3_2d
LOTTO5_MIXED_1_2_3: selection == lastDigit(lot1_3d) + lot2_2d + lot3_2d
```

## 5. Sale evaluation changes

SaleAcceptanceEvaluator must validate:

```text
- GameCode supports BetType
- BetType requires/no-supports betOption according to BetOption enum
- Selection canonicalizes successfully with betType + betOption
- ExposureKey includes betOption and canonicalSelection
```

## 6. Receipt/message formatting

Receipt line labels use BetOption labels where applicable.

```text
LOTO3 Exact 123
LOTO3 Désordre 123
LOTO4 2 premiers 12
LOTO5 1er+2e lot 12345
```

## 7. Migration caution

Existing tickets using old canonical values must be considered. If legacy tickets exist in dev only, reset dev DB. If prod data exists, keep old settlement logic for old records or add a migration/version flag.

