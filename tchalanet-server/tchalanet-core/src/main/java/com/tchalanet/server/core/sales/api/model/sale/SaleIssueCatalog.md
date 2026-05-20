# Sale Issue Catalog

Issue codes returned by cashier sale preview and sell flows.

| Code | Decision | Scope | Detail fields |
| --- | --- | --- | --- |
| `SELECTION_EXPOSURE_LIMIT_EXCEEDED` | `REQUIRES_CHANGES` | Line or basket | `allowedRemaining`, `requestedStake`, `exposureKey` |
| `EXPOSURE_CHANGED` | `REQUIRES_CHANGES` | Basket | `allowedRemaining`, `requestedStake`, `exposureKey` |
| `APPROVAL_REQUIRED` | `REQUIRES_CHANGES` | Line or basket | `policyRef`, `threshold`, `requestedStake` |
| `DRAW_CUTOFF_EXCEEDED` | `REJECTED_FINAL` | Basket | `drawId`, `cutoffAt`, `now` |
| `DRAW_CLOSED` | `REJECTED_FINAL` | Basket | `drawId`, `status` |
| `SESSION_CLOSED` | `REJECTED_FINAL` | Basket | `sessionId`, `status` |
| `TERMINAL_BLOCKED` | `REJECTED_FINAL` | Basket | `terminalId`, `status` |
| `OUTLET_SUSPENDED` | `REJECTED_FINAL` | Basket | `outletId`, `status` |
| `TENANT_DISABLED` | `REJECTED_FINAL` | Basket | `tenantId`, `status` |
| `UNTRUSTED_OPERATIONAL_CONTEXT` | `REJECTED_FINAL` | Basket | `reason` |
| `INVALID_SELECTION_FORMAT` | `REQUIRES_CHANGES` | Line | `selection`, `gameCode`, `betType` |
| `STAKE_TOO_HIGH` | `REQUIRES_CHANGES` | Line | `maxStake`, `requestedStake`, `currency` |
| `STAKE_TOO_LOW` | `REQUIRES_CHANGES` | Line | `minStake`, `requestedStake`, `currency` |
| `BASKET_LINE_COUNT_EXCEEDED` | `REQUIRES_CHANGES` | Basket | `maxLines`, `requestedLines` |
| `BASKET_TOTAL_EXCEEDED` | `REQUIRES_CHANGES` | Basket | `maxTotal`, `requestedTotal`, `currency` |

`lineIndex = -1` means the issue applies to the whole basket.
