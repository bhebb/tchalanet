# Testing strategy — core.promotion

## 1. Unit tests purs

Tester `SimplePromotionRuleEngine` sans Spring.

Scénarios minimum :

```text
- no active rules -> empty decision
- rule active but outside date window -> empty decision
- cart total below threshold -> no free line
- cart total above threshold -> free line grant
- Christmas before noon -> payout multiplier 60
- Christmas after noon -> no boost
- tenant A multiplier 60, tenant B multiplier 50 -> decisions distinctes
- conflicting multiplier overrides -> highest priority wins
- equal priority conflict -> configuration error
- offline context + online-only rule -> not applied
```

## 2. Validation JSON

Tester que `condition_json` et `effect_json` sont validés avant publication.

```text
- unknown condition type rejected
- unknown effect type rejected
- missing required fields rejected
- invalid money/currency rejected
- invalid timezone rejected
- unknown game code rejected
- invalid multiplier rejected
- payout multiplier override without prizeRank rejected when required
```

## 3. Integration tests

Avec DB + RLS :

```text
- tenant A cannot read tenant B rules
- active rules are loaded for current tenant only
- archived versions are not used for new tickets
- existing ticket snapshots remain stable after rule update
- promotion_rule unique tenant/code/version respected
```

## 4. Sales integration tests

```text
- preview returns promotion notice
- sale confirmation revalidates rules
- client-forced free line is rejected if not eligible
- ticket line stores promotion snapshot
- payout modifier snapshot stores tenant-specific multiplier
```

## 5. Settlement tests

```text
- payout calculation uses ticket snapshot
- changed promotion config after sale does not affect old ticket
- free Maryaj line can win if configured as payable
- Christmas multiplier 60 used for ticket sold before noon
- Christmas multiplier not used for ticket sold after noon
```
