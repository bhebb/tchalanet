# Design — Bet Options, Selection Canonicalization, and Result Evaluation

## 1. Conceptual model

```text
gameCode   = HT_LOTO4
betType    = LOTTO4_PATTERN
betOption  = LOTTO4_BOX
selection  = 1245
```

The pair `betType + betOption` defines how to validate, canonicalize, price, expose, and settle a line.

## 2. BetOption enum

Create `BetOption` in:

```text
com.tchalanet.server.catalog.game.api.model.BetOption
```

Recommended enum:

```java
public enum BetOption {

    MARRIAGE_EXACT_ORDER(BetType.MARRIAGE_2D2D, (short) 1,
        "Ordre exact",
        "Les deux numéros doivent sortir dans l'ordre joué"),

    MARRIAGE_REVERSE_ALLOWED(BetType.MARRIAGE_2D2D, (short) 2,
        "Revers / Double",
        "Les deux numéros peuvent sortir dans les deux sens"),

    LOTTO3_STRAIGHT(BetType.LOTTO3_3D, (short) 1,
        "Exact",
        "Les 3 chiffres doivent sortir exactement dans l'ordre joué"),

    LOTTO3_BOX(BetType.LOTTO3_3D, (short) 2,
        "Désordre / Box",
        "Les 3 chiffres peuvent sortir dans n'importe quel ordre"),

    LOTTO4_STRAIGHT(BetType.LOTTO4_PATTERN, (short) 1,
        "Exact",
        "Les 4 chiffres doivent sortir exactement dans l'ordre joué"),

    LOTTO4_BOX(BetType.LOTTO4_PATTERN, (short) 2,
        "Désordre / Box",
        "Les 4 chiffres peuvent sortir dans n'importe quel ordre"),

    LOTTO4_FRONT_PAIR(BetType.LOTTO4_PATTERN, (short) 3,
        "2 premiers chiffres",
        "Les 2 premiers chiffres du tirage doivent correspondre"),

    LOTTO4_BACK_PAIR(BetType.LOTTO4_PATTERN, (short) 4,
        "2 derniers chiffres",
        "Les 2 derniers chiffres du tirage doivent correspondre"),

    LOTTO5_LOT1_LOT2(BetType.LOTTO5_PATTERN, (short) 1,
        "1er lot + 2e lot",
        "3 chiffres du 1er lot suivis des 2 chiffres du 2e lot"),

    LOTTO5_LOT1_LOT3(BetType.LOTTO5_PATTERN, (short) 2,
        "1er lot + 3e lot",
        "3 chiffres du 1er lot suivis des 2 chiffres du 3e lot"),

    LOTTO5_MIXED_1_2_3(BetType.LOTTO5_PATTERN, (short) 3,
        "Mixte 1er/2e/3e lot",
        "Dernier chiffre du 1er lot + 2 chiffres du 2e lot + 2 chiffres du 3e lot");
}
```

Rules:

```text
- A BetOption is scoped to exactly one BetType.
- Code values are stable API/DB values.
- Labels/descriptions are MVP defaults; later they can move to i18n/catalog.
- Option code 1 is not globally meaningful; it is meaningful only with its BetType.
```

## 3. BetType option requirements

```text
MATCH_1_2D    -> option not supported; betOption must be null
MATCH_2_2D    -> option not supported; betOption must be null
MATCH_3_2D    -> option not supported; betOption must be null
MARRIAGE_2D2D -> option required: 1 exact, 2 revers/double
LOTTO3_3D     -> option required: 1 exact, 2 box
LOTTO4_PATTERN -> option required: 1 exact, 2 box, 3 front pair, 4 back pair
LOTTO5_PATTERN -> option required: 1 lot1+lot2, 2 lot1+lot3, 3 mixed
```

## 4. Selection rules

### 4.1 Bolet / Numéro

```text
BetTypes: MATCH_1_2D, MATCH_2_2D, MATCH_3_2D
Input: 2 digits, accepts shorter input padded left
Examples:
  5  -> 05
  45 -> 45
betOption: null
canonicalSelection: 05 / 45
```

### 4.2 Maryaj

```text
BetType: MARRIAGE_2D2D
Input: two 2D numbers separated by -, /, or space
Examples:
  12-45 -> 12-45
  12/45 -> 12-45
  12 45 -> 12-45
betOption:
  1 = exact order
  2 = revers / double
canonicalSelection preserves order; do not sort.
```

Important: Do not canonicalize `45-12` to `12-45`. Order is meaningful for option 1.

### 4.3 Loto 3

```text
BetType: LOTTO3_3D
Input: 3 digits, accepts shorter input padded left
Examples:
  23  -> 023
  123 -> 123
betOption:
  1 = exact
  2 = box / désordre
canonicalSelection: 3 digits
```

3-way vs 6-way box is derived from the digits, not selected by the seller.

```text
112 -> box 3-way
123 -> box 6-way
111 -> special case; product/rules must decide whether box is invalid or same as straight
```

### 4.4 Loto 4

```text
BetType: LOTTO4_PATTERN
betOption 1 exact:
  input: 4 digits, ex 1245
  canonical: 1245

betOption 2 box:
  input: 4 digits, ex 1245
  canonical: 1245

betOption 3 front pair:
  input: 2 digits, ex 12
  canonical: 12**

betOption 4 back pair:
  input: 2 digits, ex 45
  canonical: **45
```

The seller does not type `*`; the backend generates `*` for canonical front/back pair keys.

### 4.5 Loto 5

```text
BetType: LOTTO5_PATTERN
Input: 5 digits
betOption 1 = 3 digits lot1 + 2 digits lot2
betOption 2 = 3 digits lot1 + 2 digits lot3
betOption 3 = last digit lot1 + 2 digits lot2 + 2 digits lot3
canonicalSelection: 5 digits
```

## 5. Result evaluation rules

### 5.1 Required result facts

The result projection must expose explicit ordered facts, not only a set:

```text
lot1_3d
lot2_2d
lot3_2d
pick3 / lot4 source if applicable
pick4 for Loto 4 straight/box/front/back pair
ordered two-digit lots for Maryaj ordering
```

The current calculator extracts a `Set<String>` of two-digit values, which is insufficient for ordered Maryaj and precise Loto 5 option mapping.

### 5.2 Maryaj

```text
selection = A-B
option exact:
  wins if A and B appear in winning ordered lots in that order

option revers/double:
  wins if A and B both appear in winning lots in either order
```

### 5.3 Loto 3

```text
option exact:
  selection == drawnPick3

option box:
  sortedDigits(selection) == sortedDigits(drawnPick3)
```

### 5.4 Loto 4

```text
option exact:
  selection == drawnPick4

option box:
  sortedDigits(selection) == sortedDigits(drawnPick4)

option front pair:
  canonicalSelection = 12**
  drawnPick4 startsWith 12

option back pair:
  canonicalSelection = **45
  drawnPick4 endsWith 45
```

### 5.5 Loto 5

If:

```text
lot1_3d = 123
lot2_2d = 45
lot3_2d = 67
```

Winning selections:

```text
option 1 = 12345
option 2 = 12367
option 3 = 34567
```

## 6. Exposure implications

ExposureKey must include:

```text
drawId
gameCode
betType
betOption
canonicalSelection
scope
scopeRef
```

Why: `LOTTO4_PATTERN/1245/option1` and `LOTTO4_PATTERN/1245/option2` do not necessarily share the same risk, odds, or limits.

## 7. Receipt display

Receipts must show human labels, not enum names.

Examples:

```text
LOTO3
Exact          123      10.00 HTG
Désordre       123      10.00 HTG

LOTO4
Exact          1245     10.00 HTG
Désordre       1245     10.00 HTG
2 premiers     12       10.00 HTG
2 derniers     45       10.00 HTG

LOTO5
1er+2e lot     12345    10.00 HTG
Mixte          34567    10.00 HTG
```

## 8. Cashier game-options endpoint

Add or update an endpoint used by POS:

```http
GET /tenant/cashier/games/available
```

Response shape:

```json
{
  "gameCode": "HT_LOTO4",
  "gameLabel": "Loto 4",
  "betType": "LOTTO4_PATTERN",
  "betTypeLabel": "Loto 4",
  "selectionHint": "Choisissez une option puis entrez le numéro demandé.",
  "requiresOption": true,
  "options": [
    {
      "code": 1,
      "label": "Exact",
      "description": "Les 4 chiffres doivent sortir exactement dans l'ordre joué",
      "selectionHint": "4 chiffres, ex: 1245"
    },
    {
      "code": 2,
      "label": "Désordre / Box",
      "description": "Les 4 chiffres peuvent sortir dans n'importe quel ordre",
      "selectionHint": "4 chiffres, ex: 1245"
    },
    {
      "code": 3,
      "label": "2 premiers chiffres",
      "description": "Les 2 premiers chiffres du tirage doivent correspondre",
      "selectionHint": "2 chiffres, ex: 12"
    },
    {
      "code": 4,
      "label": "2 derniers chiffres",
      "description": "Les 2 derniers chiffres du tirage doivent correspondre",
      "selectionHint": "2 chiffres, ex: 45"
    }
  ]
}
```

