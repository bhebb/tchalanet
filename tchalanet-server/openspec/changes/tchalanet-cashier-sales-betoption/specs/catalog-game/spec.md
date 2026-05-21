# Spec — catalog.game BetOption MVP

## New public enum

Create:

```text
com.tchalanet.server.catalog.game.api.model.BetOption
```

Fields:

```java
BetType betType
short code
String label
String description
```

Required static helpers:

```java
List<BetOption> allowedFor(BetType betType)
boolean requiresOption(BetType betType)
BetOption from(BetType betType, Short code)
```

Behavior:

```text
- from(null, x) throws.
- If betType has no options, code must be null and from returns null.
- If betType has options, code is required.
- Unsupported option code throws IllegalArgumentException.
```

## MVP options

```text
MARRIAGE_2D2D:
  1 MARRIAGE_EXACT_ORDER
  2 MARRIAGE_REVERSE_ALLOWED

LOTTO3_3D:
  1 LOTTO3_STRAIGHT
  2 LOTTO3_BOX

LOTTO4_PATTERN:
  1 LOTTO4_STRAIGHT
  2 LOTTO4_BOX
  3 LOTTO4_FRONT_PAIR
  4 LOTTO4_BACK_PAIR

LOTTO5_PATTERN:
  1 LOTTO5_LOT1_LOT2
  2 LOTTO5_LOT1_LOT3
  3 LOTTO5_MIXED_1_2_3
```

## BetType integration

`BetType` should delegate option knowledge to `BetOption`:

```java
public boolean requiresOption() {
    return BetOption.requiresOption(this);
}

public List<BetOption> allowedOptions() {
    return BetOption.allowedFor(this);
}

public boolean supportsOption(Short option) {
    try {
        BetOption.from(this, option);
        return true;
    } catch (IllegalArgumentException ex) {
        return false;
    }
}
```

## POS display

The UI must use labels/hints from the game-options endpoint, not enum names.

