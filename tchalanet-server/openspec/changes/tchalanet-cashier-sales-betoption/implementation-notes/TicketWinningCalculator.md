# Implementation Note — TicketWinningCalculator cleanup

Current result calculation is partially option-aware:

- LOTTO4_PATTERN and LOTTO5_PATTERN receive `line.betOption()`.
- LOTTO3_3D is exact-only.
- MARRIAGE_2D2D ignores order.
- LOTTO4 options 1/2/3 currently behave the same.
- LOTTO5 option 3 currently returns false.

Target:

```text
BetOption.from(line.betType(), line.betOption())
```

Use explicit result facts instead of only `Set<String> twoDigits`:

```text
orderedTwoDigits
lot1_3d
lot2_2d
lot3_2d
pick3
pick4
```

If `DrawResultProjection` cannot expose those facts directly, add an adapter in core.sales result service to derive a `TicketResultFacts` object from the projection.

