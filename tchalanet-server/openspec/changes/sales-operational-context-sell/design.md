# Design — Sales Sell Operational Context

## Flow

```text
Controller / offline promotion
  -> resolve TchRequestContext
  -> resolve SellerOperationalContext
  -> execute core.sales sell command/API
  -> publish events after commit
```

## SellerOperationalContext inputs

- tenant id from request context;
- actor user id;
- terminal id;
- optional outlet id;
- sales session id;
- operation type `SELL` or `OFFLINE_PROMOTION`.

## Validation owners

| Validation | Owner |
|---|---|
| user exists/profile/membership | `platform.identity` |
| permission to sell | `platform.accesscontrol` |
| terminal active/belongs to tenant/outlet | `core.terminal` |
| outlet active/belongs to tenant | `core.outlet` |
| session open/belongs to terminal/outlet/user | `core.session` |
| draw cutoff/pricing/limits/ticket creation | `core.sales` and related core APIs |

## Package options

If resolver is used only by sales, keep under:

```text
core.sales.internal.application.service
```

If resolver is used by sales, payout, offline sync, and other POS flows, promote to:

```text
platform.operationalcontext.api
platform.operationalcontext.internal.service
```

Initial recommendation: implement reusable API only if at least two modules consume it immediately.
