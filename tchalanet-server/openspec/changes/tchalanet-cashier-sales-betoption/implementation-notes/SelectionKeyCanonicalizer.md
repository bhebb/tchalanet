# Implementation Note — SelectionKeyCanonicalizer cleanup

Current uploaded code has both the new three-arg method and the old two-arg method in the same class. Remove or deprecate the old two-arg method after all callers migrate.

The two-arg method currently sorts Maryaj pairs and accepts `*` masks for LOTTO4/LOTTO5. That conflicts with V1 decisions.

Target shape:

```java
public static SelectionKey canonicalize(
    BetType betType,
    Short rawBetOption,
    String rawSelectionKey
) { ... }
```

Rules:

```text
- Maryaj preserves order.
- Loto 4 front/back pair generates `*` internally.
- Seller free-form `*` input is not part of cashier V1.
- BetOption.from(...) is the source of truth for option validation.
```

