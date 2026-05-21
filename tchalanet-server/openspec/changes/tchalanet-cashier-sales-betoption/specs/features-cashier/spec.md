# Spec — features.cashier Game Options & POS Input

## Endpoint

Add/update:

```http
GET /tenant/cashier/games/available
```

Purpose: provide the POS with seller-facing game choices, labels, option labels, selection hints, and validation constraints.

## Response concepts

```java
CashierGameOptionResponse(
  GameCode gameCode,
  String gameLabel,
  BetType betType,
  String betTypeLabel,
  boolean requiresOption,
  List<CashierBetOptionResponse> options,
  String selectionHint
)

CashierBetOptionResponse(
  short code,
  String label,
  String description,
  String selectionHint
)
```

## UX rules

```text
- POS never displays raw BetType enum names.
- POS never displays only option code 1/2/3/4 without label.
- POS uses per-option selectionHint when present.
- POS sends raw seller input as selection; backend canonicalizes.
```

## Example Loto 4 response

```json
{
  "gameCode": "HT_LOTO4",
  "gameLabel": "Loto 4",
  "betType": "LOTTO4_PATTERN",
  "betTypeLabel": "Loto 4",
  "requiresOption": true,
  "selectionHint": "Choisissez une option puis entrez le numéro demandé.",
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

## Preview/Sell payload

Existing line payload stays stable:

```json
{
  "gameCode": "HT_LOTO4",
  "betType": "LOTTO4_PATTERN",
  "betOption": 3,
  "selection": "12",
  "stake": "10.00"
}
```

Backend canonicalizes this to `12**` for exposure/result purposes, but seller-facing responses should display `12` with label `2 premiers chiffres`.

